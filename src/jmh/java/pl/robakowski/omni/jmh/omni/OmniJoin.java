package pl.robakowski.omni.jmh.omni;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.omni.data.*;

import java.util.*;
import java.util.stream.Collectors;

public class OmniJoin {
    @State(Scope.Benchmark)
    public static class TestState extends OmniState {
        @Override
        protected String getPath() {
            return "build/o4";
        }

        @Override
        protected List<Person> getPeople() {
            ArrayList<Person> people = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Person person = new Person();
                people.add(person);
                for (int j = 0; j < 100; j++) {
                    Pet pet = new Pet();
                    pet.setName("a");
                    person.addPet(pet);
                }
                if (i % 5 == 0) {
                    Pet pet = new Pet();
                    pet.setName("b");
                    person.addPet(pet);
                }
            }
            return people;
        }
    }

    @Benchmark
    public Collection<Person> testIndexed(TestState omni) {
        return omni.o.query(people -> people.stream().filter(Person::hasPetNamedB).collect(Collectors.toList()));
    }

    @Benchmark
    public Collection<Person> testNotIndexed(TestState omni) {
        return omni.o.query(people -> people.stream().filter(p -> p.getPets().stream().anyMatch(pet -> Objects.equals
                (pet.getName(), "b"))).collect(Collectors.toList()));
    }
}
