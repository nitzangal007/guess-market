package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReceiptRecoveryTest {
    private WorldEvent event(int id,int fee,CommissionMode mode) {
        return new WorldEvent(id,"Event "+id,"Details",List.of("Yes","No"),fee,mode,"Owner",
                new LmsrConfiguration(100),WorldEventStatus.ACTIVE,LmsrCalculator.totalCost(0,0,100));
    }
    private UserSnapshot user(MarketWorld world,String name) {
        return world.snapshot().users().stream().filter(u->u.name().equals(name)).findFirst().orElseThrow();
    }
    @Test void ownCommissionRecoversAndPreviewPredictsFinalEligibility() throws Exception {
        double base=LmsrCalculator.purchaseCost(0,0,100,100),fee=base*.05,cash=base+fee/2;
        var initial=new MarketWorld(Map.of(1,event(1,5,CommissionMode.ON_PURCHASE)),
                Map.of("Owner",new MarketUser("Owner",cash,List.of(1))));
        var preview=initial.previewLmsrPurchase("Owner",1,1,100,0);
        assertTrue(preview.balanceBefore()-preview.totalDebit()<0);
        assertTrue(preview.balanceAfter()>0);assertFalse(preview.becomesBlocked());
        var next=initial.purchaseLmsrShares("Owner",1,1,100);
        assertFalse(user(next,"Owner").blocked());
        assertDoesNotThrow(()->next.previewLmsrPurchase("Owner",1,2,1,0));
    }
    @Test void passiveCommissionRequiresStrictlyPositiveResultAndPositiveReceipt() throws Exception {
        double fee=LmsrCalculator.purchaseCost(0,0,100,100)*.05;
        for(double cash:new double[]{-fee-1,-fee,Math.nextUp(-fee)}) {
            var initial=new MarketWorld(Map.of(1,event(1,5,CommissionMode.ON_PURCHASE)),Map.of(
                    "Owner",new MarketUser("Owner",cash,List.of(1),true),"Buyer",new MarketUser("Buyer",1000,List.of())));
            var next=initial.purchaseLmsrShares("Buyer",1,1,100);var owner=user(next,"Owner");
            assertEquals(cash+fee,owner.currentBalance());
            assertEquals(owner.currentBalance()<=0,owner.blocked());
            assertDoesNotThrow(()->next.previewLmsrClose("Owner",1,1,0));
            if(owner.currentBalance()>0)assertDoesNotThrow(()->next.previewLmsrPurchase("Owner",1,2,1,0));
        }
        var zeroFee=new MarketWorld(Map.of(1,event(1,0,CommissionMode.ON_PURCHASE)),Map.of(
                "Owner",new MarketUser("Owner",1,List.of(1),true),"Buyer",new MarketUser("Buyer",1000,List.of())));
        assertTrue(user(zeroFee.purchaseLmsrShares("Buyer",1,1,1),"Owner").blocked());
    }
    @Test void winningPayoutRecoversOnlyAboveZeroAndAllowsNextEventAction() throws Exception {
        var paid=LmsrTradingState.empty().purchased("Buyer",1,10,5,0);
        var closing=new WorldEvent(1,"Closing","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(100),WorldEventStatus.ACTIVE,10,paid);
        for(double cash:new double[]{-11,-10,-9}) {
            var initial=new MarketWorld(Map.of(1,closing,2,event(2,0,CommissionMode.ON_PURCHASE)),Map.of(
                    "Owner",new MarketUser("Owner",-100,List.of(1,2),true),"Buyer",new MarketUser("Buyer",cash,List.of(),true)));
            var before=initial.snapshot();var next=initial.closeLmsrEvent("Owner",1,1);var buyer=user(next,"Buyer");
            assertEquals(cash+10,buyer.currentBalance());assertEquals(cash+10<=0,buyer.blocked());
            assertEquals(before,initial.snapshot());assertEquals(0,next.snapshot().events().stream().filter(e->e.id()==1).findFirst().orElseThrow().contractBalance());
            if(cash+10>0)assertDoesNotThrow(()->next.purchaseLmsrShares("Buyer",2,1,1));
            else assertThrows(WorldCommandException.class,()->next.previewLmsrPurchase("Buyer",2,1,1,0));
            assertTrue(user(next,"Owner").blocked());
        }
    }
    @Test void ordinaryOverdraftStaysBlockedWithoutReceiptAndOwnerStillCanClose() throws Exception {
        var initial=new MarketWorld(Map.of(1,event(1,0,CommissionMode.ON_PURCHASE)),Map.of(
                "Owner",new MarketUser("Owner",1,List.of(1)),"Buyer",new MarketUser("Buyer",1,List.of())));
        var next=initial.purchaseLmsrShares("Buyer",1,1,100);
        assertTrue(user(next,"Buyer").currentBalance()<0);assertTrue(user(next,"Buyer").blocked());
        assertThrows(WorldCommandException.class,()->next.previewLmsrPurchase("Buyer",1,2,1,0));
        assertDoesNotThrow(()->next.previewLmsrClose("Owner",1,1,0));
    }
}

