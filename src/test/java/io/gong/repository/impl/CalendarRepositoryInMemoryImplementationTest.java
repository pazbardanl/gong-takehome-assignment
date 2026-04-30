package io.gong.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.gong.domain.BusySlot;

public class CalendarRepositoryInMemoryImplementationTest {

    private CalendarRepositoryInMemoryImplementation repository;

    @Before
    public void setUp() {
        repository = new CalendarRepositoryInMemoryImplementation();
    }

    @Test
    public void getBusySlots_whenEmpty_returnsEmpty() {
        assertTrue(repository.getBusySlots(Collections.singletonList("Alice")).isEmpty());
    }

    @Test
    public void storeBusySlots_thenGetBusySlots_returnsSlotsForThatPerson() {
        BusySlot a1 = new BusySlot(LocalTime.of(9, 0), LocalTime.of(10, 0), "Alice", "Standup");
        repository.storeBusySlots(Collections.singletonList(a1));

        List<BusySlot> got = repository.getBusySlots(Collections.singletonList("Alice"));

        assertEquals(1, got.size());
        assertEquals(a1, got.get(0));
    }

    @Test
    public void getBusySlots_unknownPerson_contributesNothing() {
        repository.storeBusySlots(
                Collections.singletonList(
                        new BusySlot(LocalTime.of(9, 0), LocalTime.of(10, 0), "Alice", "A")));

        List<BusySlot> got = repository.getBusySlots(Arrays.asList("Bob"));

        assertTrue(got.isEmpty());
    }

    @Test
    public void getBusySlots_multiplePersons_concatenatesInQueryOrder() {
        BusySlot a = new BusySlot(LocalTime.of(9, 0), LocalTime.of(10, 0), "Alice", "A");
        BusySlot b = new BusySlot(LocalTime.of(11, 0), LocalTime.of(12, 0), "Bob", "B");
        repository.storeBusySlots(Arrays.asList(a, b));

        List<BusySlot> bobFirst = repository.getBusySlots(Arrays.asList("Bob", "Alice"));
        assertEquals(Arrays.asList(b, a), bobFirst);

        List<BusySlot> aliceFirst = repository.getBusySlots(Arrays.asList("Alice", "Bob"));
        assertEquals(Arrays.asList(a, b), aliceFirst);
    }

    @Test
    public void storeBusySlots_appendsMultipleEventsForSamePerson() {
        BusySlot m1 = new BusySlot(LocalTime.of(8, 0), LocalTime.of(9, 0), "Alice", "Morning");
        BusySlot m2 = new BusySlot(LocalTime.of(14, 0), LocalTime.of(15, 0), "Alice", "Afternoon");
        repository.storeBusySlots(Arrays.asList(m1, m2));

        List<BusySlot> got = repository.getBusySlots(Collections.singletonList("Alice"));

        assertEquals(Arrays.asList(m1, m2), got);
    }

    @Test
    public void storeBusySlots_calledTwice_accumulatesForPerson() {
        repository.storeBusySlots(
                Collections.singletonList(
                        new BusySlot(LocalTime.of(8, 0), LocalTime.of(9, 0), "Alice", "First")));
        repository.storeBusySlots(
                Collections.singletonList(
                        new BusySlot(LocalTime.of(10, 0), LocalTime.of(11, 0), "Alice", "Second")));

        List<BusySlot> got = repository.getBusySlots(Collections.singletonList("Alice"));

        assertEquals(2, got.size());
        assertEquals("First", got.get(0).eventSubject());
        assertEquals("Second", got.get(1).eventSubject());
    }
}
