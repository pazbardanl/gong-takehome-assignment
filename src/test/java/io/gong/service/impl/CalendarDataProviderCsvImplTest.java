package io.gong.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.gong.domain.BusySlot;

public class CalendarDataProviderCsvImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CalendarDataProviderCsvImpl provider = new CalendarDataProviderCsvImpl();

    @Test
    public void getBusySlots_readsValidRows() throws IOException {
        Path csv = writeCsv(
                "Alice,Morning meeting,08:00,09:30",
                "Jack,Sales call,10:00,11:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Morning meeting", slots.get(0).eventSubject());
        assertEquals(LocalTime.of(8, 0), slots.get(0).start());
        assertEquals(LocalTime.of(9, 30), slots.get(0).end());
        assertEquals("Jack", slots.get(1).person());
        assertEquals(LocalTime.of(10, 0), slots.get(1).start());
    }

    @Test
    public void getBusySlots_skipsRowWithTooFewColumns() throws IOException {
        Path csv = writeCsv(
                "Alice,Ok,09:00,10:00",
                "only,three,cols",
                "Bob,Also ok,12:00,13:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Bob", slots.get(1).person());
    }

    @Test
    public void getBusySlots_skipsRowWithInvalidTimes() throws IOException {
        Path csv = writeCsv(
                "Alice,Ok,09:00,10:00",
                "Bob,Bad start,25:00,11:00",
                "Carol,End before start,14:00,13:00",
                "Dan,Ok,15:00,16:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Dan", slots.get(1).person());
    }

    @Test
    public void getBusySlots_missingFileThrowsIllegalArgumentExceptionWithIOExceptionCause() {
        Path missing = folder.getRoot().toPath().resolve("does-not-exist.csv");

        try {
            provider.getBusySlots(missing.toString());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void getBusySlots_nullPathThrowsNullPointerException() {
        try {
            provider.getBusySlots(null);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // FileReader rejects null pathname
        }
    }

    @Test
    public void getBusySlots_pathIsDirectoryThrowsIllegalArgumentExceptionWithIOExceptionCause()
            throws IOException {
        File dir = folder.newFolder("csv-is-dir-not-file");

        try {
            provider.getBusySlots(dir.getAbsolutePath());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void getBusySlots_skipsBlankAndWhitespaceOnlyLines() throws IOException {
        Path csv = writeCsv(
                "Alice,Ok,09:00,10:00",
                "",
                "   \t  ",
                "Bob,Also ok,12:00,13:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Bob", slots.get(1).person());
    }

    @Test
    public void getBusySlots_emptyFileReturnsEmptyList() throws IOException {
        Path csv = writeCsv();

        assertTrue(provider.getBusySlots(csv.toString()).isEmpty());
    }

    @Test
    public void getBusySlots_allInvalidRowsReturnsEmptyList() throws IOException {
        Path csv = writeCsv(
                "too,few",
                "x,y,not-time,still-bad",
                "A,B,09:00,08:00",
                "Sam,Same time,14:00,14:00");

        assertTrue(provider.getBusySlots(csv.toString()).isEmpty());
    }

    @Test
    public void getBusySlots_skipsRowWhereStartEqualsEnd() throws IOException {
        Path csv = writeCsv(
                "Alice,Ok,09:00,10:00",
                "Eve,Equal bounds,14:00,14:00",
                "Dan,Ok,15:00,16:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Dan", slots.get(1).person());
    }

    /**
     * Naive comma-splitting cannot parse RFC-style quoted fields; the row is discarded when time
     * columns shift and {@link LocalTime#parse} fails.
     */
    @Test
    public void getBusySlots_subjectWithQuotedCommaSplitsWrong_rowSkipped_followedRowsKept()
            throws IOException {
        Path csv = writeCsv(
                "Alice,Ok,09:00,10:00",
                "Bob,\"Morning, catered\",11:00,12:00",
                "Carl,Closing,13:00,14:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(2, slots.size());
        assertEquals("Alice", slots.get(0).person());
        assertEquals("Carl", slots.get(1).person());
    }

    /** Extra commas produce too many segments; naive parse fails on malformed time tokens. */
    @Test
    public void getBusySlots_extraCommasMisalignColumns_badRowSkipped() throws IOException {
        Path csv =
                writeCsv("Alice,A,B,C,D,09:00,10:00", "Bob,Ok,11:00,12:00");

        List<BusySlot> slots = provider.getBusySlots(csv.toString());

        assertEquals(1, slots.size());
        assertEquals("Bob", slots.get(0).person());
    }

    private Path writeCsv(String... lines) throws IOException {
        Path path = Files.createTempFile(folder.getRoot().toPath(), "cal", ".csv");
        Files.write(path, List.of(lines), StandardCharsets.UTF_8);
        return path;
    }
}
