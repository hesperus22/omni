package pl.robakowski.omni.jmh.omni;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.omni.data.*;

import java.util.concurrent.ExecutionException;

public class OmniSlow {
    @State(Scope.Benchmark)
    public static class TestState extends OmniState {
        @Override
        protected String getPath() {
            return "build/o2";
        }
    }

    @Benchmark
    public int test(TestState omni) throws ExecutionException, InterruptedException {
        return omni.o.executeAndQuery(new UpdateOperation(), true);
    }
}
