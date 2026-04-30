package io.gong.repository;

import java.util.List;

import io.gong.domain.BusySlot;

public interface CalendarRepository {
    void storeBusySlots(List<BusySlot> busySlots);
    List<BusySlot> getBusySlots(List<String> personList);
}
