package pl.robakowski.omni;

import java.util.concurrent.CompletableFuture;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public interface VoidOperation<E> extends Operation<E, Void>
{
    void run( E root, Instant now );

    @Override
    default Void perform( E root, Instant now )
    {
        run( root, now );
        return null;
    }
}
