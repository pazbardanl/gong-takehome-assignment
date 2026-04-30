package io.gong.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.gong.domain.BusySlot;
import io.gong.repository.impl.CalendarRepositoryInMemoryImpl;
import io.gong.service.impl.CalendarDataProviderCsvImpl;

public class CalendarDataFlowIntegrationTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void csvProvider_loadsCsv_repositoryStoresAndQueriesByPerson() throws IOException {
        Path csv =
                Files.createTempFile(folder.getRoot().toPath(), "schedule", ".csv");
        Files.write(
                csv,
                Arrays.asList(
                        "Alice,Standup,08:30,09:15",
                        "Bob,Planning,09:30,11:00",
                        "Alice,Review,14:00,15:00"),
                StandardCharsets.UTF_8);

        CalendarDataProviderCsvImpl csvProvider = new CalendarDataProviderCsvImpl();
        CalendarRepositoryInMemoryImpl repository = new CalendarRepositoryInMemoryImpl();

        List<BusySlot> loaded = csvProvider.getBusySlots(csv.toString());
        repository.storeBusySlots(loaded);

        BusySlot alice1 = new BusySlot(LocalTime.of(8, 30), LocalTime.of(9, 15), "Alice", "Standup");
        BusySlot bob = new BusySlot(LocalTime.of(9, 30), LocalTime.of(11, 0), "Bob", "Planning");
        BusySlot alice2 = new BusySlot(LocalTime.of(14, 0), LocalTime.of(15, 0), "Alice", "Review");

        assertEquals(Arrays.asList(alice1, alice2), repository.getBusySlots(Collections.singletonList("Alice")));
        assertEquals(Collections.singletonList(bob), repository.getBusySlots(Collections.singletonList("Bob")));
        assertEquals(Arrays.asList(bob, alice1, alice2),repository.getBusySlots(Arrays.asList("Bob", "Alice")));
    }
}
