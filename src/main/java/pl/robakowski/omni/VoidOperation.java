package pl.robakowski.omni;

import java.time.Instant;

@FunctionalInterface
public interface VoidOperation<E> extends Operation<E, Void> {
    void run(E root, Instant now);

    @Override
    default Void perform(E root, Instant now) {
        run(root, now);
        return null;
    }
}
