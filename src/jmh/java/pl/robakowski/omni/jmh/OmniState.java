package pl.robakowski.omni.jmh;

import com.esotericsoftware.kryo.Kryo;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import pl.robakowski.omni.Omni;
import pl.robakowski.omni.OmniBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by probakowski on 2016-10-21.
 */
public abstract class OmniState
{
    public Omni<List<Person>> o;

    protected abstract String getPath();

    protected abstract void configureKryo( Kryo kryo );

    protected Omni<List<Person>> getOmni()
    {
        return OmniBuilder.builder( () ->
        {
            List<Person> list = new ArrayList<>();
            list.add( new Person() );
            return list;
        } ).configureKryo( this::configureKryo ).path( getPath() ).build();
    }

    @Setup
    public void setup()
    {
        new File( getPath() ).mkdirs();
        o = getOmni();
    }

    @TearDown
    public void tearDown()
    {
        File dir = new File( getPath() );

        Stream.of( Optional.ofNullable( dir.listFiles() ).orElse( new File[ 0 ] ) ).forEach( File::delete );
        dir.delete();
    }
}
