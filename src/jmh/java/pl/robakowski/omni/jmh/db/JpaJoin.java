package pl.robakowski.omni.jmh.db;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.db.data.*;

import javax.persistence.*;
import java.util.*;

public class JpaJoin {

    @State(Scope.Thread)
    public static class JpaState {
        private static volatile EntityManagerFactory entityManagerFactory;

        static {
            try {
                entityManagerFactory = Persistence.createEntityManagerFactory("join");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public EntityManager em = entityManagerFactory.createEntityManager();
        public long id;

        @Setup
        public void setup() {
            em.getTransaction().begin();
            for (int i = 0; i < 1000; i++) {
                Person person = new Person();

                for (int j = 0; j < 100; j++) {
                    Pet pet = new Pet();
                    pet.setName("a");
                    pet.setOwner(person);
                }
                if (i % 5 == 0) {
                    Pet pet = new Pet();
                    pet.setName("b");
                    pet.setOwner(person);
                }

                em.persist(person);
            }
            em.flush();
            em.getTransaction().commit();
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
    public List<Person> test(JpaState state) {
        TypedQuery<Person> query = state.em.createQuery("select p from Person p inner join p.pets pet where pet" +
                ".name like 'b'", Person.class);
        return new ArrayList<>(query.getResultList());
    }
}
