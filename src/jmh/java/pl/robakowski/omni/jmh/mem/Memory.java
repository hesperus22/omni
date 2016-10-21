package pl.robakowski.omni.jmh.mem;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import pl.robakowski.omni.jmh.omni.data.Person;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by probakowski on 2016-10-21.
 */
public class Memory
{
    @State( Scope.Benchmark )
    public static class TestState
    {
        public List< Person > o = Collections.singletonList( new Person() );
    }

    @Benchmark
    public int test( TestState omni ) throws ExecutionException, InterruptedException
    {
        Person person = omni.o.get( 0 );
        person.setAge( person.getAge() + 1 );
        return person.getAge();
    }
}
