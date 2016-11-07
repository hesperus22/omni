package pl.robakowski.omni;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

class SerializedOperation<ROOT, RESULT> {
    private final CompletableFuture<Boolean> future = new CompletableFuture<>();
    private Instant clock = Instant.now();
    private final Operation<ROOT, RESULT> operation;

    SerializedOperation(Operation<ROOT, RESULT> operation) {
        this.operation = operation;
    }

    SerializedOperation(Operation<ROOT, RESULT> operation, Instant clock) {
        this(operation);
        this.clock = clock;
    }

    Instant getClock() {
        return clock;
    }

    Operation<ROOT, RESULT> getOperation() {
        return operation;
    }

    CompletableFuture<Boolean> getFuture() {
        return future;
    }

    RESULT perform(ROOT root) {
        return operation.perform(root, clock);
    }
}
