package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

import java.io.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.zip.*;

public class Omni<ROOT> {
    private final BlockingQueue<SerializedOperation<ROOT, ?>> queue = new ArrayBlockingQueue<>(10);
    private final StampedLock lock = new StampedLock();
    private final Kryo kryo;
    private final File dir;
    private final File snapshot;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(this::createThread);
    private FileOutputStream outputStream;
    private Output output;

    private ROOT root;
    private volatile long lastCommandId = 0;

    Omni(Kryo kryo, File path, Supplier<ROOT> instanceCreator) {
        this.kryo = kryo;
        dir = path;
        snapshot = new File(dir, "snapshot");
        root = snapshot.exists() ? loadSnapshot() : instanceCreator.get();
        loadOperations();
        lastCommandId++;
        startOutput();
        executor.scheduleAtFixedRate(this::sync, 0, 1, TimeUnit.SECONDS);
    }

    private Thread createThread(Runnable r) {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
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

    public <RESULT> RESULT query(Function<ROOT, RESULT> query) {
        try (Lock $ = lock(false)) {
            return query.apply(root);
        }
    }

    public void execute(VoidOperation<ROOT> operation) {
        execute(operation, false);
    }

    public <RESULT> RESULT executeAndQuery(Operation<ROOT, RESULT> operation) {
        return executeAndQuery(operation, false);
    }

    public void execute(VoidOperation<ROOT> operation, boolean waitForWrite) {
        executeAndQuery(operation, waitForWrite);
    }

    public <RESULT> RESULT executeAndQuery(Operation<ROOT, RESULT> operation, boolean waitForWrite) {
        try {
            RESULT result;
            SerializedOperation<ROOT, RESULT> op;
            try (Lock $ = lock(true)) {
                op = new SerializedOperation<>(operation);
                result = op.perform(root);
                queue.put(op);
            }

            writeToFile();

            if (waitForWrite) {
                op.getFuture().get();
            }

            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
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
        SerializedOperation<ROOT, ?> operation = queue.poll();
        kryo.writeObject(output, operation.getClock());
        kryo.writeClassAndObject(output, operation.getOperation());
        output.flush();
        operation.getFuture().complete(true);
    }

    private ROOT loadSnapshot() {
        try (Input input = new Input(new GZIPInputStream(new FileInputStream(snapshot)))) {
            lastCommandId = input.readVarLong(true);
            return (ROOT) kryo.readClassAndObject(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void loadOperations() {
        Stream.of(dir.listFiles()).filter(this::isOperation).filter(this::isAfterSnapshot).sorted(Comparator
                .comparing(this::getId)).forEach(this::loadOperations);
    }

    private void loadOperations(File f) {
        try (Input input = new Input(new BufferedInputStream(new FileInputStream(f)))) {
            lastCommandId = getId(f);
            while (!input.eof()) {
                Instant clock = kryo.readObject(input, Instant.class);
                ((Operation) kryo.readClassAndObject(input)).perform(root, clock);
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
        File tmp = new File(dir, "tmp");
        try {
            try (Lock $ = lock(false); Output out = new Output(new GZIPOutputStream(new FileOutputStream(tmp)))) {
                out.writeVarLong(lastCommandId, true);
                kryo.writeClassAndObject(out, root);
                lastCommandId++;
            }

            if ((!snapshot.exists() || snapshot.delete()) && tmp.renameTo(snapshot)) {
                outputStream.close();
                startOutput();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void close() {
        try {
            sync();
            executor.shutdownNow();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Lock lock(boolean write) {
        long stamp = write ? lock.writeLock() : lock.readLock();
        return () -> lock.unlock(stamp);
    }

    interface Lock extends AutoCloseable {
        @Override
        void close();
    }
}
