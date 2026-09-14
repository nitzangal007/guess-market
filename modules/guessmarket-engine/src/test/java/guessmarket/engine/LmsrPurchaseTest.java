package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LmsrPurchaseTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;
    @Test void reachableHugeOrderBookReceiptRejectsLostLmsrDebitAtomically() throws Exception {
        String events="";
        for(int id=1;id<=3;id++) events+="<GM-event name=\"Boundary "+id+"\"><id>"+id+"</id><description>Cash boundary</description>"
                +"<commission type=\"on-purchase\">0</commission><GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method>"
                +(id==3?"<GM-LMSR><b>1</b></GM-LMSR>":"<GM-order-book initial=\"0\" d=\"2147483647\" allow-mint=\"true\"/>")
                +"</GM-method></GM-event>";
        String xml="<Guess-Market><GM-events>"+events+"</GM-events><GM-users>"
                +"<GM-user name=\"Owner\"><initial-cash>1000</initial-cash><GM-market-maker><event id=\"1\"/><event id=\"2\"/><event id=\"3\"/></GM-market-maker></GM-user>"
                +"<GM-user name=\"A\"><initial-cash>1000</initial-cash></GM-user><GM-user name=\"B\"><initial-cash>1000</initial-cash></GM-user></GM-users></Guess-Market>";
        var engine=new GuessMarketWorldEngineImpl();
        engine.loadWorldFromXml(java.nio.file.Files.writeString(directory.resolve("boundary.xml"),xml));
        engine.openLmsrEvent("Owner",3,engine.previewLmsrOpening("Owner",3).worldRevision());
        for(int id:new int[]{1,2})engine.openOrderBookEvent("Owner",id,engine.previewOrderBookOpening("Owner",id).worldRevision());
        var witness=new OrderRequest("B",2,1,OrderSide.BUY,1,new java.math.BigDecimal("1"));
        engine.submitOrder(witness,engine.previewOrder(witness).worldRevision());
        var opposite=new OrderRequest("A",1,2,OrderSide.BUY,Integer.MAX_VALUE,new java.math.BigDecimal("1073741823"));
        engine.submitOrder(opposite,engine.previewOrder(opposite).worldRevision());
        var winning=new OrderRequest("Owner",1,1,OrderSide.BUY,Integer.MAX_VALUE,new java.math.BigDecimal("1073741824"));
        engine.submitOrder(winning,engine.previewOrder(winning).worldRevision());
        engine.closeOrderBookEvent("Owner",1,1,engine.previewOrderBookClose("Owner",1,1).worldRevision());
        var before=engine.getWorldSnapshot();
        var owner=before.users().stream().filter(u->u.name().equals("Owner")).findFirst().orElseThrow();
        assertTrue(owner.currentBalance()>2e18);assertFalse(owner.blocked());
        assertEquals(1,before.events().get(1).orderBook().orElseThrow().orders().size());
        long revision=engine.previewOrder(witness).worldRevision();
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                ()->engine.previewLmsrPurchase("Owner",3,1,1)).getCode());
        assertEquals(before,engine.getWorldSnapshot());
        assertEquals(revision,engine.previewOrder(witness).worldRevision());
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                ()->engine.purchaseLmsrShares("Owner",3,1,1,revision)).getCode());
        assertEquals(before,engine.getWorldSnapshot());
        assertEquals(revision,engine.previewOrder(witness).worldRevision());
    }

    @Test void selfMakerGrossDebitRecoversWithPositiveCashAfterOwnCommission() throws Exception {
        double base=LmsrCalculator.purchaseCost(0,0,100,100), fee=base*.05;
        for(double cash:new double[]{base+fee/2,Math.nextDown(base+fee),base+fee}) {
            var event=new WorldEvent(1,"Self purchase","Details",List.of("Yes","No"),5,CommissionMode.ON_PURCHASE,
                    "Owner",new LmsrConfiguration(100),WorldEventStatus.ACTIVE,LmsrCalculator.totalCost(0,0,100));
            var initial=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",cash,List.of(1))));
            var before=initial.snapshot();
            var preview=initial.previewLmsrPurchase("Owner",1,1,100,3);
            boolean shouldBlock=false;
            assertEquals(cash-(base+fee)<0,preview.overdraftBeforeReceipts());
            assertEquals(shouldBlock,preview.becomesBlocked());
            assertEquals(fee,preview.commissionReceived());
            assertEquals((cash-(base+fee))+fee,preview.balanceAfter());
            assertTrue(preview.balanceAfter()>0);
            assertEquals(before,initial.snapshot());
            var next=initial.purchaseLmsrShares("Owner",1,1,100);
            var account=next.snapshot().users().getFirst();
            assertEquals(preview.balanceAfter(),account.currentBalance());
            assertEquals(shouldBlock,account.blocked());
            assertEquals(fee,next.snapshot().events().getFirst().lmsrTrading().orElseThrow().totalPurchaseCommission());
            assertEquals(cash+before.events().getFirst().contractBalance(),
                    account.currentBalance()+next.snapshot().events().getFirst().contractBalance(),1e-12);
            assertDoesNotThrow(()->next.previewLmsrPurchase("Owner",1,2,1,0));
        }
    }
    @Test void engineRechecksRevisionAndReturnsOneCommittedReceipt() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();
        var path=java.nio.file.Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml");
        engine.loadWorldFromXml(path);
        var opening=engine.previewLmsrOpening("Tikva",1);
        engine.openLmsrEvent("Tikva",1,opening.worldRevision());
        var quote=engine.previewLmsrPurchase("Menash",1,1,1);
        var before=engine.getWorldSnapshot();
        assertEquals(before,engine.getWorldSnapshot());
        var result=engine.purchaseLmsrShares("Menash",1,1,1,quote.worldRevision());
        assertEquals(quote.totalDebit(),result.receipt().totalPaid());
        assertEquals(result.world(),engine.getWorldSnapshot());
        assertFalse(result.newlyBlocked());
        assertEquals(WorldCommandException.Code.STALE_WORLD,assertThrows(WorldCommandException.class,
                ()->engine.purchaseLmsrShares("Menash",1,1,1,quote.worldRevision())).getCode());
        var next=engine.previewLmsrPurchase("Avrum",1,2,3);
        engine.loadWorldFromXml(path);
        var reloaded=engine.getWorldSnapshot();
        assertThrows(WorldCommandException.class,()->engine.purchaseLmsrShares("Avrum",1,2,3,next.worldRevision()));
        assertEquals(reloaded,engine.getWorldSnapshot());
        assertEquals(0,before.events().getFirst().lmsrTrading().orElseThrow().quantityOne());
    }
    @Test void passiveFeeReceiptRecoversMarketMaker() throws Exception {
        var event=new WorldEvent(1,"Question","Details",List.of("Yes","No"),90,CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(100),WorldEventStatus.ACTIVE,100);
        var initial=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",-1,List.of(1),true),
                "Buyer",new MarketUser("Buyer",1000,List.of())));
        var next=initial.purchaseLmsrShares("Buyer",1,1,100);
        var owner=next.snapshot().users().stream().filter(u->u.name().equals("Owner")).findFirst().orElseThrow();
        assertTrue(owner.currentBalance()>0);assertFalse(owner.blocked());
        assertEquals("Owner",next.previewLmsrClose("Owner",1,1,1).actingUser());
        var settled=next.closeLmsrEvent("Owner",1,1).snapshot();
        assertFalse(settled.users().stream().filter(u->u.name().equals("Owner")).findFirst().orElseThrow().blocked());
        assertDoesNotThrow(()->next.purchaseLmsrShares("Owner",1,1,1));
    }
    private MarketWorld world(double buyerCash, CommissionMode mode) {
        var event = new WorldEvent(1, "Question", "Details", List.of("Yes", "No"), 5, mode,
                "Owner", new LmsrConfiguration(100), WorldEventStatus.ACTIVE, LmsrCalculator.totalCost(0,0,100));
        return new MarketWorld(Map.of(1,event), Map.of(
                "Owner",new MarketUser("Owner",10000,List.of(1)),
                "Buyer",new MarketUser("Buyer",buyerCash,List.of())));
    }
    @Test void purchaseCreditsBaseToContractFeeToOwnerAndDerivesHoldings() throws Exception {
        var before=world(100,CommissionMode.ON_PURCHASE);
        var preview=before.previewLmsrPurchase("Buyer",1,1,100,7);
        double base=LmsrCalculator.purchaseCost(0,0,100,100);
        assertEquals(base,preview.shareCost());
        assertEquals(base*.05,preview.commission());
        assertEquals(base*1.05,preview.totalDebit(),1e-12);
        assertEquals(7,preview.worldRevision());
        var candidate=before.purchaseLmsrShares("Buyer",1,1,100);
        var snapshot=candidate.snapshot();
        var buyer=snapshot.users().stream().filter(u->u.name().equals("Buyer")).findFirst().orElseThrow();
        var owner=snapshot.users().stream().filter(u->u.name().equals("Owner")).findFirst().orElseThrow();
        assertEquals(100-base-base*.05,buyer.currentBalance(),1e-12);
        assertEquals(10000+base*.05,owner.currentBalance());
        assertEquals(LmsrCalculator.totalCost(0,0,100)+base,snapshot.events().getFirst().contractBalance());
        assertEquals(List.of(1),buyer.participatingEventIds());
        assertEquals(100,buyer.positions().getFirst().quantityOne());
        assertEquals(0,buyer.positions().getFirst().quantityTwo());
        var trading=snapshot.events().getFirst().lmsrTrading().orElseThrow();
        assertEquals(100,trading.quantityOne());
        assertEquals(base*.05,trading.totalPurchaseCommission());
        assertEquals("Buyer",trading.newestFirstHistory().getFirst().userName());
        assertEquals(0,before.snapshot().events().getFirst().lmsrTrading().orElseThrow().quantityOne());
        assertThrows(UnsupportedOperationException.class,()->trading.newestFirstHistory().clear());
    }
    @Test void feeCanCreateNegativeBalanceThenEveryBuyerMutationIsBlocked() throws Exception {
        double base=LmsrCalculator.purchaseCost(0,0,100,100);
        var initial=world(base,CommissionMode.ON_PURCHASE);
        assertTrue(initial.previewLmsrPurchase("Buyer",1,1,100,1).becomesBlocked());
        var blocked=initial.purchaseLmsrShares("Buyer",1,1,100);
        var buyer=blocked.snapshot().users().stream().filter(u->u.name().equals("Buyer")).findFirst().orElseThrow();
        assertTrue(buyer.currentBalance()<0);
        assertTrue(buyer.blocked());
        var state=blocked.snapshot();
        assertEquals(WorldCommandException.Code.USER_BLOCKED,assertThrows(WorldCommandException.class,
                ()->blocked.purchaseLmsrShares("Buyer",1,2,1)).getCode());
        assertEquals(WorldCommandException.Code.USER_BLOCKED,assertThrows(WorldCommandException.class,
                ()->blocked.openLmsrEvent("Buyer",1)).getCode());
        assertEquals(state,blocked.snapshot());
    }
    @Test void onCloseCostsNoPurchaseFeeAndZeroDoesNotBlock() throws Exception {
        double base=LmsrCalculator.purchaseCost(0,0,100,100);
        var next=world(base,CommissionMode.ON_CLOSE).purchaseLmsrShares("Buyer",1,1,100);
        var buyer=next.snapshot().users().stream().filter(u->u.name().equals("Buyer")).findFirst().orElseThrow();
        assertEquals(0,buyer.currentBalance());
        assertFalse(buyer.blocked());
        assertEquals(0,next.snapshot().events().getFirst().lmsrTrading().orElseThrow().totalPurchaseCommission());
    }
    @Test void invalidInputsLeaveHistoryAndAllAccountsUnchanged() {
        var initial=world(100,CommissionMode.ON_PURCHASE);
        var before=initial.snapshot();
        for(int option:new int[]{0,3}) assertThrows(WorldCommandException.class,
                ()->initial.purchaseLmsrShares("Buyer",1,option,1));
        for(int quantity:new int[]{0,-1}) assertThrows(WorldCommandException.class,
                ()->initial.purchaseLmsrShares("Buyer",1,1,quantity));
        assertThrows(WorldCommandException.class,()->initial.purchaseLmsrShares("Missing",1,1,1));
        assertThrows(WorldCommandException.class,()->initial.purchaseLmsrShares("Buyer",2,1,1));
        assertEquals(before,initial.snapshot());
    }
    @Test void reachableQuantityOverflowAndUnderflowRejectWithoutPartialChanges() throws Exception {
        var event=new WorldEvent(1,"Boundary","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(1),WorldEventStatus.ACTIVE,LmsrCalculator.totalCost(0,0,1));
        var initial=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",100,List.of(1)),
                "Buyer",new MarketUser("Buyer",1.0e10,List.of())));
        var saturated=initial.purchaseLmsrShares("Buyer",1,1,Integer.MAX_VALUE);
        var before=saturated.snapshot();
        assertEquals(WorldCommandException.Code.INVALID_QUANTITY,assertThrows(WorldCommandException.class,
                ()->saturated.purchaseLmsrShares("Buyer",1,1,1)).getCode());
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                ()->saturated.purchaseLmsrShares("Buyer",1,2,1)).getCode());
        assertEquals(before,saturated.snapshot());
    }
}
