package pl.robakowski.omni.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import pl.robakowski.omni.Omni;

/**
 * Created by hesperus on 2016-10-20.
 */
public class OmniPT {

    @State(Scope.Group)
    public static class OmniState {
    }

    @Benchmark
    public void testMethod() {

    }
}
