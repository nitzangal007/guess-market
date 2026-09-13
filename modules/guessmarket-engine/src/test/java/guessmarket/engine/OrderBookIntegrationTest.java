package guessmarket.engine;

import guessmarket.dto.world.*;
import java.math.BigDecimal;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class OrderBookIntegrationTest {
    @TempDir Path directory;
    private GuessMarketWorldEngineImpl engine(int fee,String mode,int initial,int d,boolean mint) throws Exception {
        String event="<GM-event name=\"Book%s\"><id>%s</id><description>Test</description><commission type=\""+mode+"\">"+fee+"</commission>"
                +"<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method><GM-order-book initial=\""+initial+"\" d=\""+d+"\" allow-mint=\""+mint+"\"/></GM-method></GM-event>";
        String xml="<Guess-Market><GM-events>"+event.formatted(1,1)+event.formatted(2,2)+"</GM-events><GM-users>"
                +"<GM-user name=\"Owner\"><initial-cash>1000</initial-cash><GM-market-maker><event id=\"1\"/><event id=\"2\"/></GM-market-maker></GM-user>"
                +"<GM-user name=\"A\"><initial-cash>5</initial-cash></GM-user><GM-user name=\"B\"><initial-cash>1000</initial-cash></GM-user></GM-users></Guess-Market>";
        Path path=Files.writeString(directory.resolve("world.xml"),xml);
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(path);
        for(int id:new int[]{1,2}){var p=engine.previewOrderBookOpening("Owner",id);engine.openOrderBookEvent("Owner",id,p.worldRevision());}
        return engine;
    }
    private OrderRequest request(String user,int event,int option,OrderSide side,int quantity,String price){
        return new OrderRequest(user,event,option,side,quantity,new BigDecimal(price));
    }
    private OrderResult order(GuessMarketWorldEngine engine,String user,int event,int option,OrderSide side,int quantity,String price)throws Exception{
        var request=request(user,event,option,side,quantity,price);var preview=engine.previewOrder(request);
        return engine.submitOrder(request,preview.worldRevision());
    }
    private EventSnapshot event(GuessMarketWorldEngine engine,int id)throws Exception{return engine.getWorldSnapshot().events().stream().filter(e->e.id()==id).findFirst().orElseThrow();}
    private UserSnapshot user(GuessMarketWorldEngine engine,String name)throws Exception{return engine.getWorldSnapshot().users().stream().filter(u->u.name().equals(name)).findFirst().orElseThrow();}
    @Test void waitingBuyDoesNotDebitAndCountsAsParticipantThenOrdinaryFillTransfers()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"A",1,1,OrderSide.BUY,4,"0.60");assertEquals(5,user(engine,"A").currentBalance());
        assertTrue(user(engine,"A").participatingEventIds().contains(1));
        order(engine,"Owner",1,1,OrderSide.SELL,2,"0.50");
        assertEquals(3.8,user(engine,"A").currentBalance(),1e-12);assertEquals(801.2,user(engine,"Owner").currentBalance(),1e-12);
        var book=event(engine,1).orderBook().orElseThrow();assertEquals(1,book.orders().size());assertEquals(2,book.orders().getFirst().quantity());
        assertEquals(100,event(engine,1).contractBalance());assertEquals(2,user(engine,"A").positions().getFirst().quantityOne());
    }
    @Test void oversellAndSelfOrdinaryAreAtomicEvenAfterEarlierPotentialFill()throws Exception{
        var engine=engine(0,"on-purchase",10,1,false);
        order(engine,"Owner",1,1,OrderSide.SELL,8,"0.50");var before=engine.getWorldSnapshot();
        assertThrows(WorldCommandException.class,()->engine.previewOrder(request("Owner",1,1,OrderSide.SELL,5,"0.50")));assertEquals(before,engine.getWorldSnapshot());
        assertEquals(WorldCommandException.Code.SELF_MATCH_REJECTED,assertThrows(WorldCommandException.class,
                ()->engine.previewOrder(request("Owner",1,1,OrderSide.BUY,1,"0.60"))).getCode());
        assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void ordinaryThenMintChargesBothBuyersAndBlocksOnlyAfterWholeCommand()throws Exception{
        var engine=engine(10,"on-purchase",100,1,true);
        order(engine,"B",1,2,OrderSide.BUY,20,"0.45");
        order(engine,"Owner",1,1,OrderSide.SELL,4,"0.60");
        var result=order(engine,"A",1,1,OrderSide.BUY,10,"0.65");
        assertEquals(-1.27,user(engine,"A").currentBalance(),1e-12);assertTrue(user(engine,"A").blocked());
        assertEquals(10,user(engine,"A").positions().getFirst().quantityOne());
        assertEquals(106,event(engine,1).contractBalance());assertEquals(3,result.execution().fills().size());
        assertTrue(event(engine,1).orderBook().orElseThrow().orders().stream().noneMatch(o->o.userName().equals("A")));
    }
    @Test void selfMintRejectsWholeNewOrderAndEqualityIsAcceptedForOthers()throws Exception{
        var engine=engine(0,"on-purchase",0,1,true);
        order(engine,"A",1,2,OrderSide.BUY,2,"0.40");var before=engine.getWorldSnapshot();
        assertEquals(WorldCommandException.Code.SELF_MATCH_REJECTED,assertThrows(WorldCommandException.class,
                ()->engine.previewOrder(request("A",1,1,OrderSide.BUY,2,"0.60"))).getCode());
        assertEquals(before,engine.getWorldSnapshot());
        order(engine,"B",1,1,OrderSide.BUY,2,"0.60");
        assertEquals(2,event(engine,1).contractBalance());assertEquals(2,event(engine,1).orderBook().orElseThrow().issuedPairs());
    }
    @Test void finalBlockCancelsOrdersAcrossEventsAndPayoutRecoveryNeverResurrects()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"Owner",2,1,OrderSide.SELL,1,"0");
        order(engine,"A",2,1,OrderSide.BUY,1,"0");
        order(engine,"A",2,1,OrderSide.SELL,1,"0.90");
        order(engine,"A",2,2,OrderSide.BUY,3,"0.20");
        order(engine,"Owner",1,1,OrderSide.SELL,20,"0.60");
        order(engine,"A",1,1,OrderSide.BUY,20,"0.60");
        assertTrue(user(engine,"A").blocked());assertTrue(event(engine,2).orderBook().orElseThrow().orders().stream().noneMatch(o->o.userName().equals("A")));
        assertTrue(event(engine,2).orderBook().orElseThrow().sellReservations().stream().noneMatch(o->o.userName().equals("A")));
        var close=engine.previewOrderBookClose("Owner",1,1);engine.closeOrderBookEvent("Owner",1,1,close.worldRevision());
        assertEquals(13,user(engine,"A").currentBalance(),1e-12);assertFalse(user(engine,"A").blocked());
        assertTrue(event(engine,2).orderBook().orElseThrow().orders().stream().noneMatch(o->o.userName().equals("A")));
        assertDoesNotThrow(()->order(engine,"A",2,2,OrderSide.BUY,1,"0.10"));
    }
    @Test void zeroSubcentAndUpperBoundAreValidatedAndPreviewRevisionIsAtomic()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"A",1,1,OrderSide.BUY,1,"0.3333");
        var req=request("B",1,2,OrderSide.BUY,1,"0.99");var preview=engine.previewOrder(req);
        order(engine,"A",2,1,OrderSide.BUY,1,"0");
        assertEquals(WorldCommandException.Code.STALE_WORLD,assertThrows(WorldCommandException.class,
                ()->engine.submitOrder(req,preview.worldRevision())).getCode());
        var before=engine.getWorldSnapshot();
        assertThrows(WorldCommandException.class,()->engine.previewOrder(request("B",1,1,OrderSide.BUY,1,"0.9901")));
        assertThrows(WorldCommandException.class,()->engine.previewOrder(request("B",1,1,OrderSide.BUY,0,"0.5")));assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void selfMatchAfterOtherLiquidityStillRejectsEveryPlannedFill()throws Exception{
        var engine=engine(0,"on-purchase",100,1,true);
        order(engine,"Owner",1,1,OrderSide.SELL,1,"0.10");
        order(engine,"A",1,1,OrderSide.BUY,1,"0.10");
        order(engine,"Owner",1,1,OrderSide.SELL,1,"0.20");
        order(engine,"A",1,1,OrderSide.SELL,1,"0.30");
        var before=engine.getWorldSnapshot();
        assertEquals(WorldCommandException.Code.SELF_MATCH_REJECTED,assertThrows(WorldCommandException.class,
                ()->engine.submitOrder(request("A",1,1,OrderSide.BUY,2,"0.40"),
                        engine.previewOrder(request("B",2,1,OrderSide.BUY,1,"0")).worldRevision())).getCode());
        assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void selfMintAfterOrdinaryPlanDoesNotLeakOrdinaryFill()throws Exception{
        var engine=engine(0,"on-purchase",100,1,true);
        order(engine,"Owner",1,1,OrderSide.SELL,1,"0.20");
        order(engine,"A",1,2,OrderSide.BUY,2,"0.40");
        var before=engine.getWorldSnapshot();
        assertEquals(WorldCommandException.Code.SELF_MATCH_REJECTED,assertThrows(WorldCommandException.class,
                ()->engine.previewOrder(request("A",1,1,OrderSide.BUY,3,"0.60"))).getCode());
        assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void restingBuyerOverdraftFinishesAllItsFillsThenCancelsRemainder()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"A",1,1,OrderSide.BUY,10,"0.60");
        order(engine,"A",1,1,OrderSide.BUY,10,"0.60");
        var result=order(engine,"Owner",1,1,OrderSide.SELL,15,"0.50");
        assertEquals(2,result.execution().fills().size());assertEquals(-4,user(engine,"A").currentBalance());
        assertEquals(15,user(engine,"A").positions().getFirst().quantityOne());
        assertTrue(user(engine,"A").blocked());assertEquals(1,result.execution().cancelledOrderCount());
        assertTrue(event(engine,1).orderBook().orElseThrow().orders().isEmpty());
    }
    @Test void bothQuotesUseMidAndOneSidedBookFallsBackToLast()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"A",1,1,OrderSide.BUY,1,"0.30");
        var quote=event(engine,1).orderBook().orElseThrow().quotes().getFirst();
        assertEquals("UNAVAILABLE",quote.valuationBasis());assertTrue(quote.mid().isEmpty());assertTrue(quote.spread().isEmpty());
        order(engine,"Owner",1,1,OrderSide.SELL,2,"0.50");
        quote=event(engine,1).orderBook().orElseThrow().quotes().getFirst();
        assertEquals("MID",quote.valuationBasis());assertEquals(0,new BigDecimal("0.40").compareTo(quote.estimate().orElseThrow()));
        order(engine,"B",1,1,OrderSide.BUY,2,"0.50");
        quote=event(engine,1).orderBook().orElseThrow().quotes().getFirst();
        assertEquals("LAST",quote.valuationBasis());assertTrue(quote.ask().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->event(engine,1).orderBook().orElseThrow().orders().clear());
    }
    @Test void participantPositionsExposePerOutcomeValuesForEveryValuationBasis()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"Owner",1,1,OrderSide.SELL,2,"0.50");
        order(engine,"A",1,1,OrderSide.BUY,1,"0.30");
        var active=event(engine,1).orderBook().orElseThrow();
        var owner=active.positions().stream().filter(p->p.userName().equals("Owner")).findFirst().orElseThrow();
        var unmatched=active.positions().stream().filter(p->p.userName().equals("A")).findFirst().orElseThrow();
        assertEquals("MID",active.quotes().getFirst().valuationBasis());
        assertEquals(0,new BigDecimal("40").compareTo(owner.optionOneValue().orElseThrow()));
        assertTrue(owner.optionTwoValue().isEmpty());
        assertEquals(0,BigDecimal.ZERO.compareTo(unmatched.optionOneValue().orElseThrow()));
        assertEquals(0,BigDecimal.ZERO.compareTo(unmatched.optionTwoValue().orElseThrow()));

        order(engine,"B",1,1,OrderSide.BUY,2,"0.50");
        active=event(engine,1).orderBook().orElseThrow();
        owner=active.positions().stream().filter(p->p.userName().equals("Owner")).findFirst().orElseThrow();
        assertEquals("LAST",active.quotes().getFirst().valuationBasis());
        assertEquals(0,new BigDecimal("49").compareTo(owner.optionOneValue().orElseThrow()));

        var close=engine.previewOrderBookClose("Owner",1,1);
        engine.closeOrderBookEvent("Owner",1,1,close.worldRevision());
        var closed=event(engine,1).orderBook().orElseThrow();
        owner=closed.positions().stream().filter(p->p.userName().equals("Owner")).findFirst().orElseThrow();
        assertEquals("SETTLEMENT",closed.quotes().getFirst().valuationBasis());
        assertEquals(0,new BigDecimal("98").compareTo(owner.optionOneValue().orElseThrow()));
        assertEquals(0,BigDecimal.ZERO.compareTo(owner.optionTwoValue().orElseThrow()));
    }
    @Test void saleLedgerAndFundingAllocationSurviveOptionTwoClose()throws Exception{
        var engine=engine(0,"on-close",100,1,false);
        order(engine,"Owner",1,2,OrderSide.SELL,10,"0.40");
        order(engine,"A",1,2,OrderSide.BUY,10,"0.40");
        order(engine,"A",1,2,OrderSide.SELL,4,"0.60");
        order(engine,"B",1,2,OrderSide.BUY,4,"0.60");
        var p=engine.previewOrderBookClose("Owner",1,2);engine.closeOrderBookEvent("Owner",1,2,p.worldRevision());
        var positions=event(engine,1).orderBook().orElseThrow().positions();
        var a=positions.stream().filter(v->v.userName().equals("A")).findFirst().orElseThrow();
        assertEquals(0,new BigDecimal("4.40").compareTo(a.profitLoss()));assertEquals(6,a.optionTwoShares());
        assertEquals(0,new BigDecimal("4.00").compareTo(a.purchases()));assertEquals(0,new BigDecimal("2.40").compareTo(a.saleReceipts()));
        var owner=positions.stream().filter(v->v.userName().equals("Owner")).findFirst().orElseThrow();
        assertEquals(0,new BigDecimal("50").compareTo(owner.optionOnePaid()));assertEquals(0,new BigDecimal("50").compareTo(owner.optionTwoPaid()));
        assertEquals(0,positions.stream().map(OrderBookPosition::profitLoss).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(BigDecimal.ZERO));
    }
    @Test void emptyCloseReleasesWaitingOrdersAndKeepsParticipation()throws Exception{
        var engine=engine(10,"on-close",0,5,false);
        order(engine,"A",1,1,OrderSide.BUY,1,"2");
        var p=engine.previewOrderBookClose("Owner",1,2);assertEquals(0,p.totalGrossPayout());
        engine.closeOrderBookEvent("Owner",1,2,p.worldRevision());
        var book=event(engine,1).orderBook().orElseThrow();
        assertTrue(book.orders().isEmpty());assertTrue(book.participants().contains("A"));
        assertTrue(user(engine,"A").positions().stream().anyMatch(v->v.eventId()==1));
        assertFalse(user(engine,"A").participatingEventIds().contains(1));
    }
    @Test void ownerCommissionRecoversAboveZeroButNotAtZero()throws Exception{
        for(int quantity:new int[]{39,40}){
            var engine=engine(10,"on-purchase",490,1,false);
            order(engine,"Owner",1,1,OrderSide.SELL,quantity,"0");
            order(engine,"B",1,1,OrderSide.BUY,quantity,"0");
            order(engine,"B",1,1,OrderSide.SELL,quantity,"0.50");
            var result=order(engine,"Owner",1,1,OrderSide.BUY,quantity,"0.50");
            assertEquals(quantity==39?0.5:0,user(engine,"Owner").currentBalance());
            assertEquals(quantity==40,user(engine,"Owner").blocked());
            assertEquals(quantity==40,result.execution().blockedAfter());
            assertDoesNotThrow(()->engine.previewOrderBookClose("Owner",1,1));
        }
    }
    @Test void lmsrOverdraftAlsoCancelsOrderBookOrders()throws Exception{
        var owner=new MarketUser("Owner",1000,java.util.List.of(1,2));
        var buyer=new MarketUser("A",5,java.util.List.of());
        var book=new WorldEvent(1,"Book","",java.util.List.of("Yes","No"),0,guessmarket.dto.CommissionMode.ON_PURCHASE,
                "Owner",new OrderBookConfiguration(100,1,false));
        var lmsr=new WorldEvent(2,"LMSR","",java.util.List.of("Yes","No"),0,guessmarket.dto.CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(100));
        var world=new MarketWorld(java.util.Map.of(1,book,2,lmsr),java.util.Map.of("Owner",owner,"A",buyer));
        world=world.openOrderBookEvent("Owner",1).openLmsrEvent("Owner",2);
        world=world.planOrder(request("A",1,1,OrderSide.BUY,5,"0.50"),10).world();
        var next=world.purchaseLmsrShares("A",2,1,100);
        assertTrue(next.snapshot().users().stream().filter(u->u.name().equals("A")).findFirst().orElseThrow().blocked());
        var state=next.snapshot().events().stream().filter(e->e.id()==1).findFirst().orElseThrow().orderBook().orElseThrow();
        assertTrue(state.orders().isEmpty());assertTrue(state.participants().contains("A"));
    }
    @Test void cashTooSmallToChangeExistingAccountRejectsWithoutFreeShares()throws Exception{
        var engine=engine(0,"on-purchase",100,1,false);
        order(engine,"Owner",1,1,OrderSide.SELL,1,"0.000000000000000000000000000001");
        var before=engine.getWorldSnapshot();
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                ()->engine.previewOrder(request("A",1,1,OrderSide.BUY,1,"0.10"))).getCode());
        assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void closeUsesNonUnitPayoutAndPreservesLedgerProfitAndValuation()throws Exception{
        var engine=engine(10,"on-close",100,5,false);
        order(engine,"Owner",1,1,OrderSide.SELL,2,"1.20");
        order(engine,"A",1,1,OrderSide.BUY,2,"1.30");
        var before=event(engine,1).orderBook().orElseThrow();
        assertEquals("LAST",before.quotes().getFirst().valuationBasis());
        var preview=engine.previewOrderBookClose("Owner",1,1);assertEquals(100,preview.totalGrossPayout());assertEquals(10,preview.totalCommission());
        engine.closeOrderBookEvent("Owner",1,1,preview.worldRevision());
        var closed=event(engine,1).orderBook().orElseThrow();assertEquals(0,event(engine,1).contractBalance());assertTrue(closed.orders().isEmpty());
        var a=closed.positions().stream().filter(p->p.userName().equals("A")).findFirst().orElseThrow();
        assertEquals(0,new BigDecimal("6.60").compareTo(a.profitLoss()));assertEquals(2,a.optionOneShares());
        assertEquals("SETTLEMENT",closed.quotes().getFirst().valuationBasis());
        assertThrows(WorldCommandException.class,()->engine.previewOrder(request("A",1,1,OrderSide.BUY,1,"1")));
    }
}
