package pl.robakowski.omni.jmh.omni.data;

import java.util.*;

public class Person {
    private int age;
    private int bPets;
    private final Set<Pet> pets = new HashSet<>();

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Set<Pet> getPets() {
        return Collections.unmodifiableSet(pets);
    }

    public void addPet(Pet pet) {
        if ("b".equals(pet.getName())) {
            bPets++;
        }
        pets.add(pet);
    }

    public void removePet(Pet pet) {
        if ("b".equals(pet.getName())) {
            bPets--;
        }
        pets.add(pet);
    }

    public boolean hasPetNamedB() {
        return bPets > 0;
    }
}
