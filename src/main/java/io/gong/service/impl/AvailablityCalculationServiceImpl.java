package io.gong.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import io.gong.domain.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.domain.MultiPersonBusySlot;
import io.gong.repository.CalendarRepository;
import io.gong.service.AvailabilityCalculationService;
import org.apache.logging.log4j.LogManager;

public class AvailablityCalculationServiceImpl implements AvailabilityCalculationService {

    private static final String DEFAULT_WORKDAY_PROPERTIES_RESOURCE = "io/gong/workday.properties";

    private static final Logger LOGGER = LogManager.getLogger(AvailablityCalculationServiceImpl.class);

    private final CalendarRepository calendarRepository;
    private final LocalTime workdayStart;
    private final LocalTime workdayEnd;

    public AvailablityCalculationServiceImpl(CalendarRepository calendarRepository) {
        this(calendarRepository, DEFAULT_WORKDAY_PROPERTIES_RESOURCE);
    }
    public AvailablityCalculationServiceImpl(
            CalendarRepository calendarRepository, String workdayPropertiesClasspathResource) {
        this.calendarRepository = Objects.requireNonNull(calendarRepository, "calendarRepository");
        Properties properties = loadClasspathProperties(workdayPropertiesClasspathResource);
        this.workdayStart = parseRequiredLocalTime(properties, "workday.start");
        this.workdayEnd = parseRequiredLocalTime(properties, "workday.end");
        if (!workdayEnd.isAfter(workdayStart)) {
            throw new IllegalArgumentException("workdayEnd must be after workdayStart");
        }
    }

    private static Properties loadClasspathProperties(String classpathResource) {
        ClassLoader loader = AvailablityCalculationServiceImpl.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IllegalStateException("Classpath resource not found: " + classpathResource);
            }
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + classpathResource, e);
        }
    }

    private static LocalTime parseRequiredLocalTime(Properties properties, String key) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("Missing or empty property: " + key);
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Invalid LocalTime for property " + key + ": " + raw, e);
        }
    }

    @Override
    public List<AvailableSlot> findAvailableSlots(List<String> personList, Duration eventDuration) {
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
        return eventDurationSlots;
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
