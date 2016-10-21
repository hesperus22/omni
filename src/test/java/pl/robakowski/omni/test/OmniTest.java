package pl.robakowski.omni.test;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pl.robakowski.omni.Omni;
import pl.robakowski.omni.OmniBuilder;
import pl.robakowski.omni.VoidOperation;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Created by probakowski on 2016-10-19.
 */
public class OmniTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldHaveEmptyInitialState() throws IOException, ExecutionException, InterruptedException
    {
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( Person.class ) )
                .path( folder.newFolder().getPath() );

        Omni< Person > omni1 = builder.build();

        CompletableFuture< String > query = omni1.query( Person::getName );
        Assert.assertNull( query.get() );

    }

    @Test
    public void shouldStoreOperation() throws IOException, ExecutionException, InterruptedException
    {
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( Person.class ) )
                .path( folder.newFolder().getPath() );

        Omni< Person > omni = builder.build();
        CompletableFuture< Void > result = omni.execute( ( root, now ) -> root.setName( "person" ) );
        result.get();

        Omni< Person > omni1 = builder.build();
        CompletableFuture< String > query = omni1.query( Person::getName );

        Assert.assertEquals( "person", query.get() );
    }

    @Test
    public void shouldStoreOperationTime() throws IOException, InterruptedException, ExecutionException
    {
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( Person.class ) )
                .path( folder.newFolder().getPath() );

        Omni< Person > omni = builder.build();
        Instant start = Instant.now();
        CompletableFuture< Void > result = omni.execute( Person::setCreatedAt );
        result.get();
        Thread.sleep( 2000 );

        Omni< Person > omni1 = builder.build();
        CompletableFuture< Instant > query = omni1.query( Person::getCreatedAt );

        Assert.assertEquals( start.truncatedTo( ChronoUnit.SECONDS ),
            query.get().truncatedTo( ChronoUnit.SECONDS ) );
    }

    @Test
    public void shouldStoreSnapshot() throws IOException, ExecutionException, InterruptedException
    {
        File folder = this.folder.newFolder();
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( Person.class ) )
                .path( folder.getPath() );

        Omni< Person > omni = builder.build();

        omni.execute( ( root, now ) -> root.setName( "person" ) ).get();
        omni.execute( ( root, now ) -> root.setName( root.getName() + "person" ) ).get();

        omni.takeSnapshot();

        omni.execute( ( root, now ) -> root.setName( root.getName() + "person" ) ).get();

        Assert.assertTrue( new File( folder, "snapshot.zip" ).exists() );

        omni = builder.build();
        CompletableFuture< String > query = omni.query( Person::getName );

        Assert.assertEquals( "personpersonperson", query.get() );
    }

    @Test
    public void shouldUseSnapshot() throws IOException, ExecutionException, InterruptedException
    {
        File folder = this.folder.newFolder();
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( Person.class ) )
                .path( folder.getPath() );

        Omni< Person > omni = builder.build();

        omni.execute( ( root, now ) -> root.setName( "person" ) ).get();
        omni.execute( ( root, now ) -> root.setName( root.getName() + "person" ) ).get();

        omni.takeSnapshot();

        omni.execute( ( root, now ) -> root.setName( root.getName() + "person" ) ).get();

        Assert.assertTrue( new File( folder, "snapshot.zip" ).exists() );

        Stream.of( folder.listFiles() )
            .filter( f -> f.getName().contains( "1" ) | f.getName().contains( "2" ) ).forEach( File::delete );

        omni = builder.build();
        CompletableFuture< String > query = omni.query( Person::getName );

        Assert.assertEquals( "personpersonperson", query.get() );
    }

    @Test
    public void shouldWorkInMultithreadedMode() throws IOException, InterruptedException, ExecutionException
    {
        File folder = this.folder.newFolder();
        OmniBuilder< Person > builder =
            OmniBuilder.builder( Person::new ).configureKryo( k -> k.register( UpdateAge.class ) )
                .path( folder.getPath() );

        Omni< Person > omni = builder.build();

        CountDownLatch latch = new CountDownLatch( 50 );
        CountDownLatch start = new CountDownLatch( 1 );

        ExecutorService executorService = Executors.newFixedThreadPool( 50 );

        Omni< Person > finalOmni = omni;
        for( int i = 0; i < 50; i++ )
        {
            executorService.submit( () ->
            {
                start.await();
                for( int j = 0; j < 1000; j++ )
                {
                    finalOmni.execute( new UpdateAge() );
                }
                latch.countDown();
                return null;
            } );
        }

        start.countDown();
        latch.await();

        int integer = omni.query( Person::getAge ).get();
        Assert.assertEquals( 50000, integer );

        omni = builder.build();
        integer = omni.query( Person::getAge ).get();
        Assert.assertEquals( 50000, integer );

        omni.takeSnapshot();

        omni = builder.build();
        omni.execute( new UpdateAge() );
        integer = omni.query( Person::getAge ).get();
        Assert.assertEquals( 50001, integer );
    }

    public static class UpdateAge implements VoidOperation< Person >
    {
        @Override public void run( Person root, Instant now )
        {
            root.setAge( root.getAge() + 1 );
        }
    }
}
