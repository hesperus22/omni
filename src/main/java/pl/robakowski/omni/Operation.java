package pl.robakowski.omni;

import java.io.Serializable;
import java.time.Instant;

@FunctionalInterface
public interface Operation<T, E> extends Serializable
{
    E perform( T root, Instant now );
}
