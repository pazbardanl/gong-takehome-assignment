package io.gong.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.gong.domain.AvailableSlot;
import io.gong.domain.BusySlot;
import io.gong.repository.impl.CalendarRepositoryInMemoryImpl;
import io.gong.service.impl.AvailablityCalculationServiceImpl;
import io.gong.service.impl.CalendarDataProviderCsvImpl;

/**
 * Loads the bundled README-style {@code calendar.csv}, stores all rows in the repository, then runs
 * {@link AvailablityCalculationServiceImpl} for Alice &amp; Jack and a sixty-minute slot (same scenario as README).
 */
public class AvailablityCalculationFlowIntegrationTests {

    private static final String CALENDAR_CSV_RESOURCE = "io/gong/calendar.csv";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readmeCalendarCsv_repository_andAvailability_returnsExpectedAliceAndJackSlots()
            throws IOException {
        Path csvOnDisk =
                folder.getRoot().toPath().resolve("calendar.csv");

        copyClasspathResourceTo(CALENDAR_CSV_RESOURCE, csvOnDisk);

        CalendarDataProviderCsvImpl dataProvider = new CalendarDataProviderCsvImpl();
        List<BusySlot> allRows = dataProvider.getBusySlots(csvOnDisk.toString());

        CalendarRepositoryInMemoryImpl repository = new CalendarRepositoryInMemoryImpl();
        repository.storeBusySlots(allRows);

        List<String> attendees = Arrays.asList("Alice", "Jack");
        AvailablityCalculationServiceImpl calculator =
                new AvailablityCalculationServiceImpl(repository, LocalTime.of(7, 0), LocalTime.of(19, 0));

        List<AvailableSlot> actual =
                calculator.findAvailableSlots(attendees, Duration.ofMinutes(60));

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

    private static void copyClasspathResourceTo(String classpathResource, Path destinationPath)
            throws IOException {
        try (InputStream in =
                AvailablityCalculationFlowIntegrationTests.class.getClassLoader().getResourceAsStream(classpathResource)) {
            assertNotNull("Classpath resource missing: " + classpathResource, in);
            Files.copy(in, destinationPath);
        }
    }
}
