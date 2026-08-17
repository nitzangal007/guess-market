package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalInt;

final class SavedStateValidator {
    private static final int FINANCIAL_EQUALITY_ULPS = 8;

    LinkedHashMap<Integer, MarketEvent> validate(SavedState candidate)
            throws EngineOperationException {
        if (candidate == null || candidate.getClass() != SavedState.class) {
            throw invalid("The saved state root is invalid.");
        }
        if (candidate.getFormatVersion() != SavedState.CURRENT_FORMAT_VERSION) {
            throw invalid("The saved state format version is unsupported.");
        }

        ArrayList<MarketEvent> events = candidate.getEvents();
        if (events == null || events.getClass() != ArrayList.class || events.isEmpty()) {
            throw invalid("The saved state must contain an ordered nonempty event list.");
        }

        IdentityHashMap<Object, Boolean> mutableNodes = new IdentityHashMap<>();
        requireDistinct(mutableNodes, events);
        LinkedHashMap<Integer, MarketEvent> validated = new LinkedHashMap<>();
        for (MarketEvent event : events) {
            validateEvent(event, mutableNodes, validated);
        }
        return validated;
    }

    private void validateEvent(
            MarketEvent event,
            IdentityHashMap<Object, Boolean> mutableNodes,
            LinkedHashMap<Integer, MarketEvent> validated)
            throws EngineOperationException {
        if (event == null || event.getClass() != MarketEvent.class) {
            throw invalid("The saved state contains an invalid event.");
        }
        requireDistinct(mutableNodes, event);
        if (validated.containsKey(event.getEventId())) {
            throw invalid("Each saved event ID must be unique.");
        }
        requireCanonicalEventName(event.getName());
        requireOuterTrimmedText(event.getDescription(), "event description");
        if (event.getLiquidityParameter() <= 0) {
            throw invalid("Each saved event must have a positive liquidity parameter.");
        }

        CommissionPolicy policy = event.getCommissionPolicy();
        if (policy == null || policy.getClass() != CommissionPolicy.class
                || policy.getMode() == null
                || (policy.getMode() != CommissionMode.ON_PURCHASE
                && policy.getMode() != CommissionMode.ON_CLOSE)
                || policy.getPercentage() < 0 || policy.getPercentage() > 90) {
            throw invalid("The saved event commission policy is invalid.");
        }

        List<MarketOption> options = event.getOptions();
        if (options == null || options.getClass() != ArrayList.class || options.size() != 2) {
            throw invalid("Each saved event must contain exactly two ordered options.");
        }
        requireDistinct(mutableNodes, options);
        validateOption(options.get(0), 1, mutableNodes);
        validateOption(options.get(1), 2, mutableNodes);

        List<TradeRecord> history = event.getPurchaseHistory();
        if (history == null || history.getClass() != ArrayList.class) {
            throw invalid("The saved event purchase history is invalid.");
        }
        requireDistinct(mutableNodes, history);
        EventAccount account = event.getAccount();
        if (account == null || account.getClass() != EventAccount.class) {
            throw invalid("The saved event account is invalid.");
        }
        requireDistinct(mutableNodes, account);
        validateLifecycle(event, options);
        validateHistoryAndAccount(event, options, history, account, mutableNodes);
        validated.put(event.getEventId(), event);
    }

    private void validateOption(
            MarketOption option,
            int expectedNumber,
            IdentityHashMap<Object, Boolean> mutableNodes)
            throws EngineOperationException {
        if (option == null || option.getClass() != MarketOption.class
                || option.getOptionNumber() != expectedNumber || option.getQuantity() < 0) {
            throw invalid("The saved event options are invalid.");
        }
        requireDistinct(mutableNodes, option);
        requireOuterTrimmedText(option.getLabel(), "option label");
    }

    private void validateHistoryAndAccount(
            MarketEvent event,
            List<MarketOption> options,
            List<TradeRecord> history,
            EventAccount account,
            IdentityHashMap<Object, Boolean> mutableNodes)
            throws EngineOperationException {
        int firstQuantity = 0;
        int secondQuantity = 0;
        double expectedBalance = 0.0;
        double expectedCommission = 0.0;
        for (TradeRecord record : history) {
            if (record == null || record.getClass() != TradeRecord.class) {
                throw invalid("The saved event purchase history is invalid.");
            }
            requireDistinct(mutableNodes, record);
            int optionNumber = record.getOptionNumber();
            if ((optionNumber != 1 && optionNumber != 2) || record.getQuantity() <= 0) {
                throw invalid("A saved purchase record does not match its event option.");
            }
            String optionLabel = record.getOptionLabel();
            requireOuterTrimmedText(optionLabel, "purchase option label");
            if (!optionLabel.equals(options.get(optionNumber - 1).getLabel())) {
                throw invalid("A saved purchase record does not match its event option.");
            }
            requireFinitePositive(record.getBaseShareCost(), "purchase base cost");
            requireFiniteNonnegative(record.getPurchaseCommission(), "purchase commission");
            requireFinitePositive(record.getTotalPaid(), "purchase total");
            if (!financiallyEqual(record.getTotalPaid(),
                    record.getBaseShareCost() + record.getPurchaseCommission())) {
                throw invalid("A saved purchase total is inconsistent.");
            }
            validatePurchaseCommission(event.getCommissionPolicy(), record.getBaseShareCost(),
                    record.getPurchaseCommission());
            try {
                if (optionNumber == 1) {
                    firstQuantity = Math.addExact(firstQuantity, record.getQuantity());
                } else {
                    secondQuantity = Math.addExact(secondQuantity, record.getQuantity());
                }
            } catch (ArithmeticException exception) {
                throw invalid("Saved purchase quantities overflow their option quantity.", exception);
            }
            expectedBalance = addFinite(expectedBalance, record.getTotalPaid(), "account balance");
            expectedCommission = addFinite(expectedCommission, record.getPurchaseCommission(),
                    "total commission");
        }
        if (firstQuantity != options.get(0).getQuantity()
                || secondQuantity != options.get(1).getQuantity()) {
            throw invalid("Saved option quantities do not match purchase history.");
        }

        if (event.getStatus() == EventStatus.CLOSED) {
            OptionalInt winner = event.getWinningOptionNumber();
            int winnerNumber = winner.orElseThrow(() -> new IllegalStateException("missing winner"));
            double grossPayout = options.get(winnerNumber - 1).getQuantity();
            double closingCommission = event.getCommissionPolicy().closingCommission(grossPayout);
            requireFiniteNonnegative(closingCommission, "closing commission");
            expectedBalance = addFinite(expectedBalance, -(grossPayout - closingCommission),
                    "closed account balance");
            expectedCommission = addFinite(expectedCommission, closingCommission,
                    "closed total commission");
        }
        requireFinite(account.getBalance(), "saved account balance");
        requireFiniteNonnegative(account.getTotalCommissionCollected(), "saved total commission");
        if (!financiallyEqual(account.getBalance(), expectedBalance)
                || !financiallyEqual(account.getTotalCommissionCollected(), expectedCommission)) {
            throw invalid("Saved account values are inconsistent with event history.");
        }
    }

    private void validateLifecycle(MarketEvent event, List<MarketOption> options)
            throws EngineOperationException {
        EventStatus status = event.getStatus();
        OptionalInt winner = event.getWinningOptionNumber();
        if (status == EventStatus.OPEN && winner.isEmpty()) {
            return;
        }
        if (status == EventStatus.CLOSED && winner.isPresent()
                && winner.getAsInt() >= 1 && winner.getAsInt() <= options.size()) {
            return;
        }
        throw invalid("Saved event lifecycle state is inconsistent.");
    }

    private static void validatePurchaseCommission(
            CommissionPolicy policy,
            double baseShareCost,
            double commission)
            throws EngineOperationException {
        double expectedCommission = policy.purchaseCommission(baseShareCost);
        if (policy.getMode() == CommissionMode.ON_CLOSE || policy.getPercentage() == 0) {
            if (commission != 0.0) {
                throw invalid("Saved purchase commission is inconsistent with its policy.");
            }
        } else if (commission <= 0.0 || !financiallyEqual(commission, expectedCommission)) {
            throw invalid("Saved purchase commission is inconsistent with its policy.");
        }
    }

    private static void requireCanonicalEventName(String value) throws EngineOperationException {
        if (value == null || value.getClass() != String.class) {
            throw invalid("Saved event name is invalid.");
        }
        String trimmed = value.trim();
        String canonical = trimmed.isEmpty() ? "" : trimmed.replaceAll("\\s+", " ");
        if (!value.equals(canonical)) {
            throw invalid("Saved event name is not in its XML-normalized form.");
        }
    }

    private static void requireOuterTrimmedText(String value, String field)
            throws EngineOperationException {
        if (value == null || value.getClass() != String.class || !value.equals(value.trim())) {
            throw invalid("Saved " + field + " is not in its XML-normalized form.");
        }
    }

    private static void requireFinite(double value, String field) throws EngineOperationException {
        if (!Double.isFinite(value)) {
            throw invalid("Saved " + field + " must be finite.");
        }
    }

    private static void requireFinitePositive(double value, String field) throws EngineOperationException {
        requireFinite(value, field);
        if (value <= 0.0) {
            throw invalid("Saved " + field + " must be positive.");
        }
    }

    private static void requireFiniteNonnegative(double value, String field)
            throws EngineOperationException {
        requireFinite(value, field);
        if (value < 0.0) {
            throw invalid("Saved " + field + " must be nonnegative.");
        }
    }

    private static double addFinite(double first, double second, String field)
            throws EngineOperationException {
        double result = first + second;
        requireFinite(result, field);
        return result;
    }

    private static boolean financiallyEqual(double actual, double expected) {
        if (!Double.isFinite(actual) || !Double.isFinite(expected)) {
            return false;
        }
        double scale = Math.max(Math.abs(actual), Math.abs(expected));
        return Math.abs(actual - expected)
                <= FINANCIAL_EQUALITY_ULPS * Math.ulp(scale);
    }

    private static void requireDistinct(IdentityHashMap<Object, Boolean> mutableNodes, Object node)
            throws EngineOperationException {
        if (mutableNodes.put(node, Boolean.TRUE) != null) {
            throw invalid("The saved state contains a mutable object alias.");
        }
    }

    private static EngineOperationException invalid(String detail) {
        return invalid(detail, null);
    }

    private static EngineOperationException invalid(String detail, Throwable cause) {
        return new EngineOperationException(
                EngineErrorCode.SAVED_STATE_INVALID,
                detail,
                "Restore a valid saved state file and try again.",
                null, null, null, null, null, null, null, null, null, cause);
    }
}
