package pl.robakowski.omni;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

class SerializedOperation<T, E> {
    private final CompletableFuture<Boolean> future = new CompletableFuture<>();
    private final Instant clock = Instant.now();
    private final Operation<T, E> operation;

    SerializedOperation(Operation<T, E> operation) {
        this.operation = operation;
    }

    Instant getClock() {
        return clock;
    }

    Operation<T, E> getOperation() {
        return operation;
    }

    CompletableFuture<Boolean> getFuture() {
        return future;
    }

    E perform(T root) {
        return operation.perform(root, clock);
    }
}
