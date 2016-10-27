package pl.robakowski.omni.jmh.omni.data;

import pl.robakowski.omni.VoidOperation;

import java.time.Instant;
import java.util.List;

/**
 * Created by probakowski on 2016-10-27.
 */
public class AddWithPets implements VoidOperation<List<Person>> {
    @Override
    public void run(List<Person> people, Instant now) {
        Person person = new Person();
        people.add(person);
        for (int j = 0; j < 100; j++) {
            Pet pet = new Pet();
            pet.setName("a");
            person.addPet(pet);
        }
        if (people.size() % 5 == 0) {
            Pet pet = new Pet();
            pet.setName("b");
            person.addPet(pet);
        }
    }
}
