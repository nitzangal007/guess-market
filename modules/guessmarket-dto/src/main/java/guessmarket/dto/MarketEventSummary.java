package guessmarket.dto;

import java.util.List;
import java.util.Objects;

public final class MarketEventSummary {
    private final int eventId;
    private final String name;
    private final String description;
    private final int commissionPercentage;
    private final CommissionMode commissionMode;
    private final List<String> optionLabels;
    private final EventStatus status;

    public MarketEventSummary(
            int eventId,
            String name,
            String description,
            int commissionPercentage,
            CommissionMode commissionMode,
            List<String> optionLabels,
            EventStatus status) {
        if (commissionPercentage < 0 || commissionPercentage > 90) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 90");
        }

        List<String> copiedLabels = List.copyOf(Objects.requireNonNull(optionLabels, "optionLabels"));
        if (copiedLabels.size() != 2) {
            throw new IllegalArgumentException("Exactly two option labels are required");
        }

        this.eventId = eventId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.commissionPercentage = commissionPercentage;
        this.commissionMode = Objects.requireNonNull(commissionMode, "commissionMode");
        this.optionLabels = copiedLabels;
        this.status = Objects.requireNonNull(status, "status");
    }

    public int getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercentage() {
        return commissionPercentage;
    }

    public CommissionMode getCommissionMode() {
        return commissionMode;
    }

    public List<String> getOptionLabels() {
        return optionLabels;
    }

    public EventStatus getStatus() {
        return status;
    }
}
