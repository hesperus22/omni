import javaslang.control.Try;

import java.io.Serializable;
import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
@FunctionalInterface
public interface Operation<T, E> extends Serializable
{
    Try<E> perform( T root, Instant now );
}
