package guessmarket.dto;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class MarketEventDetails {
    private final int eventId;
    private final String name;
    private final String description;
    private final int commissionPercentage;
    private final CommissionMode commissionMode;
    private final EventStatus status;
    private final List<MarketOptionSnapshot> options;
    private final double eventAccountBalance;
    private final double totalCommissionCollected;
    private final List<TradeHistoryEntry> purchaseHistory;
    private final OptionalInt winningOptionNumber;

    public MarketEventDetails(
            int eventId,
            String name,
            String description,
            int commissionPercentage,
            CommissionMode commissionMode,
            EventStatus status,
            List<MarketOptionSnapshot> options,
            double eventAccountBalance,
            double totalCommissionCollected,
            List<TradeHistoryEntry> purchaseHistory,
            OptionalInt winningOptionNumber) {
        if (commissionPercentage < 0 || commissionPercentage > 90) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 90");
        }

        List<MarketOptionSnapshot> copiedOptions = List.copyOf(Objects.requireNonNull(options, "options"));
        if (copiedOptions.size() != 2
                || copiedOptions.get(0).getOptionNumber() != 1
                || copiedOptions.get(1).getOptionNumber() != 2) {
            throw new IllegalArgumentException("Options must contain option 1 followed by option 2");
        }

        if (!Double.isFinite(eventAccountBalance)) {
            throw new IllegalArgumentException("Event account balance must be finite");
        }
        if (!Double.isFinite(totalCommissionCollected) || totalCommissionCollected < 0.0) {
            throw new IllegalArgumentException("Total commission must be finite and non-negative");
        }

        EventStatus requiredStatus = Objects.requireNonNull(status, "status");
        OptionalInt requiredWinner = Objects.requireNonNull(winningOptionNumber, "winningOptionNumber");
        if (requiredStatus == EventStatus.OPEN && requiredWinner.isPresent()) {
            throw new IllegalArgumentException("An open event cannot have a winning option");
        }
        if (requiredStatus == EventStatus.CLOSED && requiredWinner.isEmpty()) {
            throw new IllegalArgumentException("A closed event must have a winning option");
        }
        if (requiredWinner.isPresent()
                && requiredWinner.getAsInt() != 1
                && requiredWinner.getAsInt() != 2) {
            throw new IllegalArgumentException("Winning option must be 1 or 2");
        }

        this.eventId = eventId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.commissionPercentage = commissionPercentage;
        this.commissionMode = Objects.requireNonNull(commissionMode, "commissionMode");
        this.status = requiredStatus;
        this.options = copiedOptions;
        this.eventAccountBalance = eventAccountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.purchaseHistory = List.copyOf(Objects.requireNonNull(purchaseHistory, "purchaseHistory"));
        this.winningOptionNumber = requiredWinner;
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

    public EventStatus getStatus() {
        return status;
    }

    public List<MarketOptionSnapshot> getOptions() {
        return options;
    }

    public double getEventAccountBalance() {
        return eventAccountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public List<TradeHistoryEntry> getPurchaseHistory() {
        return purchaseHistory;
    }

    public OptionalInt getWinningOptionNumber() {
        return winningOptionNumber;
    }
}
