package io.gong.contract;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public class AvailableSlot {
    private final LocalTime start;
    private final LocalTime end;

    public AvailableSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvailableSlot that = (AvailableSlot) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "AvailableSlot{" + "start=" + start + ", end=" + end + '}';
    }
}
