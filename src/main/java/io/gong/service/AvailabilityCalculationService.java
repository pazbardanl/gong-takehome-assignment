package io.gong.service;

import java.time.Duration;
import java.util.List;

import io.gong.domain.AvailableSlot;

public interface AvailabilityCalculationService {
    List<AvailableSlot> findAvailableSlots( List<String> personList, Duration eventDuration);
}
