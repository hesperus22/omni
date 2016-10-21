package pl.robakowski.omni.jmh.omni;

import com.esotericsoftware.kryo.Kryo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import pl.robakowski.omni.jmh.omni.data.OmniState;
import pl.robakowski.omni.jmh.omni.data.UpdateOperation;

import java.util.concurrent.ExecutionException;

public class OmniFast
{

    @State( Scope.Benchmark )
    public static class TestState extends OmniState
    {
        @Override
        protected String getPath()
        {
            return "build/o1";
        }

        @Override
        protected void configureKryo( Kryo kryo )
        {
            kryo.register( UpdateOperation.class );
        }
    }

    @Benchmark
    public int test( TestState omni ) throws ExecutionException, InterruptedException
    {
        return omni.o.executeAndQuery( new UpdateOperation() ).get();
    }
}
