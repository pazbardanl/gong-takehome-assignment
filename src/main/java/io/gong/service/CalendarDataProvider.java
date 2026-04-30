package io.gong.service;

import io.gong.domain.BusySlot;
import java.util.List;

public interface CalendarDataProvider {
    List<BusySlot> getBusySlots(String filePath);
}
