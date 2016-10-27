package pl.robakowski.omni.jmh.db;

import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.jmh.db.data.*;

import javax.persistence.*;

public class JpaJoinAdd {

    @State(Scope.Thread)
    public static class JpaState {
        private static volatile EntityManagerFactory entityManagerFactory;

        static {
            try {
                entityManagerFactory = Persistence.createEntityManagerFactory("joinadd");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public EntityManager em = entityManagerFactory.createEntityManager();
        public long id;

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
    public void test(JpaState state) {
        state.em.getTransaction().begin();
        Person person = new Person();

        for (int j = 0; j < 100; j++) {
            Pet pet = new Pet();
            pet.setName("a");
            pet.setOwner(person);
        }
        if (Math.random() < 0.2) {
            Pet pet = new Pet();
            pet.setName("b");
            pet.setOwner(person);
        }

        state.em.persist(person);
        state.em.flush();
        state.em.getTransaction().commit();
    }
}
