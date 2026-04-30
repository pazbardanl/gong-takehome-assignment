package io.gong.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.gong.domain.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.repository.CalendarRepository;

public class AvailablityCalculationServiceImplTest {

    /** No bookings: every whole hour under the workday permits a full one-hour slot. */
    @Test
    public void findAvailableSlots_noBusy_yieldsSixtyMinuteSlotsForEachWholeHourSevenThroughEighteen() {
        List<String> attendees = Collections.singletonList("Pat");
        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, Collections.emptyList())
                        .findAvailableSlots(attendees, Duration.ofMinutes(60));

        List<AvailableSlot> expected = hourlyOneHourSlots(7, 18);
        assertEquals(expected, actual);
    }

    /** Busy from workday start to end: no contiguous free hour remains for a sixty-minute booking. */
    @Test
    public void findAvailableSlots_fullDayBusy_returnsEmpty_forSixtyMinuteMeetings() {
        List<String> attendees = Collections.singletonList("Pat");
        List<BusySlot> blocking =
                Collections.singletonList(
                        new BusySlot(LocalTime.of(7, 0), LocalTime.of(19, 0), "Pat", "Blocked"));

        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, blocking).findAvailableSlots(attendees, Duration.ofMinutes(60));

        assertTrue(actual.isEmpty());
    }

    /**
     * Durations other than sixty minutes slice each free fragment greedily from its start edge (policy in
     * {@link AvailablityCalculationServiceImpl}).
     */
    @Test
    public void findAvailableSlots_nonSixtyDuration_greedlyPacksFromStartOfFreeWindows() {
        List<String> attendees = Collections.singletonList("Pat");
        List<BusySlot> blocking =
                Collections.singletonList(new BusySlot(LocalTime.of(7, 0), LocalTime.of(9, 0), "Pat", "Busy"));

        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, blocking)
                        .findAvailableSlots(attendees, Duration.ofMinutes(120));

        assertEquals(
                Arrays.asList(
                        new AvailableSlot(LocalTime.of(9, 0), LocalTime.of(11, 0)),
                        new AvailableSlot(LocalTime.of(11, 0), LocalTime.of(13, 0)),
                        new AvailableSlot(LocalTime.of(13, 0), LocalTime.of(15, 0)),
                        new AvailableSlot(LocalTime.of(15, 0), LocalTime.of(17, 0)),
                        new AvailableSlot(LocalTime.of(17, 0), LocalTime.of(19, 0))),
                actual);
    }

    /** Busy intervals that overlap when sorted are merged before gaps are inferred. */
    @Test
    public void findAvailableSlots_overlappingBusySlotsMerged_beforeHourlyCuts() {
        List<String> attendees = Collections.singletonList("Pat");
        List<BusySlot> overlaps =
                Arrays.asList(
                        new BusySlot(LocalTime.of(10, 0), LocalTime.of(12, 0), "Pat", "A"),
                        new BusySlot(LocalTime.of(11, 0), LocalTime.of(13, 0), "Pat", "B"));

        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, overlaps)
                        .findAvailableSlots(attendees, Duration.ofMinutes(60));

        assertEquals(hourlyOneHourExceptBlockedRange(10, 13), actual);
    }

    /** Combined busy timelines for multiple attendees behave like union of their intervals when merged. */
    @Test
    public void findAvailableSlots_multipleAttendeesOverlappingMeeting_unionCutsSameHour_asSingleBlock() {
        List<String> attendees = Arrays.asList("Ada", "Ben");
        List<BusySlot> bothBusyTogether =
                Arrays.asList(
                        new BusySlot(LocalTime.of(10, 0), LocalTime.of(11, 0), "Ada", "Sync"),
                        new BusySlot(LocalTime.of(10, 0), LocalTime.of(11, 0), "Ben", "Sync"));

        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, bothBusyTogether)
                        .findAvailableSlots(attendees, Duration.ofMinutes(60));

        assertEquals(hourlyOneHourExceptBlockedRange(10, 11), actual);
    }

    @Test
    public void readmeSample_aliceAndJack_oneHourMeeting_matchesExpectedAvailability() {
        List<BusySlot> readmeSlots =
                Arrays.asList(
                        new BusySlot(LocalTime.of(8, 0), LocalTime.of(9, 30), "Alice", "Morning meeting"),
                        new BusySlot(LocalTime.of(13, 0), LocalTime.of(14, 0), "Alice", "Lunch with Jack"),
                        new BusySlot(LocalTime.of(16, 0), LocalTime.of(17, 0), "Alice", "Yoga"),
                        new BusySlot(LocalTime.of(8, 0), LocalTime.of(8, 50), "Jack", "Morning meeting"),
                        new BusySlot(LocalTime.of(9, 0), LocalTime.of(9, 40), "Jack", "Sales call"),
                        new BusySlot(LocalTime.of(13, 0), LocalTime.of(14, 0), "Jack", "Lunch with Alice"),
                        new BusySlot(LocalTime.of(16, 0), LocalTime.of(17, 0), "Jack", "Yoga"));

        List<String> attendees = Arrays.asList("Alice", "Jack");

        List<AvailableSlot> actual =
                availabilityServiceFor(attendees, readmeSlots).findAvailableSlots(attendees, Duration.ofMinutes(60));

        List<AvailableSlot> expected =
                Arrays.asList(
                        new AvailableSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                        new AvailableSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        new AvailableSlot(LocalTime.of(11, 0), LocalTime.of(12, 0)),
                        new AvailableSlot(LocalTime.of(12, 0), LocalTime.of(13, 0)),
                        new AvailableSlot(LocalTime.of(14, 0), LocalTime.of(15, 0)),
                        new AvailableSlot(LocalTime.of(15, 0), LocalTime.of(16, 0)),
                        new AvailableSlot(LocalTime.of(17, 0), LocalTime.of(18, 0)),
                        new AvailableSlot(LocalTime.of(18, 0), LocalTime.of(19, 0)));

        assertEquals(expected, actual);
    }

    /** One-hour slots at each clock hour whose full [hour, hour+1) lies in [07:00, 19:00] workday gaps. */
    private static List<AvailableSlot> hourlyOneHourSlots(int firstHourInclusive, int lastHourInclusive) {
        List<AvailableSlot> slots = new ArrayList<>();
        for (int h = firstHourInclusive; h <= lastHourInclusive; h++) {
            slots.add(new AvailableSlot(LocalTime.of(h, 0), LocalTime.of(h + 1, 0)));
        }
        return slots;
    }

    /** Union of contiguous busy intervals [busyStartHour, busyEndHour) excludes those hour starts only. */
    private static List<AvailableSlot> hourlyOneHourExceptBlockedRange(
            int busyStartHourInclusive, int busyEndHourExclusive) {
        List<AvailableSlot> slots = new ArrayList<>();
        for (int h = 7; h <= 18; h++) {
            if (h >= busyStartHourInclusive && h < busyEndHourExclusive) {
                continue;
            }
            slots.add(new AvailableSlot(LocalTime.of(h, 0), LocalTime.of(h + 1, 0)));
        }
        return slots;
    }

    private static AvailablityCalculationServiceImpl availabilityServiceFor(
            List<String> attendees, List<BusySlot> busyReturnedByRepository) {
        CalendarRepository repository = mock(CalendarRepository.class);
        when(repository.getBusySlots(eq(attendees))).thenReturn(busyReturnedByRepository);
        return new AvailablityCalculationServiceImpl(repository);
    }
}
