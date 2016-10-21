package pl.robakowski.omni.jmh;

import com.esotericsoftware.kryo.Kryo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import pl.robakowski.omni.Omni;
import pl.robakowski.omni.OmniBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by hesperus on 2016-10-20.
 */
@Fork( 0 )
@Warmup( time = 2, iterations = 5 )
@Measurement( time = 5, iterations = 3 )
@Threads( 4 )
public class Omni2PT
{
    @State( Scope.Benchmark )
    public static class TestState extends OmniState
    {
        @Override
        protected String getPath()
        {
            return "build/o2";
        }

        @Override
        protected void configureKryo( Kryo kryo )
        {
            kryo.register( SlowUpdateOperation.class );
        }
    }

    @Benchmark
    public int testMethod( TestState omni ) throws ExecutionException, InterruptedException
    {
        return omni.o.executeAndQuery( new SlowUpdateOperation() ).get();
    }
}
