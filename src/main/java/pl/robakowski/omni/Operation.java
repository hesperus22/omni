package pl.robakowski.omni;

import java.io.Serializable;
import java.time.Instant;

@FunctionalInterface
public interface Operation<ROOT, RESULT> extends Serializable {
    RESULT perform(ROOT root, Instant now);
}
