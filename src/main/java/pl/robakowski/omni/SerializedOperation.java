package pl.robakowski.omni;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
class SerializedOperation<T, E>
{
    private Instant clock;
    private Operation<T, E> operation;

    SerializedOperation( Instant clock, Operation<T, E> operation )
    {
        this.clock = clock;
        this.operation = operation;
    }

    Instant getClock()
    {
        return clock;
    }

    Operation<T, E> getOperation()
    {
        return operation;
    }
}
