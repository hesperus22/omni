package pl.robakowski.omni.test;

import java.time.Instant;

public class Person {
    private String name;
    private Instant createdAt;
    private int age;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    int getAge() {
        return age;
    }

    void setAge(int age) {
        this.age = age;
    }
}
