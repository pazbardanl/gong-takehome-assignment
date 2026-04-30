package io.gong.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import io.gong.domain.MultiPersonBusySlot;
import io.gong.service.AvailabilityRecommendationsService;

@Service
public class AvailabilityRecommendationsServiceImpl implements AvailabilityRecommendationsService {

    @Override
    public List<String> provideRecommendations(List<String> personList, List<MultiPersonBusySlot> multiPersonBusySlots) {
        List<String> recommendations = new ArrayList<>();
        recommendations.addAll(provideRecommendationsForSinglePersonSlots(personList, multiPersonBusySlots));
        return recommendations;
    }

    private List<String> provideRecommendationsForSinglePersonSlots(List<String> personList, List<MultiPersonBusySlot> multiPersonBusySlots) {
        List<String> recommendations = new ArrayList<>();
        if (personList.isEmpty() || personList.size() < 3 || multiPersonBusySlots.isEmpty()) {
            return Collections.emptyList();
        }
        multiPersonBusySlots.stream().forEach(multiPersonBusySlot -> {
            if (multiPersonBusySlot.persons().size() == 1) {
                recommendations.add("Open slot at " + multiPersonBusySlot.start() + " without " + multiPersonBusySlot.persons().iterator().next());
            }
        });
        return recommendations;
    }
}
