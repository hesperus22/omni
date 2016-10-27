package pl.robakowski.omni.jmh.omni;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.omni.data.*;

public class OmniJoinAdd {
    @State(Scope.Benchmark)
    public static class TestState extends OmniState {
        @Override
        protected String getPath() {
            return "build/o5";
        }
    }

    @Benchmark
    public void test(TestState omni) {
        omni.o.execute(new AddWithPets());
    }
}
