package guessmarket.engine;

import java.io.Serializable;

final class EventAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private double balance;
    private double totalCommissionCollected;

    EventAccount() {
        balance = 0.0;
        totalCommissionCollected = 0.0;
    }

    double getBalance() {
        return balance;
    }

    double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    void commitPurchase(double newBalance, double newTotalCommissionCollected) {
        balance = newBalance;
        totalCommissionCollected = newTotalCommissionCollected;
    }

    void commitClose(double newBalance, double newTotalCommissionCollected) {
        balance = newBalance;
        totalCommissionCollected = newTotalCommissionCollected;
    }
}
