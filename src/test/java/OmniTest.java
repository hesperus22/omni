import javaslang.control.Try;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

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

        Try<String> query = omni1.query( ( root, now ) -> Try.success( root.getName() ) );
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
        Try<String> query = omni1.query( ( root, now ) -> Try.success( root.getName() ) );
        Assert.assertTrue( query.isSuccess() );
        Assert.assertEquals( "person", query.get() );

    }
}
