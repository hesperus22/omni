package pl.robakowski.omni;

import javaslang.control.Try;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public interface Query<T, E>
{
    Try<E> perform( T root );
}
