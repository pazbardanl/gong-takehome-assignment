package io.gong.domain;

import java.time.LocalTime;
import java.util.Objects;

public final class BusySlot {

    private final LocalTime start;
    private final LocalTime end;
    private final String person;
    private final String eventSubject;

    public BusySlot(LocalTime start, LocalTime end, String person, String eventSubject) {
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        this.person = Objects.requireNonNull(person, "person");
        this.eventSubject = Objects.requireNonNull(eventSubject, "eventSubject");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    public String person() {
        return person;
    }

    public String eventSubject() {
        return eventSubject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BusySlot busySlot = (BusySlot) o;
        return start.equals(busySlot.start) && end.equals(busySlot.end) && person.equals(busySlot.person) && eventSubject.equals(busySlot.eventSubject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, person, eventSubject);
    }

    @Override
    public String toString() {
        return "BusySlot[" + start + "–" + end + "] " + person + " - " + eventSubject;
    }
}
