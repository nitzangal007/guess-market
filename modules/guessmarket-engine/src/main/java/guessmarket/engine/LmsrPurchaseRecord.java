package guessmarket.engine;

import guessmarket.dto.world.PurchaseEntry;

record LmsrPurchaseRecord(long sequence, String userName, int optionNumber, int quantity,
                          double shareCost, double commission) {
    PurchaseEntry snapshot(int eventId, String optionLabel) {
        return new PurchaseEntry(sequence, userName, eventId, optionNumber, optionLabel,
                quantity, shareCost, commission, shareCost + commission);
    }
}

