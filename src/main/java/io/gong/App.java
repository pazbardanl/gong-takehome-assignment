package io.gong;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.gong.domain.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.repository.CalendarRepository;
import io.gong.repository.impl.CalendarRepositoryInMemoryImpl;
import io.gong.service.AvailabilityCalculationService;
import io.gong.service.CalendarDataProvider;
import io.gong.service.impl.AvailablityCalculationServiceImpl;
import io.gong.service.impl.CalendarDataProviderCsvImpl;

/**
 * Loads a CSV calendar, stores it in-memory, finds joint availability for named persons.
 *
 * <p>{@code Usage: calendar.csv-path durationMinutes person [person ...]}
 *
 * <p>Example: {@code mvn exec:java -Dexec.args="path/to/calendar.csv 60 Alice Jack"}
 */
public class App {

    private static final String LOGO =
            "                                    //                                                              \n"
                    + "                      *///.      .////*         ,*.                                                 \n"
                    + "                     ,///////***////////,   *//////                                                 \n"
                    + "      ./////*,,,,,. *///////////////////////////////                                                \n"
                    + "        ,///////////////////////////////////////////          ,,                                    \n"
                    + "         *//////////////////////////////////////////////////////.                                   \n"
                    + "          .////////////////////////////////////////////////////.                                    \n"
                    + "     *////////////////////////////////////////////////////////,                                     \n"
                    + " ///////////////////**#@@@@@@@@@@%///////*/%@@@@@@@@@#/*/////(@@@@@       @@@@@@       &@@@@@@@@@@@/\n"
                    + "  .///////////////*%@@@@@@@@@@@@@@%*////%@@@@@@@@@@@@@@@#*///(@@@@@@#     @@@@@@   ,@@@@@@@@@@@@@@@@\n"
                    + "     *////////////&@@@@&/*/////////////@@@@@%*////*/&@@@@%*//#@@@@@@@@/   @@@@@@  .@@@@@@.       ,,.\n"
                    + "      .//////////%@@@@&*///((((((((//*&@@@@%*//////*(@@@@@(*/%@@@@@@@@@@# @@@@@/  @@@@@@   ,########\n"
                    + "     .///////////&@@@@%*/*/&@@@@@@@(*/@@@@@#*//////*(@@@@@(*/&@@@@ (@@@@@@@@@@@.  @@@@@@   /@@@@@@@@\n"
                    + "    *////////////%@@@@@/*//***%@@@@(/*#@@@@@//////*/&@@@@%*//@@@@@  .%@@@@@@@@@.  @@@@@@*      @@@@@\n"
                    + "  .///////////////%@@@@@@%(((#@@@@&///*#@@@@@@#((%@@@@@@#*/*(@@@@@    /@@@@@@@@.  .&@@@@@@@@(%@@@@@@\n"
                    + "///////////////////*#@@@@@@@@@@@@@%/////*(&@@@@@@@@@@&(*///*#@@@@@       @@@@@@.     (@@@@@@@@@@@@@@\n"
                    + "          .////////////*********////////////********////////*****/                                  \n"
                    + "           ./////////////////////////////////////////////.                                          \n"
                    + "          ./////////////////////////////////////////////.                                           \n"
                    + "         *//////////////////////////////////////////////.                                           \n"
                    + "        ,/////////*  *////////,/////////////////////////.                                           \n"
                    + "       ///////,,      ,/////*    *////////*        ,,,*/.                                           \n"
                    + "      *///**.           */.        ,////*.                                                          \n"
                    + "    ,//*                              *,                                                            ";

    private static final DateTimeFormatter SLOT_OUTPUT = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        System.out.println(LOGO);
        System.out.println();
        System.out.println();
        try {
            if (!hasValidArity(args)) {
                printUsageAndExit();
            }
            String calendarPath = args[0].trim();
            Duration eventDuration = parseDurationMinutes(args[1]);
            List<String> attendees = collectAttendeeNames(args);
            
            CalendarRepository repository = setup(calendarPath);
            AvailabilityCalculationService calculationService = new AvailablityCalculationServiceImpl(repository);
            List<AvailableSlot> openSlots = calculationService.findAvailableSlots(attendees, eventDuration);

            printOutput(attendees, eventDuration, openSlots);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /** Loads CSV from disk into a fresh in-memory repository. */
    private static CalendarRepository setup(String calendarPath) {
        CalendarDataProvider dataProvider = new CalendarDataProviderCsvImpl();
        List<BusySlot> calendarRows = dataProvider.getBusySlots(calendarPath);
        CalendarRepository repository = new CalendarRepositoryInMemoryImpl();
        repository.storeBusySlots(calendarRows);
        return repository;
    }

    private static void printOutput(
            List<String> attendees, Duration eventDuration, List<AvailableSlot> openSlots) {
        System.out.println("Available slots for " + attendees + " with duration " + eventDuration + ":");
        for (AvailableSlot slot : openSlots) {
            System.out.println("Available slot: " + SLOT_OUTPUT.format(slot.start()));
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
