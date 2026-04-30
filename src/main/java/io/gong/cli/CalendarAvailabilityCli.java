package io.gong.cli;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.gong.contract.AvailabilityResponse;
import io.gong.contract.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.repository.CalendarRepository;
import io.gong.service.AvailabilityCalculationService;
import io.gong.service.CalendarDataProvider;

public class CalendarAvailabilityCli {

    private static final DateTimeFormatter SLOT_OUTPUT = DateTimeFormatter.ofPattern("HH:mm");

    private final CalendarRepository calendarRepository;
    private final CalendarDataProvider calendarDataProvider;
    private final AvailabilityCalculationService calculationService;

    public CalendarAvailabilityCli(
            CalendarRepository calendarRepository,
            CalendarDataProvider calendarDataProvider,
            AvailabilityCalculationService calculationService) {
        this.calendarRepository = calendarRepository;
        this.calendarDataProvider = calendarDataProvider;
        this.calculationService = calculationService;
    }

    public void run(String[] args) {
        if (!hasValidArity(args)) {
            printUsageAndExit();
        }

        String calendarPath = args[0].trim();
        Duration eventDuration = parseDurationMinutes(args[1]);
        List<String> attendees = collectAttendeeNames(args);

        List<BusySlot> calendarRows = calendarDataProvider.getBusySlots(calendarPath);
        calendarRepository.storeBusySlots(calendarRows);

        AvailabilityResponse availabilityResponse = calculationService.calculateAvailability(attendees, eventDuration);

        List<AvailableSlot> openSlots = availabilityResponse.availableSlots();
        List<String> recommendations = availabilityResponse.recommendations();

        printOutput(attendees, eventDuration, openSlots, recommendations);
    }

    private static void printOutput(List<String> attendees, Duration eventDuration, List<AvailableSlot> openSlots, List<String> recommendations) {
        printOpenSlots(attendees, eventDuration, openSlots);
        System.out.println();
        printRecommendations(recommendations);
        System.out.println();
    }


    private static void printOpenSlots(List<String> attendees, Duration eventDuration, List<AvailableSlot> openSlots) {
        System.out.println("Available slots for " + attendees + " with duration " + eventDuration + ":");
        if (openSlots.isEmpty()) {
            System.out.println("None");
        } else {
            for (AvailableSlot slot : openSlots) {
                System.out.println("Available slot: " + SLOT_OUTPUT.format(slot.start()));
            }
        }
    }

    private static void printRecommendations(List<String> recommendations) {
        System.out.println("Recommendations:");
        if (recommendations.isEmpty()) {
            System.out.println("None");
        } else {
            for (String recommendation : recommendations) {
                System.out.println("Recommendation: " + recommendation);
            }
        }
    }

    private static boolean hasValidArity(String[] args) {
        return args != null && args.length >= 3;
    }

    private static Duration parseDurationMinutes(String raw) throws IllegalArgumentException {
        try {
            long minutes = Long.parseLong(raw.trim());
            if (minutes <= 0) {
                throw new IllegalArgumentException("durationMinutes must be positive, got: " + raw);
            }
            return Duration.ofMinutes(minutes);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration minutes: " + raw, e);
        }
    }

    private static List<String> collectAttendeeNames(String[] args) throws IllegalArgumentException {
        List<String> names = new ArrayList<>(args.length - 2);
        for (int i = 2; i < args.length; i++) {
            String name = args[i].trim();
            if (name.isEmpty()) {
                continue;
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one non-blank attendee name.");
        }
        return names;
    }

    private static void printUsageAndExit() {
        System.err.println(
                "Usage: calendar.csv-path durationMinutes person [person ...]"
                        + System.lineSeparator()
                        + "Example: mvn exec:java -Dexec.args=\"/path/to/calendar.csv 60 Alice Jack\"");
        System.exit(2);
    }
}
