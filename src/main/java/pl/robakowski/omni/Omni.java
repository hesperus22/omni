package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import static java.util.Comparator.comparing;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Omni<T>
{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final BlockingQueue<SerializedOperation<T, ?>> queue = new ArrayBlockingQueue<>( 10 );
    private final Kryo kryo;
    private final File dir;
    private final File snapshot;
    private FileOutputStream outputStream;
    private Output output;
    private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor( r ->
    {
        Thread t = new Thread( r );
        t.setDaemon( true );
        t.setName( "omni-timer" );
        return t;
    } );

    private T root;
    private volatile long lastCommandId = 0;

    Omni( Kryo kryo, String path, Supplier<T> instanceCreator )
    {
        this.kryo = kryo;
        dir = new File( path );
        snapshot = new File( dir, "snapshot.zip" );
        root = snapshot.exists() ? loadSnapshot() : instanceCreator.get();
        timer.scheduleAtFixedRate( this::sync, 0, 1, TimeUnit.SECONDS );

        loadOperations();
        startOutput();
    }

    private void sync()
    {
        try
        {
            outputStream.getFD().sync();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
    }

    public <E> CompletableFuture<E> query( Query<T, E> query )
    {
        lock.readLock().lock();
        try
        {
            return CompletableFuture.completedFuture( query.perform( root ) );
        }
        catch( Exception e )
        {
            CompletableFuture<E> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally( e );
            return completableFuture;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public CompletableFuture<Void> execute( VoidOperation<T> operation )
    {
        return executeAndQuery( operation );
    }

    public <E> CompletableFuture<E> executeAndQuery( Operation<T, E> operation )
    {
        CompletableFuture<E> ret = new CompletableFuture<>();
        writeToFile( operation, ret );
        writeLocked( this::lockedExecuteAndQuery );
        return ret;
    }

    private void startOutput()
    {
        try
        {
            outputStream = new FileOutputStream( new File( dir, String.valueOf( lastCommandId ) ) );
            output = new Output( outputStream );
        }
        catch( FileNotFoundException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private synchronized <E> void writeToFile( Operation<T, E> operation, CompletableFuture<E> ret )
    {
        Instant now = Instant.now();

        kryo.writeObject( output, now );
        kryo.writeClassAndObject( output, operation );
        output.flush();

        queue.add( new SerializedOperation<>( operation, now, ret ) );
    }

    private T loadSnapshot()
    {
        try (ZipInputStream in = new ZipInputStream( new FileInputStream( snapshot ) );
            Input input = new Input( new BufferedInputStream( in ) ))
        {
            in.getNextEntry();
            lastCommandId = input.readVarLong( true );
            return (T)kryo.readClassAndObject( input );
        }
        catch( Exception e )
        {
            throw new IllegalStateException( e );
        }
    }

    private void loadOperations()
    {
        Stream.of( Optional.ofNullable( dir.listFiles() ).orElse( new File[ 0 ] ) )
            .filter( this::isOperation ).filter( this::isAfterSnapshot ).sorted( comparing( this::getId ) )
            .forEach( this::loadOperations );
    }

    private void loadOperations( File f )
    {
        try (Input input = new Input( new BufferedInputStream( new FileInputStream( f ) ) ))
        {
            lastCommandId = getId( f );
            while( !input.eof() )
            {
                Instant clock = kryo.readObject( input, Instant.class );
                Operation<T, ?> operation = (Operation<T, ?>)kryo.readClassAndObject( input );

                try
                {
                    operation.perform( root, clock );
                }
                catch( Exception e )
                {
                }
            }
        }
        catch( FileNotFoundException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private long getId( File file )
    {
        return Long.parseLong( file.getName() );
    }

    private boolean isAfterSnapshot( File file )
    {
        return getId( file ) >= lastCommandId;
    }

    private boolean isOperation( File file )
    {
        return file.getName().matches( "[0-9]+" );
    }

    private <E> void lockedExecuteAndQuery()
    {
        SerializedOperation<T, E> poll = (SerializedOperation<T, E>)queue.poll();
        try
        {
            poll.getFuture().complete( poll.getOperation().perform( root, poll.getClock() ) );
        }
        catch( Exception e )
        {
            poll.getFuture().completeExceptionally( e );
        }
    }

    public void takeSnapshot()
    {
        writeLocked( this::lockedTakeSnapshot );
    }

    private void lockedTakeSnapshot()
    {
        File tmpSnapshot = new File( dir, "tmpSnapshot" );

        try (FileOutputStream fos = new FileOutputStream( tmpSnapshot );
            ZipOutputStream zip = new ZipOutputStream( fos );
            Output output = new Output( zip ))
        {
            zip.putNextEntry( new ZipEntry( "file" ) );
            output.writeVarLong( lastCommandId, true );
            kryo.writeClassAndObject( output, root );

            output.flush();
        }
        catch( Exception e )
        {
            throw new IllegalStateException( e );
        }

        try
        {
            snapshot.delete();

            Files.move( tmpSnapshot.toPath(), snapshot.toPath() );

            lastCommandId++;
            outputStream.close();
            startOutput();
        }
        catch( IOException e )

        {
            throw new IllegalStateException( e );
        }

    }

    private void writeLocked( Runnable runnable )
    {
        lock.writeLock().lock();
        try
        {
            runnable.run();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}
