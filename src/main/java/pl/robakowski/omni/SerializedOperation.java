package pl.robakowski.omni;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Created by probakowski on 2016-10-19.
 */
class SerializedOperation<T, E>
{
    private final CompletableFuture<E> future;
    private final Instant clock;
    private final Operation<T, E> operation;

    SerializedOperation( Operation<T, E> operation, Instant clock, CompletableFuture<E> future )
    {
        this.clock = clock;
        this.operation = operation;
        this.future = future;
    }

    Instant getClock()
    {
        return clock;
    }

    Operation<T, E> getOperation()
    {
        return operation;
    }

    CompletableFuture<E> getFuture()
    {
        return future;
    }
}
