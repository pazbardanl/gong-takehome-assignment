package io.gong.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import io.gong.domain.BusySlot;
import io.gong.repository.CalendarRepository;

@Repository
public class CalendarRepositoryInMemoryImpl implements CalendarRepository {

    private final Map<String, List<BusySlot>> personToBusySlots = new HashMap<>();

    @Override
    public void storeBusySlots(List<BusySlot> busySlots) {
        for (BusySlot busySlot : busySlots) {
            personToBusySlots.computeIfAbsent(busySlot.person(), k -> new ArrayList<>()).add(busySlot);
        }
    }

    @Override
    public List<BusySlot> getBusySlots(List<String> personList) {
        List<BusySlot> busySlots = new ArrayList<>();
        for (String person : personList) {
            busySlots.addAll(personToBusySlots.getOrDefault(person, new ArrayList<>()));
        }
        return busySlots;
    }

}
