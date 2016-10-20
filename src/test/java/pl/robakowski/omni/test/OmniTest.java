package pl.robakowski.omni.test;

import javaslang.control.Try;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pl.robakowski.omni.Omni;
import pl.robakowski.omni.OmniBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * Created by probakowski on 2016-10-19.
 */
public class OmniTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldHaveEmptyInitialState() throws IOException
    {
        OmniBuilder<Person> builder =
            OmniBuilder.builder( Person::new ).register( Person.class ).path( folder.newFolder().getPath() );

        Omni<Person> omni1 = builder.build();

        Try<String> query = omni1.query( root -> Try.success( root.getName() ) );
        Assert.assertTrue( query.isSuccess() );
        Assert.assertNull( query.get() );

    }

    @Test
    public void shouldStoreOperation() throws IOException
    {
        OmniBuilder<Person> builder =
            OmniBuilder.builder( Person::new ).register( Person.class ).path( folder.newFolder().getPath() );

        Omni<Person> omni = builder.build();
        Try<Void> result = omni.execute( ( root, now ) -> root.setName( "person" ) );
        result.onFailure( t ->
        {
            throw new IllegalStateException( t );
        } );

        Omni<Person> omni1 = builder.build();
        Try<String> query = omni1.query( root -> Try.success( root.getName() ) );
        Assert.assertTrue( query.isSuccess() );
        Assert.assertEquals( "person", query.get() );
    }

    @Test
    public void shouldStoreOperationTime() throws IOException, InterruptedException
    {
        OmniBuilder<Person> builder =
            OmniBuilder.builder( Person::new ).register( Person.class ).path( folder.newFolder().getPath() );

        Omni<Person> omni = builder.build();
        Instant start = Instant.now();
        Try<Void> result = omni.execute( Person::setCreatedAt );
        result.onFailure( t ->
        {
            throw new IllegalStateException( t );
        } );

        Thread.sleep( 2000 );

        Omni<Person> omni1 = builder.build();
        Try<Instant> query = omni1.query( root -> Try.success( root.getCreatedAt() ) );
        Assert.assertTrue( query.isSuccess() );
        Assert.assertEquals( start.truncatedTo( ChronoUnit.SECONDS ), query.get().truncatedTo( ChronoUnit.SECONDS ) );
    }

    @Test
    public void shouldStoreSnapshot() throws IOException
    {
        File folder = this.folder.newFolder();
        OmniBuilder<Person> builder =
            OmniBuilder.builder( Person::new ).register( Person.class ).path( folder.getPath() );

        Omni<Person> omni = builder.build();

        omni.execute( (root, now)->root.setName( "person" ) ).get();
        omni.execute( (root, now)->root.setName( root.getName()+"person" ) ).get();

        omni.takeSnapshot();

        omni.execute( (root, now)->root.setName( root.getName()+"person" ) ).get();

        Assert.assertTrue( new File(folder, "snapshot.zip").exists() );

        omni = builder.build();
        Try<String> query = omni.query( root -> Try.success( root.getName() ));

        Assert.assertEquals( "personpersonperson", query.get() );
    }

    @Test
    public void shouldUseSnapshot() throws IOException
    {
        File folder = this.folder.newFolder();
        OmniBuilder<Person> builder =
            OmniBuilder.builder( Person::new ).register( Person.class ).path( folder.getPath() );

        Omni<Person> omni = builder.build();

        omni.execute( (root, now)->root.setName( "person" ) ).get();
        omni.execute( (root, now)->root.setName( root.getName()+"person" ) ).get();

        omni.takeSnapshot();

        omni.execute( (root, now)->root.setName( root.getName()+"person" ) ).get();

        Assert.assertTrue( new File(folder, "snapshot.zip").exists() );

        Stream.of( folder.listFiles()).filter( f->f.getName().contains( "1" )|f.getName().contains( "2" ) ).forEach( File::delete );

        omni = builder.build();
        Try<String> query = omni.query( root -> Try.success( root.getName() ));

        Assert.assertEquals( "personpersonperson", query.get() );
    }
}
