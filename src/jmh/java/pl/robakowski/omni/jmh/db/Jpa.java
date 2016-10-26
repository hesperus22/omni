package pl.robakowski.omni.jmh.db;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.db.data.Person;

import javax.persistence.*;

public class Jpa {

    @State(Scope.Thread)
    public static class JpaState {
        private static volatile EntityManagerFactory entityManagerFactory;

        static {
            try {
                entityManagerFactory = Persistence.createEntityManagerFactory("persistence");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public EntityManager em = entityManagerFactory.createEntityManager();
        public long id;

        @Setup
        public void setup() {
            Person entity = new Person();
            em.persist(entity);
            id = entity.getId();
        }

        @TearDown
        public synchronized void tearDown() {
            em.close();
            try {
                if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                    entityManagerFactory.close();
                    entityManagerFactory = null;
                }
            } catch (Exception e) {
            }
        }
    }

    @Benchmark
    public int test(JpaState state) {
        state.em.getTransaction().begin();
        Person person = state.em.find(Person.class, state.id);
        person.setAge(person.getAge() + 1);
        state.em.flush();
        state.em.getTransaction().commit();
        return person.getAge();
    }
}
