package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.MarketOptionSnapshot;
import guessmarket.dto.TradeHistoryEntry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class MarketEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int eventId;
    private final String name;
    private final String description;
    private final CommissionPolicy commissionPolicy;
    private final int b;
    private final ArrayList<MarketOption> options;
    private final EventAccount account;
    private final ArrayList<TradeRecord> purchaseHistory;
    private EventStatus status;
    private Integer winningOptionNumber;

    public MarketEvent(
            int eventId,
            String name,
            String description,
            CommissionMode commissionMode,
            int commissionPercentage,
            int b,
            String firstOptionLabel,
            String secondOptionLabel) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be positive");
        }

        this.eventId = eventId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        commissionPolicy = new CommissionPolicy(commissionMode, commissionPercentage);
        this.b = b;
        options = new ArrayList<>(2);
        options.add(new MarketOption(1, firstOptionLabel));
        options.add(new MarketOption(2, secondOptionLabel));
        account = new EventAccount();
        purchaseHistory = new ArrayList<>();
        status = EventStatus.OPEN;
        winningOptionNumber = null;

        LmsrCalculator.maximumSubsidy(b);
    }

    TradeRecord purchase(int optionNumber, int quantity)
            throws EngineOperationException {
        requireOpen(optionNumber, quantity);
        MarketOption selectedOption = requireOption(optionNumber, quantity);
        requirePositiveQuantity(quantity, optionNumber);

        int newSelectedQuantity;
        try {
            newSelectedQuantity = Math.addExact(selectedOption.getQuantity(), quantity);
        } catch (ArithmeticException exception) {
            throw quantityFailure(
                    optionNumber,
                    quantity,
                    "The purchase would overflow the option quantity",
                    "Choose a smaller quantity.",
                    exception);
        }

        MarketOption otherOption = options.get(1 - (optionNumber - 1));
        double baseCost;
        double purchaseCommission;
        double totalPaid;
        double newBalance;
        double newTotalCommission;
        TradeRecord newRecord;

        try {
            baseCost = LmsrCalculator.purchaseCost(
                    selectedOption.getQuantity(),
                    otherOption.getQuantity(),
                    quantity,
                    b);
            purchaseCommission = commissionPolicy.purchaseCommission(baseCost);
            requireCommissionRepresentable(purchaseCommission);
            totalPaid = requireFinitePositive(
                    baseCost + purchaseCommission,
                    "total paid");
            newBalance = requireFinite(
                    account.getBalance() + totalPaid,
                    "event account balance");
            newTotalCommission = requireFiniteNonnegative(
                    account.getTotalCommissionCollected() + purchaseCommission,
                    "total commission collected");

            int firstQuantity = optionNumber == 1
                    ? newSelectedQuantity
                    : options.get(0).getQuantity();
            int secondQuantity = optionNumber == 2
                    ? newSelectedQuantity
                    : options.get(1).getQuantity();
            requireProbability(
                    LmsrCalculator.priceForOption(firstQuantity, secondQuantity, b),
                    "first option price");
            requireProbability(
                    LmsrCalculator.priceForOption(secondQuantity, firstQuantity, b),
                    "second option price");

            newRecord = new TradeRecord(
                    optionNumber,
                    selectedOption.getLabel(),
                    quantity,
                    baseCost,
                    purchaseCommission,
                    totalPaid);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            throw calculationFailure(optionNumber, quantity, exception);
        }

        selectedOption.commitPurchase(newSelectedQuantity);
        account.commitPurchase(newBalance, newTotalCommission);
        purchaseHistory.add(newRecord);
        return newRecord;
    }

    void close(int winningOptionNumber) throws EngineOperationException {
        requireOpen(winningOptionNumber, null);
        MarketOption winningOption = requireOption(winningOptionNumber, null);

        double closingCommission;
        double newBalance;
        double newTotalCommission;

        try {
            double grossPayout = winningOption.getQuantity();
            closingCommission = requireFiniteNonnegative(
                    commissionPolicy.closingCommission(grossPayout),
                    "closing commission");
            double netPayout = requireFiniteNonnegative(
                    grossPayout - closingCommission,
                    "net payout");
            newBalance = requireFinite(
                    account.getBalance() - netPayout,
                    "event account balance");
            newTotalCommission = requireFiniteNonnegative(
                    account.getTotalCommissionCollected() + closingCommission,
                    "total commission collected");
        } catch (ArithmeticException | IllegalArgumentException exception) {
            throw calculationFailure(winningOptionNumber, null, exception);
        }

        account.commitClose(newBalance, newTotalCommission);
        this.winningOptionNumber = winningOptionNumber;
        status = EventStatus.CLOSED;
    }

    MarketEventSummary toSummary() {
        return new MarketEventSummary(
                eventId,
                name,
                description,
                commissionPolicy.getPercentage(),
                commissionPolicy.getMode(),
                List.of(options.get(0).getLabel(), options.get(1).getLabel()),
                status);
    }

    MarketEventDetails toDetails() {
        int firstQuantity = options.get(0).getQuantity();
        int secondQuantity = options.get(1).getQuantity();
        List<MarketOptionSnapshot> optionSnapshots = List.of(
                new MarketOptionSnapshot(
                        1,
                        options.get(0).getLabel(),
                        firstQuantity,
                        LmsrCalculator.priceForOption(firstQuantity, secondQuantity, b)),
                new MarketOptionSnapshot(
                        2,
                        options.get(1).getLabel(),
                        secondQuantity,
                        LmsrCalculator.priceForOption(secondQuantity, firstQuantity, b)));

        List<TradeHistoryEntry> newestFirstHistory = new ArrayList<>(purchaseHistory.size());
        for (int index = purchaseHistory.size() - 1; index >= 0; index--) {
            newestFirstHistory.add(purchaseHistory.get(index).toHistoryEntry());
        }

        OptionalInt winner = winningOptionNumber == null
                ? OptionalInt.empty()
                : OptionalInt.of(winningOptionNumber);
        return new MarketEventDetails(
                eventId,
                name,
                description,
                commissionPolicy.getPercentage(),
                commissionPolicy.getMode(),
                status,
                optionSnapshots,
                account.getBalance(),
                account.getTotalCommissionCollected(),
                newestFirstHistory,
                winner);
    }

    double maximumSubsidy() {
        return LmsrCalculator.maximumSubsidy(b);
    }

    int getEventId() {
        return eventId;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    CommissionPolicy getCommissionPolicy() {
        return commissionPolicy;
    }

    int getLiquidityParameter() {
        return b;
    }

    List<MarketOption> getOptions() {
        return options;
    }

    EventAccount getAccount() {
        return account;
    }

    List<TradeRecord> getPurchaseHistory() {
        return purchaseHistory;
    }

    EventStatus getStatus() {
        return status;
    }

    OptionalInt getWinningOptionNumber() {
        return winningOptionNumber == null
                ? OptionalInt.empty()
                : OptionalInt.of(winningOptionNumber);
    }

    private void requireOpen(Integer optionNumber, Integer quantity)
            throws EngineOperationException {
        if (status != EventStatus.OPEN) {
            throw operationFailure(
                    EngineErrorCode.EVENT_NOT_OPEN,
                    "Event " + eventId + " is not open",
                    "Choose an open event.",
                    optionNumber,
                    quantity,
                    null);
        }
    }

    private MarketOption requireOption(Integer optionNumber, Integer quantity)
            throws EngineOperationException {
        if (optionNumber == null || (optionNumber != 1 && optionNumber != 2)) {
            throw operationFailure(
                    EngineErrorCode.INVALID_OPTION,
                    "Option number must be 1 or 2",
                    "Choose option 1 or 2.",
                    optionNumber,
                    quantity,
                    null);
        }
        return options.get(optionNumber - 1);
    }

    private void requirePositiveQuantity(int quantity, int optionNumber)
            throws EngineOperationException {
        if (quantity <= 0) {
            throw quantityFailure(
                    optionNumber,
                    quantity,
                    "Purchase quantity must be positive",
                    "Choose a positive quantity.",
                    null);
        }
    }

    private void requireCommissionRepresentable(double commission) {
        requireFiniteNonnegative(commission, "purchase commission");
        if (commissionPolicy.getMode() == CommissionMode.ON_PURCHASE
                && commissionPolicy.getPercentage() > 0
                && commission <= 0.0) {
            throw new ArithmeticException("purchase commission must be positive");
        }
    }

    private static double requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " must be finite");
        }
        return value;
    }

    private static double requireFinitePositive(double value, String name) {
        requireFinite(value, name);
        if (value <= 0.0) {
            throw new ArithmeticException(name + " must be positive");
        }
        return value;
    }

    private static double requireFiniteNonnegative(double value, String name) {
        requireFinite(value, name);
        if (value < 0.0) {
            throw new ArithmeticException(name + " must be nonnegative");
        }
        return value;
    }

    private static double requireProbability(double value, String name) {
        requireFinite(value, name);
        if (value < 0.0 || value > 1.0) {
            throw new ArithmeticException(name + " must be between zero and one");
        }
        return value;
    }

    private EngineOperationException quantityFailure(
            int optionNumber,
            int quantity,
            String detail,
            String recoveryHint,
            Throwable cause) {
        return operationFailure(
                EngineErrorCode.INVALID_QUANTITY,
                detail,
                recoveryHint,
                optionNumber,
                quantity,
                cause);
    }

    private EngineOperationException calculationFailure(
            int optionNumber,
            Integer quantity,
            Throwable cause) {
        return operationFailure(
                EngineErrorCode.FINANCIAL_CALCULATION_FAILED,
                "The market calculation could not produce a valid finite result",
                "Choose a different quantity or option.",
                optionNumber,
                quantity,
                cause);
    }

    private EngineOperationException operationFailure(
            EngineErrorCode code,
            String detail,
            String recoveryHint,
            Integer optionNumber,
            Integer quantity,
            Throwable cause) {
        return new EngineOperationException(
                code,
                detail,
                recoveryHint,
                null,
                null,
                eventId,
                null,
                optionNumber,
                quantity,
                status,
                null,
                null,
                cause);
    }
}
