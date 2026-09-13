package guessmarket.engine;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OrderBookMatcherTest {
    @Test void ordinarySelfMatchRejectsWholePlanEvenAfterAnEarlierOtherFill(){
        var book=List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.40")),
                new OrderBookMatcher.SubmittedOrder(2,"Owner",quote(2,"0.50")));
        var failure=assertThrows(WorldCommandException.class,()->OrderBookMatcher.planNewOrder(
                "Owner",OrderBookMatcher.Side.BUY,quote(3,"0.60"),book));
        assertEquals(WorldCommandException.Code.SELF_MATCH_REJECTED,failure.getCode());
        assertTrue(failure.getMessage().contains("own"));
        assertEquals(2,book.getFirst().quote().quantity());assertEquals(2,book.getLast().quote().quantity());
    }
    @Test void sellSelfMatchRejectedButOwnerTradingWithOthersAllowed() throws Exception {
        var own=List.of(new OrderBookMatcher.SubmittedOrder(1,"Owner",quote(2,"0.60")));
        assertThrows(WorldCommandException.class,()->OrderBookMatcher.planNewOrder(
                "Owner",OrderBookMatcher.Side.SELL,quote(1,"0.50"),own));
        var others=List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.60")));
        assertEquals(1,OrderBookMatcher.planNewOrder("Owner",OrderBookMatcher.Side.SELL,quote(1,"0.50"),others).fills().size());
    }
    @Test void normalMatchingWinsEvenWhenMintCandidateIsOlder(){
        var result=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.BUY,quote(10,"0.65"),
                List.of(new OrderBookMatcher.SubmittedOrder(20,"Other",quote(10,"0.60"))),
                List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(10,"0.45"))),1,true);
        money("6.00",result.ordinary().principal());assertTrue(result.mints().isEmpty());
        assertEquals(0,result.incomingRemaining());assertEquals(10,result.oppositeRemaining().getFirst().quote().quantity());
    }
    @Test void partialOrdinaryFillThenMintProcessesOnlyIncomingRemainder(){
        var result=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.BUY,quote(10,"0.65"),
                List.of(new OrderBookMatcher.SubmittedOrder(20,"Other",quote(4,"0.60"))),
                List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(10,"0.45"))),1,true);
        money("2.40",result.ordinary().principal());assertEquals(4,result.ordinary().fills().getFirst().quantity());
        var mint=result.mints().getFirst();assertEquals(6,mint.fill().quantity());money("0.55",mint.fill().incomingPrice());
        money("6",mint.fill().contractCredit());assertEquals(0,result.incomingRemaining());
        assertEquals(4,result.oppositeRemaining().getFirst().quote().quantity());
    }
    @Test void equalPriceMintCandidatesUseOldestSequence(){
        var result=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.BUY,quote(3,"0.60"),List.of(),
                List.of(new OrderBookMatcher.SubmittedOrder(8,"Other",quote(3,"0.40")),
                        new OrderBookMatcher.SubmittedOrder(2,"Other",quote(2,"0.400"))),1,true);
        assertEquals(List.of(2L,8L),result.mints().stream().map(OrderBookMatcher.SubmittedMint::sequence).toList());
        assertEquals(List.of(2,1),result.mints().stream().map(m->m.fill().quantity()).toList());
        assertEquals(0,result.incomingRemaining());
    }
    @Test void highestEligibleMintPriceWinsBeforeOlderLowerBid(){
        var result=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.BUY,quote(3,"0.65"),List.of(),
                List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.40")),
                        new OrderBookMatcher.SubmittedOrder(2,"Other",quote(2,"0.45"))),1,true);
        assertEquals(List.of(2L,1L),result.mints().stream().map(OrderBookMatcher.SubmittedMint::sequence).toList());
        assertEquals(List.of(2,1),result.mints().stream().map(m->m.fill().quantity()).toList());
        money("0.55",result.mints().getFirst().fill().incomingPrice());
        money("0.60",result.mints().getLast().fill().incomingPrice());
        assertEquals(0,result.incomingRemaining());assertEquals(1,result.oppositeRemaining().getFirst().quote().quantity());
    }
    @Test void sellAndDisabledMintDoNotCreatePairs(){
        for(var side:OrderBookMatcher.Side.values()){
            var result=OrderBookMatcher.matchThenMint(side,quote(3,"0.70"),List.of(),
                    List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.40"))),1,false);
            assertTrue(result.mints().isEmpty());assertEquals(3,result.incomingRemaining());
        }
        var sell=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.SELL,quote(3,"0.70"),List.of(),
                List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.40"))),1,true);
        assertTrue(sell.mints().isEmpty());assertEquals(3,sell.incomingRemaining());
    }
    @Test void incomingBuyConsumesLowestAsksThenOldestEqualPrice(){
        var book=List.of(new OrderBookMatcher.SubmittedOrder(1,"Other",quote(3,"0.65")),
                new OrderBookMatcher.SubmittedOrder(3,"Other",quote(3,"0.50")),
                new OrderBookMatcher.SubmittedOrder(2,"Other",quote(5,"0.50")));
        var result=OrderBookMatcher.matchNewOrder(OrderBookMatcher.Side.BUY,quote(7,"0.60"),book);
        assertEquals(List.of(2L,3L),result.fills().stream().map(OrderBookMatcher.SubmittedFill::sequence).toList());
        assertEquals(List.of(5,2),result.fills().stream().map(OrderBookMatcher.SubmittedFill::quantity).toList());
        assertEquals(List.of(3L,1L),result.restingRemaining().stream().map(OrderBookMatcher.SubmittedOrder::sequence).toList());
        assertEquals(1,result.restingRemaining().getFirst().quote().quantity());
        assertEquals(0,result.incomingRemaining());money("3.50",result.principal());
        assertEquals(3,book.get(1).quote().quantity());
    }
    @Test void incomingSellConsumesHighestBidsThenOldestEqualPrice(){
        var book=List.of(new OrderBookMatcher.SubmittedOrder(3,"Other",quote(4,"0.50")),
                new OrderBookMatcher.SubmittedOrder(8,"Other",quote(4,"0.60")),
                new OrderBookMatcher.SubmittedOrder(2,"Other",quote(1,"0.60")));
        var result=OrderBookMatcher.matchNewOrder(OrderBookMatcher.Side.SELL,quote(3,"0.50"),book);
        assertEquals(List.of(2L,8L),result.fills().stream().map(OrderBookMatcher.SubmittedFill::sequence).toList());
        assertEquals(List.of(1,2),result.fills().stream().map(OrderBookMatcher.SubmittedFill::quantity).toList());
        assertEquals(2,result.restingRemaining().getFirst().quote().quantity());money("1.80",result.principal());
    }
    @Test void duplicateSubmissionSequencesCannotDefineFifo(){
        var order=new OrderBookMatcher.SubmittedOrder(1,"Other",quote(2,"0.50"));
        assertThrows(IllegalArgumentException.class,()->OrderBookMatcher.matchNewOrder(
                OrderBookMatcher.Side.BUY,quote(3,"0.60"),List.of(order,order)));
    }
    private OrderBookMatcher.Quote quote(int quantity,String price){
        return new OrderBookMatcher.Quote(quantity,new BigDecimal(price));
    }
    private void money(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    @Test void ordinaryExecutionUsesRestingPriceInBothDirections(){
        var sell=OrderBookMatcher.ordinary(OrderBookMatcher.Side.SELL,quote(10,"0.50"),List.of(quote(10,"0.60")));
        money("0.60",sell.fills().getFirst().price());money("6.00",sell.principal());
        var buy=OrderBookMatcher.ordinary(OrderBookMatcher.Side.BUY,quote(10,"0.60"),List.of(quote(10,"0.50")));
        money("0.50",buy.fills().getFirst().price());money("5.00",buy.principal());
    }
    @Test void suppliedSimulatorSellWalksBestBidsAndLeavesPartialOrder(){
        var resting=List.of(quote(20,"0.50"),quote(15,"0.48"));
        var result=OrderBookMatcher.ordinary(OrderBookMatcher.Side.SELL,quote(30,"0.45"),resting);
        money("14.80",result.principal());assertEquals(0,result.incomingRemaining());
        assertEquals(List.of(20,10),result.fills().stream().map(OrderBookMatcher.Fill::quantity).toList());
        assertEquals(List.of(quote(5,"0.48")),result.restingRemaining());
        assertEquals(15,resting.get(1).quantity());
        assertThrows(UnsupportedOperationException.class,()->result.fills().clear());
        assertThrows(UnsupportedOperationException.class,()->result.restingRemaining().clear());
    }
    @Test void explicitCallerPriorityConsumesTwentyThenThirtyWithoutChoosingFifo(){
        var result=OrderBookMatcher.ordinary(OrderBookMatcher.Side.BUY,quote(50,"0.60"),
                List.of(quote(20,"0.50"),quote(40,"0.50")));
        assertEquals(List.of(0,1),result.fills().stream().map(OrderBookMatcher.Fill::restingIndex).toList());
        assertEquals(List.of(20,30),result.fills().stream().map(OrderBookMatcher.Fill::quantity).toList());
        assertEquals(List.of(quote(10,"0.50")),result.restingRemaining());money("25",result.principal());
    }
    @Test void unmatchedRemainderAndUnsupportedPriorityAreExplicit(){
        var book=List.of(quote(4,"0.70"));
        var result=OrderBookMatcher.ordinary(OrderBookMatcher.Side.BUY,quote(5,"0.60"),book);
        assertTrue(result.fills().isEmpty());assertEquals(5,result.incomingRemaining());assertEquals(book,result.restingRemaining());
        assertThrows(IllegalArgumentException.class,()->OrderBookMatcher.ordinary(OrderBookMatcher.Side.BUY,
                quote(5,"0.90"),List.of(quote(3,"0.70"),quote(2,"0.60"))));
    }
    @Test void suppliedMintPreservesRestingPriceAndCreatesOnlySmallerQuantity(){
        var result=OrderBookMatcher.mint(quote(40,"0.62"),quote(35,"0.42"),1);
        assertEquals(OrderBookMatcher.MintStatus.MATCHED,result.status());
        var fill=result.fill().orElseThrow();
        assertEquals(35,fill.quantity());money("0.58",fill.incomingPrice());money("0.42",fill.restingPrice());
        assertEquals(5,fill.incomingRemaining());assertEquals(0,fill.restingRemaining());
        money("35",fill.contractCredit());money("20.30",fill.incomingPrincipal());money("14.70",fill.restingPrincipal());
        money("35",fill.incomingPrincipal().add(fill.restingPrincipal()));
    }
    @Test void approvedEqualityMintsAndBelowBaseDoesNotMint(){
        var equal=OrderBookMatcher.mint(quote(2,"0.60"),quote(3,"0.40"),1);
        assertEquals(OrderBookMatcher.MintStatus.MATCHED,equal.status());
        var fill=equal.fill().orElseThrow();assertEquals(2,fill.quantity());
        money("0.60",fill.incomingPrice());money("0.40",fill.restingPrice());money("2",fill.contractCredit());
        assertEquals(0,fill.incomingRemaining());assertEquals(1,fill.restingRemaining());
        assertEquals(OrderBookMatcher.MintStatus.NO_MATCH,
                OrderBookMatcher.mint(quote(2,"0.59"),quote(3,"0.40"),1).status());
    }
    @Test void nonUnitDenominatorMintKeepsExactDecimalBacking(){
        var fill=OrderBookMatcher.mint(quote(2,"3.20"),quote(5,"2.10"),5).fill().orElseThrow();
        money("2.90",fill.incomingPrice());money("2.10",fill.restingPrice());money("10",fill.contractCredit());
        assertEquals(0,fill.incomingRemaining());assertEquals(3,fill.restingRemaining());
    }
}
