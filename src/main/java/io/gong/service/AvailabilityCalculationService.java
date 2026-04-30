package io.gong.service;

import java.time.Duration;
import java.util.List;

import io.gong.contract.AvailabilityResponse;

public interface AvailabilityCalculationService {
    AvailabilityResponse calculateAvailability( List<String> personList, Duration eventDuration);
}
