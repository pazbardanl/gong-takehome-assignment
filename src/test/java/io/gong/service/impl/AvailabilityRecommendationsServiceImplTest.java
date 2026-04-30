package io.gong.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.gong.domain.MultiPersonBusySlot;

public class AvailabilityRecommendationsServiceImplTest {

    private final AvailabilityRecommendationsServiceImpl service = new AvailabilityRecommendationsServiceImpl();

    @Test
    public void provideRecommendations_emptyAttendeeList_returnsEmpty() {
        List<String> people = Collections.emptyList();
        List<MultiPersonBusySlot> busy =
                Collections.singletonList(
                        new MultiPersonBusySlot(Set.of("A"), LocalTime.of(9, 0), LocalTime.of(10, 0)));

        assertTrue(service.provideRecommendations(people, busy).isEmpty());
    }

    @Test
    public void provideRecommendations_fewerThanThreePeople_returnsEmpty() {
        List<String> people = Arrays.asList("Alice", "Jack");
        List<MultiPersonBusySlot> busy =
                Collections.singletonList(
                        new MultiPersonBusySlot(Set.of("Ada"), LocalTime.of(14, 0), LocalTime.of(15, 0)));

        assertTrue(service.provideRecommendations(people, busy).isEmpty());
    }

    @Test
    public void provideRecommendations_threePeopleButNoBusySlots_returnsEmpty() {
        List<String> people = Arrays.asList("Ada", "Ben", "Cara");

        assertTrue(service.provideRecommendations(people, Collections.emptyList()).isEmpty());
    }

    @Test
    public void provideRecommendations_onlyJointBusySlots_returnsEmpty() {
        List<String> people = Arrays.asList("Ada", "Ben", "Cara");
        List<MultiPersonBusySlot> busy =
                Arrays.asList(
                        new MultiPersonBusySlot(Set.of("Ada", "Ben"), LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        new MultiPersonBusySlot(Set.of("Ada", "Ben", "Cara"), LocalTime.of(13, 0), LocalTime.of(14, 0)));

        assertTrue(service.provideRecommendations(people, busy).isEmpty());
    }

    @Test
    public void provideRecommendations_singlePersonSlot_yieldsRecommendationForThatGap() {
        List<String> people = Arrays.asList("Ada", "Ben", "Cara");
        List<MultiPersonBusySlot> busy =
                Collections.singletonList(
                        new MultiPersonBusySlot(Set.of("Ada"), LocalTime.of(14, 0), LocalTime.of(15, 0)));

        assertEquals(
                Collections.singletonList("Availability at 14:00 to 15:00 without Ada"),
                service.provideRecommendations(people, busy));
    }

    @Test
    public void provideRecommendations_mixOfSingleAndShared_onlySinglesProduceStrings() {
        List<String> people = Arrays.asList("Ada", "Ben", "Cara");
        List<MultiPersonBusySlot> busy =
                Arrays.asList(
                        new MultiPersonBusySlot(Set.of("Ben"), LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new MultiPersonBusySlot(Set.of("Ada", "Ben"), LocalTime.of(12, 0), LocalTime.of(13, 0)));

        List<String> actual = service.provideRecommendations(people, busy);

        assertEquals(Collections.singletonList("Availability at 09:00 to 10:00 without Ben"), actual);
    }

    @Test
    public void provideRecommendations_multipleSinglePersonSlots_allListedInOrder() {
        List<String> people = Arrays.asList("Ada", "Ben", "Cara");
        List<MultiPersonBusySlot> busy =
                Arrays.asList(
                        new MultiPersonBusySlot(Set.of("Ada"), LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        new MultiPersonBusySlot(Set.of("Cara"), LocalTime.of(16, 0), LocalTime.of(17, 0)));

        assertEquals(
                Arrays.asList(
                        "Availability at 08:00 to 09:00 without Ada",
                        "Availability at 16:00 to 17:00 without Cara"),
                service.provideRecommendations(people, busy));
    }
}
