package io.gong.service.impl;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.gong.contract.AvailabilityResponse;
import io.gong.contract.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.domain.MultiPersonBusySlot;
import io.gong.repository.CalendarRepository;
import io.gong.service.AvailabilityCalculationService;
import io.gong.service.AvailabilityRecommendationsService;

@Service
public class AvailablityCalculationServiceImpl implements AvailabilityCalculationService {

    private static final Logger LOGGER = LogManager.getLogger(AvailablityCalculationServiceImpl.class);

    private final CalendarRepository calendarRepository;
    private final AvailabilityRecommendationsService availabilityRecommendationsService;
    private final LocalTime workdayStart;
    private final LocalTime workdayEnd;

    @Autowired
    public AvailablityCalculationServiceImpl(
            CalendarRepository calendarRepository,
            AvailabilityRecommendationsService availabilityRecommendationsService,
            @Value("${workday.start}") String workdayStart,
            @Value("${workday.end}") String workdayEnd) {
        this(
                calendarRepository,
                availabilityRecommendationsService,
                LocalTime.parse(workdayStart.trim()),
                LocalTime.parse(workdayEnd.trim()));
    }

    public AvailablityCalculationServiceImpl(
            CalendarRepository calendarRepository, LocalTime workdayStart, LocalTime workdayEnd) {
        this(calendarRepository, new AvailabilityRecommendationsServiceImpl(), workdayStart, workdayEnd);
    }

    private AvailablityCalculationServiceImpl(
            CalendarRepository calendarRepository,
            AvailabilityRecommendationsService availabilityRecommendationsService,
            LocalTime workdayStart,
            LocalTime workdayEnd) {
        this.calendarRepository = Objects.requireNonNull(calendarRepository, "calendarRepository");
        this.availabilityRecommendationsService =
                Objects.requireNonNull(availabilityRecommendationsService, "availabilityRecommendationsService");
        this.workdayStart = Objects.requireNonNull(workdayStart, "workdayStart");
        this.workdayEnd = Objects.requireNonNull(workdayEnd, "workdayEnd");
        if (!workdayEnd.isAfter(workdayStart)) {
            throw new IllegalArgumentException("workdayEnd must be after workdayStart");
        }
    }

    @Override
    public AvailabilityResponse calculateAvailability(List<String> personList, Duration eventDuration) {
        validateAvailabilityRequest(personList, eventDuration);
        List<BusySlot> busySlots = calendarRepository.getBusySlots(personList);
        LOGGER.debug("Busy slots: {}", busySlots);
        List<BusySlot> busySlotsSorted = sortBusySlots(busySlots);
        LOGGER.debug("Sorted busy slots: {}", busySlotsSorted);
        List<MultiPersonBusySlot> mergedBusySlots = mergeOverlappingBusySlots(busySlotsSorted);
        LOGGER.debug("Merged busy slots: {}", mergedBusySlots);
        List<AvailableSlot> mergedAvailableSlots = createMergedAvailableSlots(mergedBusySlots);
        LOGGER.debug("Merged available slots: {}", mergedAvailableSlots);
        List<AvailableSlot> eventDurationSlots = splitAvailableSlotsIntoEventDurationSlots(mergedAvailableSlots, eventDuration);
        LOGGER.debug("Event duration slots: {}", eventDurationSlots);
        List<String> recommendations = availabilityRecommendationsService.provideRecommendations(personList, mergedBusySlots);
        return new AvailabilityResponse(eventDurationSlots, recommendations);
    }

    private static void validateAvailabilityRequest(List<String> personList, Duration eventDuration) {
        Objects.requireNonNull(personList, "personList");
        Objects.requireNonNull(eventDuration, "eventDuration");
        if (personList.isEmpty()) {
            throw new IllegalArgumentException("personList must not be empty");
        }
        if (personList.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("personList must not contain null entries");
        }
        if (eventDuration.isZero() || eventDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "eventDuration must be positive, got seconds=" + eventDuration.getSeconds()
                            + ", nanos=" + eventDuration.getNano());
        }
    }

    private List<BusySlot> sortBusySlots(List<BusySlot> busySlots) {
        return busySlots.stream()
                .sorted(Comparator.comparing(BusySlot::start).thenComparing(BusySlot::person))
                .collect(Collectors.toList());
    }

    private List<MultiPersonBusySlot> mergeOverlappingBusySlots(List<BusySlot> busySlotsSorted) {
        if (busySlotsSorted.isEmpty()) {
            return new ArrayList<>();
        }
        List<MultiPersonBusySlot> mergedBusySlots = new ArrayList<>();
        BusySlot firstBusySlot = busySlotsSorted.get(0);
        mergedBusySlots.add(new MultiPersonBusySlot(Set.of(firstBusySlot.person()), firstBusySlot.start(), firstBusySlot.end()));
        for (int i = 1; i < busySlotsSorted.size(); i++) {
            BusySlot current = busySlotsSorted.get(i);
            MultiPersonBusySlot lastMergedBusySlot =
                    mergedBusySlots.get(mergedBusySlots.size() - 1);
            if (current.start().isBefore(lastMergedBusySlot.end())) {
                Set<String> updatedPersons = new HashSet<>(lastMergedBusySlot.persons());
                updatedPersons.add(current.person());
                mergedBusySlots.set(
                        mergedBusySlots.size() - 1,
                        new MultiPersonBusySlot(
                                updatedPersons,
                                lastMergedBusySlot.start(),
                                lastMergedBusySlot.end().isAfter(current.end())
                                        ? lastMergedBusySlot.end()
                                        : current.end()));
            } else {
                mergedBusySlots.add(new MultiPersonBusySlot(Set.of(current.person()), current.start(), current.end()));
            }
        }
        return mergedBusySlots;
    }

    private List<AvailableSlot> createMergedAvailableSlots(List<MultiPersonBusySlot> mergedBusySlots) {
        List<AvailableSlot> mergedAvailableSlots = new ArrayList<>();
        LocalTime startTime = workdayStart;
        for (MultiPersonBusySlot mergedBusySlot : mergedBusySlots) {
            if (startTime.isBefore(mergedBusySlot.start())) {
                mergedAvailableSlots.add(new AvailableSlot(startTime, mergedBusySlot.start()));
            }
            startTime = mergedBusySlot.end();
        }
        if (startTime.isBefore(workdayEnd)) {
            mergedAvailableSlots.add(new AvailableSlot(startTime, workdayEnd));
        }
        return mergedAvailableSlots;
    }

    /**
     * For one-hour meetings, tries each whole-hour start from {@link #workdayStart} upward while the slot still
     * ends on or before {@link #workdayEnd}. Non–whole-hour bounds use the first aligned hour inside the interval.
     */
    private List<AvailableSlot> splitAvailableSlotsIntoEventDurationSlots(
            List<AvailableSlot> mergedAvailableSlots, Duration eventDuration) {
        if (eventDuration.equals(Duration.ofMinutes(60))) {
            return splitIntoHourlyOneHourSlots(mergedAvailableSlots, eventDuration);
        }
        List<AvailableSlot> eventDurationSlots = new ArrayList<>();
        for (AvailableSlot mergedAvailableSlot : mergedAvailableSlots) {
            AvailableSlot window = mergedAvailableSlot;
            while (window.duration().compareTo(eventDuration) >= 0) {
                eventDurationSlots.add(new AvailableSlot(window.start(), window.start().plus(eventDuration)));
                window = new AvailableSlot(window.start().plus(eventDuration), window.end());
            }
        }
        return eventDurationSlots;
    }

    private List<AvailableSlot> splitIntoHourlyOneHourSlots(
            List<AvailableSlot> mergedAvailableSlots, Duration eventDuration) {
        List<AvailableSlot> eventDurationSlots = new ArrayList<>();
        for (LocalTime candidate = alignedWholeHourOnOrAfter(workdayStart);
                !candidate.plus(eventDuration).isAfter(workdayEnd);
                candidate = candidate.plusHours(1)) {
            LocalTime end = candidate.plus(eventDuration);
            for (AvailableSlot window : mergedAvailableSlots) {
                if (!candidate.isBefore(window.start()) && !end.isAfter(window.end())) {
                    eventDurationSlots.add(new AvailableSlot(candidate, end));
                    break;
                }
            }
        }
        return eventDurationSlots;
    }

    private static LocalTime alignedWholeHourOnOrAfter(LocalTime t) {
        if (t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0) {
            return t;
        }
        return t.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

}
