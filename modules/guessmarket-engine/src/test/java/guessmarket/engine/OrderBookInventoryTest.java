package guessmarket.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OrderBookInventoryTest {
    @Test void reservationsArePerOutcomeAndRejectOversellAtomically() throws Exception {
        var initial=OrderBookState.opened("Owner",10);
        var reserved=initial.reserveSell(1,"Owner",1,8);
        assertEquals(2,reserved.availableShares("Owner",1));
        assertEquals(10,reserved.availableShares("Owner",2));
        assertEquals(0,reserved.availableShares("Other",1));
        var before=reserved.snapshot();
        var failure=assertThrows(WorldCommandException.class,()->reserved.reserveSell(2,"Owner",1,5));
        assertEquals(WorldCommandException.Code.INVALID_QUANTITY,failure.getCode());
        assertTrue(failure.getMessage().contains("2"));
        assertEquals(before,reserved.snapshot());assertEquals(10,initial.availableShares("Owner",1));
    }
    @Test void partialAndFullFillTransferSharesAndReduceReservation() throws Exception {
        var reserved=OrderBookState.opened("Owner",10).reserveSell(1,"Owner",1,8);
        var partial=reserved.fillSell(1,"Buyer",3);
        assertEquals(7,partial.snapshot().holdings().stream().filter(h->h.userName().equals("Owner")).findFirst().orElseThrow().optionOneShares());
        assertEquals(3,partial.availableShares("Buyer",1));
        assertEquals(5,partial.sellReservations().getFirst().quantity());
        assertEquals(2,partial.availableShares("Owner",1));
        var filled=partial.fillSell(1,"Buyer",5);
        assertTrue(filled.sellReservations().isEmpty());assertEquals(8,filled.availableShares("Buyer",1));
        assertEquals(2,filled.availableShares("Owner",1));assertEquals(10,filled.issuedPairs());
        assertEquals(8,reserved.sellReservations().getFirst().quantity());
    }
    @Test void multipleReservationsAndBuyersRemainIndependent() throws Exception {
        var initial=OrderBookState.opened("Owner",10).reserveSell(1,"Owner",1,4).fillSell(1,"Buyer",4);
        var reserved=initial.reserveSell(2,"Owner",1,5).reserveSell(3,"Buyer",1,3).reserveSell(4,"Owner",2,8);
        assertEquals(1,reserved.availableShares("Owner",1));assertEquals(1,reserved.availableShares("Buyer",1));
        assertEquals(2,reserved.availableShares("Owner",2));
        assertThrows(WorldCommandException.class,()->reserved.reserveSell(5,"Buyer",1,2));
    }
    @Test void closingCancellationReleasesAllReservationsWithoutMovingHoldings() throws Exception {
        var reserved=OrderBookState.opened("Owner",10).reserveSell(1,"Owner",1,8).reserveSell(2,"Owner",2,7);
        var cleared=reserved.releaseAllReservations();
        assertTrue(cleared.sellReservations().isEmpty());
        assertEquals(reserved.holdings(),cleared.holdings());assertEquals(10,cleared.availableShares("Owner",1));
        assertEquals(10,cleared.availableShares("Owner",2));assertEquals(2,reserved.sellReservations().size());
        assertThrows(UnsupportedOperationException.class,()->reserved.snapshot().sellReservations().clear());
    }
    @Test void invalidFillAndDuplicateOrderLeaveReservationsUntouched() throws Exception {
        var reserved=OrderBookState.opened("Owner",10).reserveSell(1,"Owner",1,8);
        var before=reserved.snapshot();
        assertThrows(WorldCommandException.class,()->reserved.fillSell(1,"Buyer",9));
        assertThrows(WorldCommandException.class,()->reserved.fillSell(99,"Buyer",1));
        assertThrows(WorldCommandException.class,()->reserved.reserveSell(1,"Owner",2,1));
        assertThrows(WorldCommandException.class,()->reserved.reserveSell(2,"Owner",3,1));
        assertThrows(WorldCommandException.class,()->reserved.reserveSell(2,"Owner",2,0));
        assertEquals(before,reserved.snapshot());
    }
}

