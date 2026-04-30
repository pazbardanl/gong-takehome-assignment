package io.gong.domain;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

public class MultiPersonBusySlot {
    private final Set<String> persons;
    private final LocalTime start;
    private final LocalTime end;

    public MultiPersonBusySlot(Set<String> persons, LocalTime start, LocalTime end) {
        this.persons = persons;
        this.start = start;
        this.end = end;
    }

    public Set<String> persons() {
        return persons;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiPersonBusySlot that = (MultiPersonBusySlot) o;
        return Objects.equals(persons, that.persons) && Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persons, start, end);
    }

    @Override
    public String toString() {
        return "MultiPersonBusySlot{" + "persons=" + persons + ", start=" + start + ", end=" + end + '}';
    }
}
