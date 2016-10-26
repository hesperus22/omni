package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.zip.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Stream;
import java.time.Instant;


public class Omni<T> {
    private final BlockingQueue<SerializedOperation<T, ?>> queue = new ArrayBlockingQueue<>(10);
    private final StampedLock lock = new StampedLock();
    private final Kryo kryo;
    private final File dir;
    private final File snapshot;
    private FileOutputStream outputStream;
    private Output output;

    private T root;
    private volatile long lastCommandId = 0;

    Omni(Kryo kryo, File path, Supplier<T> instanceCreator) {
        this.kryo = kryo;
        dir = path;
        snapshot = new File(dir, "snapshot.zip");
        root = snapshot.exists() ? loadSnapshot() : instanceCreator.get();

        loadOperations();
        lastCommandId++;
        startOutput();
        Executors.newSingleThreadScheduledExecutor(this::createThread).scheduleAtFixedRate(this::sync, 0, 1, TimeUnit.SECONDS);
    }

    private Thread createThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }

    public void sync() {
        try {
            outputStream.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <E> E query(Function<T, E> query) {
        long stamp = lock.readLock();
        try {
            return query.apply(root);
        } finally {
            lock.unlock(stamp);
        }
    }

    public void execute(VoidOperation<T> operation) {
        execute(operation, false);
    }

    public <E> E executeAndQuery(Operation<T, E> operation) {
        return executeAndQuery(operation, false);
    }

    public void execute(VoidOperation<T> operation, boolean waitForWrite) {
        executeAndQuery(operation, waitForWrite);
    }

    public <E> E executeAndQuery(Operation<T, E> operation, boolean waitForWrite) {
        long stamp = lock.writeLock();
        E result;
        SerializedOperation<T, E> op;
        try {
            op = new SerializedOperation<>(operation);
            result = op.perform(root);
            queue.put(op);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            lock.unlock(stamp);
        }

        try {
            writeToFile();

            if (waitForWrite) {
                op.getFuture().get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }

        return result;
    }

    private void startOutput() {
        try {
            outputStream = new FileOutputStream(new File(dir, String.valueOf(lastCommandId)));
            output = new Output(outputStream);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized void writeToFile() {
        SerializedOperation<T, ?> operation = queue.poll();
        kryo.writeObject(output, operation.getClock());
        kryo.writeClassAndObject(output, operation.getOperation());
        output.flush();
        operation.getFuture().complete(true);
    }

    private T loadSnapshot() {
        try (Input input = new Input(new GZIPInputStream(new FileInputStream(snapshot)))) {
            lastCommandId = input.readVarLong(true);
            return (T) kryo.readClassAndObject(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void loadOperations() {
        Stream.of(Optional.ofNullable(dir.listFiles()).orElse(new File[0]))
                .filter(this::isOperation).filter(this::isAfterSnapshot).sorted(Comparator.comparing(this::getId))
                .forEach(this::loadOperations);
    }

    private void loadOperations(File f) {
        try (Input input = new Input(new BufferedInputStream(new FileInputStream(f)))) {
            lastCommandId = getId(f);
            while (!input.eof()) {
                Instant clock = kryo.readObject(input, Instant.class);
                Operation operation = (Operation) kryo.readClassAndObject(input);

                operation.perform(root, clock);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private long getId(File file) {
        return Long.parseLong(file.getName());
    }

    private boolean isAfterSnapshot(File file) {
        return getId(file) >= lastCommandId;
    }

    private boolean isOperation(File file) {
        return file.getName().matches("[0-9]+");
    }

    public void takeSnapshot() {
        File tmpSnapshot = new File(dir, "tmpSnapshot");

        long stamp = lock.writeLock();
        try {
            try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(tmpSnapshot)))) {
                output.writeVarLong(lastCommandId, true);
                kryo.writeClassAndObject(output, root);
                lastCommandId++;
            }

            if ((!snapshot.exists() || snapshot.delete()) && tmpSnapshot.renameTo(snapshot)) {
                outputStream.close();
                startOutput();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            lock.unlock(stamp);
        }
    }
}
