import javaslang.control.Try;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public interface VoidOperation<E> extends Operation<E, Void>
{
    void run( E root, Instant now );

    @Override
    default Try<Void> perform( E root, Instant now )
    {
        run( root, now );
        return Try.success( null );
    }
}
