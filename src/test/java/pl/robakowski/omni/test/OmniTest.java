package pl.robakowski.omni.test;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import pl.robakowski.omni.*;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class OmniTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldHaveEmptyInitialState() throws IOException, ExecutionException, InterruptedException {
        try (Omni<Person> omni = OmniBuilder.builder(Person::new).path(folder.newFolder()).build()) {
            String query = omni.query(Person::getName);
            Assert.assertNull(query);
        }
    }

    @Test
    public void shouldStoreOperation() throws IOException, ExecutionException, InterruptedException {
        OmniBuilder<Person> builder = OmniBuilder.builder(Person::new).path(folder.newFolder());

        try (Omni<Person> omni = builder.build()) {
            omni.execute((root, now) -> root.setName("person"));
        }

        try (Omni<Person> omni = builder.build()) {
            String query = omni.query(Person::getName);

            Assert.assertEquals("person", query);
        }
    }

    @Test
    public void shouldStoreOperationTime() throws IOException, InterruptedException, ExecutionException {
        OmniBuilder<Person> builder = OmniBuilder.builder(Person::new).path(folder.newFolder());
        Instant start = Instant.now();

        try (Omni<Person> omni = builder.build()) {
            omni.execute(Person::setCreatedAt);
            Thread.sleep(2000);
        }

        try (Omni<Person> omni = builder.build()) {
            Instant query = omni.query(Person::getCreatedAt);

            Assert.assertEquals(start.truncatedTo(ChronoUnit.SECONDS),
                    query.truncatedTo(ChronoUnit.SECONDS));
        }
    }

    @Test
    public void shouldStoreSnapshot() throws IOException, ExecutionException, InterruptedException {
        File folder = this.folder.newFolder();
        OmniBuilder<Person> builder = OmniBuilder.builder(Person::new).path(folder);

        try (Omni<Person> omni = builder.build()) {

            omni.execute((root, now) -> root.setName("person"));
            omni.execute((root, now) -> root.setName(root.getName() + "person"));

            omni.takeSnapshot();

            omni.execute((root, now) -> root.setName(root.getName() + "person"));

            Assert.assertTrue(new File(folder, "snapshot").exists());
        }

        try (Omni<Person> omni = builder.build()) {
            String query = omni.query(Person::getName);

            Assert.assertEquals("personpersonperson", query);
        }
    }

    @Test
    public void shouldUseSnapshot() throws IOException, ExecutionException, InterruptedException {
        File folder = this.folder.newFolder();
        OmniBuilder<Person> builder = OmniBuilder.builder(Person::new).path(folder);

        try (Omni<Person> omni = builder.build()) {

            omni.execute((root, now) -> root.setName("person"));
            omni.execute((root, now) -> root.setName(root.getName() + "person"));

            omni.takeSnapshot();

            omni.execute((root, now) -> root.setName(root.getName() + "person"));

            Assert.assertTrue(new File(folder, "snapshot").exists());

            Stream.of(Optional.ofNullable(folder.listFiles()).orElse(new File[0]))
                    .filter(f -> f.getName().contains("1") | f.getName().contains("2")).forEach(File::delete);
        }

        try (Omni<Person> omni = builder.build()) {
            String query = omni.query(Person::getName);

            Assert.assertEquals("personpersonperson", query);
        }
    }

    @Test
    public void shouldWorkInMultithreadedMode() throws IOException, InterruptedException, ExecutionException {
        File folder = this.folder.newFolder();
        OmniBuilder<Person> builder = OmniBuilder.builder(Person::new).configureKryo(k -> k.register(UpdateAge.class))
                .path(folder);

        try (Omni<Person> omni = builder.build()) {
            CountDownLatch latch = new CountDownLatch(50);
            CountDownLatch start = new CountDownLatch(1);

            ExecutorService executorService = Executors.newFixedThreadPool(50);

            for (int i = 0; i < 50; i++) {
                executorService.submit(() ->
                {
                    start.await();
                    for (int j = 0; j < 1000; j++) {
                        omni.execute(new UpdateAge());
                    }
                    latch.countDown();
                    return null;
                });
            }

            start.countDown();
            latch.await();

            int integer = omni.query(Person::getAge);
            Assert.assertEquals(50000, integer);
        }

        try (Omni<Person> omni = builder.build()) {
            int integer = omni.query(Person::getAge);
            Assert.assertEquals(50000, integer);

            omni.takeSnapshot();
        }

        try (Omni<Person> omni = builder.build()) {
            omni.execute(new UpdateAge());
            int integer = omni.query(Person::getAge);
            Assert.assertEquals(50001, integer);
        }

    }

    public static class UpdateAge implements VoidOperation<Person> {
        @Override
        public void run(Person root, Instant now) {
            root.setAge(root.getAge() + 1);
        }
    }
}
