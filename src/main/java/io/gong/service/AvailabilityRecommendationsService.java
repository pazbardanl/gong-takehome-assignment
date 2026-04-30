package io.gong.service;

import java.util.List;

import io.gong.domain.MultiPersonBusySlot;

public interface AvailabilityRecommendationsService {
    List<String> provideRecommendations(List<String> personList, List<MultiPersonBusySlot> multiPersonBusySlots);
}
