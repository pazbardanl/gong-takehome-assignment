package io.gong.service.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import io.gong.domain.BusySlot;
import io.gong.service.CalendarDataProvider;

@Service
public class CalendarDataProviderCsvImpl implements CalendarDataProvider {

    private static final Logger LOGGER = LogManager.getLogger(CalendarDataProviderCsvImpl.class);
    private static final String DELIMITER = ",";

    @Override
    public List<BusySlot> getBusySlots(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<BusySlot> busySlots = collectBusySlotsFromReader(reader);
            LOGGER.info("Found {} valid busy slots", busySlots.size());
            return busySlots;
        } catch (IOException e) {
            LOGGER.error("Error reading CSV file: {}", filePath, e);
            throw new IllegalArgumentException("Error reading CSV file", e);
        }
    }

    private List<BusySlot> collectBusySlotsFromReader(BufferedReader reader) throws IOException {
        List<BusySlot> busySlots = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(DELIMITER);
            if (parts.length < 4) {
                LOGGER.warn("Skipping invalid CSV row: {}", line);
                continue;
            }
            try {
                BusySlot busySlot = parseBusySlot(line);
                busySlots.add(busySlot);
                LOGGER.debug("Added busy slot: {}", busySlot);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                LOGGER.warn("Skipping invalid CSV row: {}", line, e);
            }
        }
        return busySlots;
    }

    private BusySlot parseBusySlot(String line) {
        String[] parts = line.split(DELIMITER);
        String person = parts[0].trim();
        String eventSubject = parts[1].trim();
        LocalTime start = LocalTime.parse(parts[2].trim());
        LocalTime end = LocalTime.parse(parts[3].trim());
        return new BusySlot(start, end, person, eventSubject);
    }

}
