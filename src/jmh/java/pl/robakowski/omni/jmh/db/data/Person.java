package pl.robakowski.omni.jmh.db.data;

import javax.persistence.*;
import java.util.*;

@Table(name = "person")
@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "age")
    private int age;

    @OneToMany(mappedBy = "id")
    private Set<Pet> pets = new HashSet<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Set<Pet> getPets() {
        return pets;
    }
}
