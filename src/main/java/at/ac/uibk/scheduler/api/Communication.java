package at.ac.uibk.scheduler.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Communication {

    private static final AtomicLong atomicLong = new AtomicLong();

    public enum Type {
        REGULAR,
        CONDITIONAL,
        PARALLEL_FOR,
        PARALLEL;
    }

    private final long id;

    private Type type;

    public Communication() {
        this.id = Communication.atomicLong.getAndIncrement();
        this.type = Type.REGULAR;
    }

    public Communication(final Type type) {
        this();
        this.type = type;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Communication)) {
            return false;
        }
        final Communication that = (Communication) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
