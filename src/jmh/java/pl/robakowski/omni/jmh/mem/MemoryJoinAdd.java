package pl.robakowski.omni.jmh.mem;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.db.data.*;

import java.util.*;

public class MemoryJoinAdd {

    @State(Scope.Thread)
    public static class MemState {
        public List<Person> o = new LinkedList<>();
    }

    @Benchmark
    public void test(MemState state) {
        synchronized (MemoryJoinAdd.class) {
            if (state.o.size() > 3000)
                state.o.clear();
            Person person = new Person();

            for (int j = 0; j < 100; j++) {
                Pet pet = new Pet();
                pet.setName("a");
                pet.setOwner(person);
            }
            if (state.o.size() % 5 == 0) {
                Pet pet = new Pet();
                pet.setName("b");
                pet.setOwner(person);
            }

            state.o.add(person);
        }
    }
}
