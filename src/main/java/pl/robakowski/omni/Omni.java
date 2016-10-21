package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Comparator.comparing;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class Omni< T >
{
    private final BlockingQueue< SerializedOperation< T, ? > > queue = new ArrayBlockingQueue<>( 10 );
    private final StampedLock lock = new StampedLock();
    private final Kryo kryo;
    private final File dir;
    private final File snapshot;
    private FileOutputStream outputStream;
    private Output output;

    private Thread getTimerThread( Runnable r )
    {
        Thread t = new Thread( r );
        t.setDaemon( true );
        t.setName( "omni-timer" );
        return t;
    }

    private T root;
    private volatile long lastCommandId = 0;

    Omni( Kryo kryo, String path, Supplier< T > instanceCreator )
    {
        this.kryo = kryo;
        dir = new File( path );
        snapshot = new File( dir, "snapshot.zip" );
        root = snapshot.exists() ? loadSnapshot() : instanceCreator.get();

        loadOperations();
        lastCommandId++;
        startOutput();
        newSingleThreadScheduledExecutor( this::getTimerThread )
            .scheduleAtFixedRate( this::sync, 0, 1, TimeUnit.SECONDS );
    }

    public void sync()
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

    public < E > CompletableFuture< E > query( Query< T, E > query )
    {
        long stamp = lock.readLock();
        try
        {
            return CompletableFuture.completedFuture( query.perform( root ) );
        }
        catch( Exception e )
        {
            CompletableFuture< E > completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally( e );
            return completableFuture;
        }
        finally
        {
            lock.unlock( stamp );
        }
    }

    public CompletableFuture< Void > execute( VoidOperation< T > operation )
    {
        return executeAndQuery( operation );
    }

    public < E > CompletableFuture< E > executeAndQuery( Operation< T, E > operation )
    {
        CompletableFuture< E > ret = new CompletableFuture<>();
        writeToFile( operation, ret );
        lockedExecuteAndQuery();
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

    private synchronized < E > void writeToFile( Operation< T, E > operation, CompletableFuture< E > ret )
    {
        Instant now = Instant.now();

        kryo.writeObject( output, now );
        kryo.writeClassAndObject( output, operation );
        output.flush();

        queue.add( new SerializedOperation<>( operation, now, ret ) );
    }

    private T loadSnapshot()
    {
        try (Input input = new Input( new GZIPInputStream( new FileInputStream( snapshot ) ) ))
        {
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
                Operation< T, ? > operation = (Operation< T, ? >)kryo.readClassAndObject( input );

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

    private < E > void lockedExecuteAndQuery()
    {
        long stamp = lock.writeLock();
        SerializedOperation< T, E > poll = (SerializedOperation< T, E >)queue.poll();
        try
        {
            poll.getFuture().complete( poll.getOperation().perform( root, poll.getClock() ) );
        }
        catch( Exception e )
        {
            poll.getFuture().completeExceptionally( e );
        }
        finally
        {
            lock.unlock( stamp );
        }
    }

    public void takeSnapshot()
    {
        File tmpSnapshot = new File( dir, "tmpSnapshot" );

        long stamp = lock.writeLock();
        try
        {
            try (Output output = new Output( new GZIPOutputStream( new FileOutputStream( tmpSnapshot ) ) ))
            {
                output.writeVarLong( lastCommandId, true );
                kryo.writeClassAndObject( output, root );
                lastCommandId++;
            }
            catch( Exception e )
            {
                lastCommandId--;
                throw new IllegalStateException( e );
            }

            try
            {
                snapshot.delete();

                Files.move( tmpSnapshot.toPath(), snapshot.toPath() );

                outputStream.close();
                startOutput();
            }
            catch( IOException e )
            {
                throw new IllegalStateException( e );
            }
        }
        finally
        {
            lock.unlock( stamp );
        }

    }

}
