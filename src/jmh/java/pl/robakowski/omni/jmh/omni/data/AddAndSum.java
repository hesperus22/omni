package pl.robakowski.omni.jmh.omni.data;

import pl.robakowski.omni.Operation;

import java.time.Instant;
import java.util.List;

/**
 * Created by probakowski on 2016-10-26.
 */
public class AddAndSum implements Operation<List<Person>, Integer> {
    @Override
    public Integer perform(List<Person> root, Instant now) {
        Person person = new Person();
        person.setAge(1);
        root.add(person);
        return root.stream().mapToInt(Person::getAge).sum();
    }
}
