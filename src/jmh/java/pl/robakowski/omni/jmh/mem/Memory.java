package pl.robakowski.omni.jmh.mem;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.omni.data.Person;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Memory {

    @State(Scope.Benchmark)
    public static class TestState {
        public List<Person> o = Collections.singletonList(new Person());
    }

    @Benchmark
    public int test(TestState omni) throws ExecutionException, InterruptedException {
        synchronized (omni) {
            Person person = omni.o.get(0);
            person.setAge(person.getAge() + 1);
            return person.getAge();
        }
    }
}
