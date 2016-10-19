import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import javaslang.control.Try;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Comparator;
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
    private final Kryo kryo;
    private final File dir;
    private T root;
    private final File snapshot;
    private long lastCommandId;

    public Omni( Kryo kryo, String path, Supplier<T> instanceCreator )
    {
        this.kryo = kryo;
        dir = new File( path );
        snapshot = new File( dir, "snapshot.zip" );
        if( snapshot.exists() )
        {

            try (ZipInputStream in = new ZipInputStream( new FileInputStream( snapshot ) );
                Input input = new Input( new BufferedInputStream( in ) ))
            {
                in.getNextEntry();
                lastCommandId = input.readVarLong( true );
                root = (T)kryo.readClassAndObject( input );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( e );
            }
        }
        else
        {
            root = instanceCreator.get();
        }

        writeLocked( this::loadOperations );
    }

    private Object loadOperations()
    {
        Stream.of( dir.listFiles() ).filter( f -> f.getName().matches( "[0-9]+" ) )
            .filter( f -> Long.parseLong( f.getName() ) > lastCommandId )
            .sorted( Comparator.comparing( f -> Long.parseLong( f.getName() ) ) ).forEach( f ->
        {
            try (Input input = new Input( new BufferedInputStream( new FileInputStream( f ) ) ))
            {
                lastCommandId = Long.parseLong( f.getName() );
                SerializedOperation<T, ?> operation = kryo.readObject( input, SerializedOperation.class );

                operation.getOperation().perform( root, operation.getClock() );
            }
            catch( FileNotFoundException e )
            {
                throw new IllegalStateException( e );
            }
        } );

        return null;
    }

    public <E> Try<E> query( Operation<T, E> query )
    {
        return readLocked( () -> query.perform( root, Instant.now() ) );
    }

    public Try<Void> execute( VoidOperation<T> operation )
    {
        return executeAndQuery( operation );
    }

    public <E> Try<E> executeAndQuery( Operation<T, E> operation )
    {
        return writeLocked( () -> lockedExecuteAndQuery( operation ) );
    }

    private <E> Try<E> lockedExecuteAndQuery( Operation<T, E> operation )
    {
        Instant now = Instant.now();
        lastCommandId++;
        File opFile = new File( dir, Long.toString( lastCommandId ) );

        try (FileOutputStream out = new FileOutputStream( opFile );
            Output output = new Output( new BufferedOutputStream( out ) ))
        {
            kryo.writeObject( output, new SerializedOperation<>( now, operation ) );
            output.flush();
            out.getFD().sync();
            return operation.perform( root, now );
        }
        catch( Exception e )
        {
            return Try.failure( e );
        }
    }

    public void takeSnapshot()
    {
        writeLocked( this::lockedTakeSnapshot );
    }

    private Try<Object> lockedTakeSnapshot()
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
            fos.getFD().sync();
        }
        catch( Exception e )
        {
            return Try.failure( e );
        }

        snapshot.delete();
        try
        {
            Files.move(tmpSnapshot.toPath(), snapshot.toPath());
        }
        catch( IOException e )
        {
            return Try.failure( e );
        }

        return Try.success( null );
    }

    private <E> E readLocked( Supplier<E> runnable )
    {
        lock.readLock().lock();
        try
        {
            return runnable.get();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private <E> E writeLocked( Supplier<E> runnable )
    {
        lock.writeLock().lock();
        try
        {
            return runnable.get();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}

