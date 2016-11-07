package pl.robakowski.omni;

import java.time.Instant;

@FunctionalInterface
public interface VoidOperation<ROOT> extends Operation<ROOT, Void> {
    void run(ROOT root, Instant now);

    @Override
    default Void perform(ROOT root, Instant now) {
        run(root, now);
        return null;
    }
}
