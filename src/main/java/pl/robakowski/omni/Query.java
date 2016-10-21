package pl.robakowski.omni;

import java.util.concurrent.CompletableFuture;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public interface Query<T, E>
{
    E perform( T root );
}
