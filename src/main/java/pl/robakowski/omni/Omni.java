package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import static java.util.Comparator.comparing;
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
    private final File snapshot;
    private File ops;
    private T root;
    private volatile long lastCommandId = 0;

    Omni( Kryo kryo, String path, Supplier<T> instanceCreator )
    {
        this.kryo = kryo;
        dir = new File( path );
        snapshot = new File( dir, "snapshot.zip" );
        root = snapshot.exists() ? loadSnapshot() : instanceCreator.get();

        ops = new File( dir, String.valueOf( lastCommandId ) );

        loadOperations();
    }

    public <E> Try<E> query( Query<T, E> query )
    {
        return readLocked( () -> query.perform( root ) );
    }

    public Try<Void> execute( VoidOperation<T> operation )
    {
        return executeAndQuery( operation );
    }

    public <E> Try<E> executeAndQuery( Operation<T, E> operation )
    {
        return writeLocked( () -> lockedExecuteAndQuery( operation ) );
    }

    private T loadSnapshot()
    {
        try (ZipInputStream in = new ZipInputStream( new FileInputStream( snapshot ) );
            Input input = new Input( new BufferedInputStream( in ) ))
        {
            in.getNextEntry();
            lastCommandId = input.readVarLong( true );
            return (T)this.kryo.readClassAndObject( input );
        }
        catch( Exception e )
        {
            throw new IllegalStateException( e );
        }
    }

    private void loadOperations()
    {
        Stream.of( dir.listFiles() ).filter( this::isOperation ).filter( this::isAfterSnapshot )
            .sorted( comparing( this::getId ) ).forEach( this::loadOperation );
    }

    private void loadOperation( File f )
    {
        try (Input input = new Input( new BufferedInputStream( new FileInputStream( f ) ) ))
        {
            lastCommandId = getId( f );
            SerializedOperation<T, ?> operation = kryo.readObject( input, SerializedOperation.class );

            operation.getOperation().perform( root, operation.getClock() );
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
        return getId( file ) > lastCommandId;
    }

    private boolean isOperation( File file )
    {
        return file.getName().matches( "[0-9]+" );
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
            Files.move( tmpSnapshot.toPath(), snapshot.toPath() );
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
