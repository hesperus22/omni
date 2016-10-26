package pl.robakowski.omni.jmh.omni.data;

import pl.robakowski.omni.Operation;

import java.time.Instant;
import java.util.List;

public class Update implements Operation<List<Person>, Integer> {
    @Override
    public Integer perform(List<Person> root, Instant now) {
        Person person = root.get(0);
        person.setAge(person.getAge() + 1);
        return person.getAge();
    }
}
