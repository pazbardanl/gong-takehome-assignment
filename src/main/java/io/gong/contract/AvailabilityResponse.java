package io.gong.contract;

import java.util.List;
import java.util.Objects;

public class AvailabilityResponse {
    private final List<AvailableSlot> availableSlots;
    private final List<String> recommendations;

    public AvailabilityResponse(List<AvailableSlot> availableSlots, List<String> recommendations) {
        this.availableSlots = availableSlots;
        this.recommendations = recommendations;
    }

    public List<AvailableSlot> availableSlots() {
        return availableSlots;
    }
    
    public List<String> recommendations() {
        return recommendations;
    }

    @Override
    public String toString() {
        return "AvailabilityResponse{" +
                "availableSlots=" + availableSlots +
                ", recommendations=" + recommendations +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvailabilityResponse that = (AvailabilityResponse) o;
        return Objects.equals(availableSlots, that.availableSlots) && Objects.equals(recommendations, that.recommendations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(availableSlots, recommendations);
    }
}