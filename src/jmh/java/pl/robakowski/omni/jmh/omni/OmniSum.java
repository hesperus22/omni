package pl.robakowski.omni.jmh.omni;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.omni.data.*;

public class OmniSum {
    @State(Scope.Benchmark)
    public static class TestState extends OmniState {
        @Override
        protected String getPath() {
            return "build/o3";
        }
    }

    @Benchmark
    public int test(TestState omni) {
        return omni.o.executeAndQuery(new Update());
    }
}
