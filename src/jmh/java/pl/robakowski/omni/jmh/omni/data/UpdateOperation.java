package pl.robakowski.omni.jmh.omni.data;

import pl.robakowski.omni.Operation;

import java.time.Instant;
import java.util.List;

/**
 * Created by probakowski on 2016-10-21.
 */
public class UpdateOperation implements Operation<List<Person>, Integer>
{
    @Override
    public Integer perform( List<Person> root, Instant now )
    {
        Person person = root.get( 0 );
        person.setAge( person.getAge() + 1 );
        return person.getAge();
    }
}
