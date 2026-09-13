package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LmsrSettlementTest {
    @Test void genuineShortfallsAreNotReconciledEvenWhenSmall() {
        double replay=LmsrCalculator.totalCost(0,0,1);
        var history=new java.util.ArrayList<LmsrPurchaseRecord>();
        for(int i=0;i<12;i++){
            double base=LmsrCalculator.purchaseCost(i*3,0,3,1);
            replay+=base;history.add(new LmsrPurchaseRecord(i+1,"Buyer",1,3,base,0));
        }
        for(double missing:new double[]{0.01,Math.ulp(replay)}){
            var event=new WorldEvent(1,"Shortfall","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                    "Owner",new LmsrConfiguration(1),WorldEventStatus.ACTIVE,replay-missing,
                    new LmsrTradingState(36,0,0,history));
            var world=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",100,List.of(1)),
                    "Buyer",new MarketUser("Buyer",100,List.of())));
            var before=world.snapshot();
            assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                    ()->world.closeLmsrEvent("Owner",1,1)).getCode());
            assertEquals(before,world.snapshot());
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"1,12,Menash", "3,42,Avrum"})
    void reviewedPurchaseSequencesMustSettle(int b,int count,String buyer,@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var source=java.nio.file.Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml");
        var file=directory.resolve("rounding-small.xml");
        java.nio.file.Files.writeString(file,java.nio.file.Files.readString(source).replace("<b>100</b>","<b>"+b+"</b>")
                .replace("<commission type=\"on-purchase\">5</commission>","<commission type=\"on-purchase\">0</commission>"));
        var engine=new GuessMarketWorldEngineImpl();
        engine.loadWorldFromXml(file);
        var opening=engine.previewLmsrOpening("Tikva",1);
        engine.openLmsrEvent("Tikva",1,opening.worldRevision());
        for(int i=0;i<count;i++){
            var quote=engine.previewLmsrPurchase(buyer,1,1,3);
            engine.purchaseLmsrShares(buyer,1,1,3,quote.worldRevision());
        }
        var before=engine.getWorldSnapshot();var event=before.events().getFirst();
        java.math.BigDecimal exact=new java.math.BigDecimal(LmsrCalculator.totalCost(0,0,b));
        for(var trade:event.lmsrTrading().orElseThrow().newestFirstHistory())exact=exact.add(new java.math.BigDecimal(trade.shareCost()));
        System.out.println("R1 contract="+event.contractBalance()+", exact recorded sum="+exact+", payout="+(count*3));
        var quote=assertDoesNotThrow(()->engine.previewLmsrClose("Tikva",1,1));
        var closed=engine.closeLmsrEvent("Tikva",1,1,quote.worldRevision()).world();
        assertEquals(0,closed.events().getFirst().contractBalance());
        assertEquals(count*3,quote.totalGrossPayout());
        assertEquals(total(before),total(closed),Math.ulp(total(before)));
        var beforeBuyer=before.users().stream().filter(u->u.name().equals(buyer)).findFirst().orElseThrow();
        var afterBuyer=closed.users().stream().filter(u->u.name().equals(buyer)).findFirst().orElseThrow();
        assertEquals(beforeBuyer.currentBalance()+count*3,afterBuyer.currentBalance());
        var deficit=new java.math.BigDecimal(count*3).subtract(new java.math.BigDecimal(event.contractBalance()));
        assertTrue(deficit.compareTo(LmsrSettlementRounding.verify(reconstruct(event)).maximumAdjustment())<=0);
        assertEquals(0.0,quote.subsidyRefund());
        assertEquals(beforeBuyer.blocked(),afterBuyer.blocked());
    }
    private static WorldEvent reconstruct(EventSnapshot event) {
        var state=event.lmsrTrading().orElseThrow();
        var history=state.newestFirstHistory().reversed().stream().map(t->new LmsrPurchaseRecord(
                t.sequence(),t.userName(),t.optionNumber(),t.quantity(),t.shareCost(),t.commission())).toList();
        return new WorldEvent(event.id(),event.name(),event.description(),event.optionLabels(),event.commissionPercentage(),
                event.commissionMode(),event.marketMakerName(),event.pricing(),event.status(),event.contractBalance(),
                new LmsrTradingState(state.quantityOne(),state.quantityTwo(),state.totalPurchaseCommission(),history));
    }
    @Test void forgedQuoteAndCachedQuantitiesCannotAuthorizeReconciliation() {
        var history=new java.util.ArrayList<LmsrPurchaseRecord>();
        double replay=LmsrCalculator.totalCost(0,0,3);
        for(int i=0;i<42;i++) {
            double quote=LmsrCalculator.purchaseCost(i*3,0,3,3);
            if(i==0)quote=Math.nextDown(quote);
            history.add(new LmsrPurchaseRecord(i+1,"Buyer",1,3,quote,0));replay+=quote;
        }
        var forged=new WorldEvent(1,"Event","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(3),WorldEventStatus.ACTIVE,replay,new LmsrTradingState(126,0,0,history));
        assertThrows(ArithmeticException.class,()->LmsrSettlementRounding.verify(forged));
        var forgedWorld=new MarketWorld(Map.of(1,forged),Map.of("Owner",new MarketUser("Owner",100,List.of(1)),
                "Buyer",new MarketUser("Buyer",100,List.of())));
        var before=forgedWorld.snapshot();
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                ()->forgedWorld.closeLmsrEvent("Owner",1,1)).getCode());
        assertEquals(before,forgedWorld.snapshot());
        double first=LmsrCalculator.purchaseCost(0,0,3,3);
        history.set(0,new LmsrPurchaseRecord(1,"Buyer",1,3,first,0));
        var badQuantity=new WorldEvent(1,"Event","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                "Owner",new LmsrConfiguration(3),WorldEventStatus.ACTIVE,replay,new LmsrTradingState(127,0,0,history));
        assertThrows(ArithmeticException.class,()->LmsrSettlementRounding.verify(badQuantity));
    }
    @Test void boundedLegitimateSequenceMatrixSettlesBothOutcomes() throws Exception {
        int cases=0,trades=0,reconciliations=0,rejectedDebits=0,rejectedPrices=0;
        java.math.BigDecimal maxDeficit=java.math.BigDecimal.ZERO,maxBound=java.math.BigDecimal.ZERO;
        for(int b:new int[]{1,2,3,5,10,100,1000,Integer.MAX_VALUE})
        for(int step:new int[]{1,3,7})
        for(int pattern=0;pattern<5;pattern++)
        for(int count:new int[]{12,42,128,256})
        for(var mode:CommissionMode.values()) {
            var event=new WorldEvent(1,"Matrix","Details",List.of("Yes","No"),5,mode,"Owner",new LmsrConfiguration(b));
            var world=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",2_000_000_000,List.of(1)),
                    "Buyer",new MarketUser("Buyer",2_000_000_000,List.of()))).openLmsrEvent("Owner",1);
            var random=new java.util.Random(7381);
            for(int i=0;i<count;i++) {
                int option=switch(pattern) { case 0->1;case 1->2;case 2->i%2+1;case 3->(i/4)%2+1;default->random.nextInt(2)+1; };
                var checkpoint=world.snapshot();
                var quantities=checkpoint.events().getFirst().lmsrTrading().orElseThrow();
                double cost;
                try{cost=LmsrCalculator.purchaseCost(option==1?quantities.quantityOne():quantities.quantityTwo(),
                        option==1?quantities.quantityTwo():quantities.quantityOne(),step,b);}catch(ArithmeticException unrepresentablePrice){
                    final var unchanged=world;
                    assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                            ()->unchanged.purchaseLmsrShares("Buyer",1,option,step)).getCode());
                    assertEquals(checkpoint,world.snapshot());rejectedPrices++;continue;
                }
                double total=cost+new CommissionPolicy(mode,5).purchaseCommission(cost);
                double cash=checkpoint.users().stream().filter(u->u.name().equals("Buyer")).findFirst().orElseThrow().currentBalance();
                if(cash-total==cash){
                    // R6: a positive price is not payable if its gross account debit disappears.
                    final var unchanged=world;
                    assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,assertThrows(WorldCommandException.class,
                            ()->unchanged.purchaseLmsrShares("Buyer",1,option,step)).getCode());
                    assertEquals(checkpoint,world.snapshot());rejectedDebits++;
                }else{world=world.purchaseLmsrShares("Buyer",1,option,step);trades++;}
            }
            var before=world.snapshot();
            var active=reconstruct(before.events().getFirst());
            var proof=LmsrSettlementRounding.verify(active);
            maxBound=maxBound.max(proof.maximumAdjustment());
            for(int winner=1;winner<=2;winner++) {
                var preview=world.previewLmsrClose("Owner",1,winner,0);
                double gross=winner==1?active.trading().quantityOne():active.trading().quantityTwo();
                assertEquals(gross,preview.totalGrossPayout());
                var deficit=new java.math.BigDecimal(gross).subtract(new java.math.BigDecimal(active.contractBalance()));
                if(deficit.signum()>0) {
                    reconciliations++;maxDeficit=maxDeficit.max(deficit);
                    assertTrue(deficit.compareTo(proof.maximumAdjustment())<=0);
                    assertEquals(0,preview.subsidyRefund());
                }
                var after=world.closeLmsrEvent("Owner",1,winner).snapshot();
                assertEquals(before,world.snapshot());
                assertEquals(WorldEventStatus.CLOSED,after.events().getFirst().status());
                assertEquals(0,after.events().getFirst().contractBalance());
                // Exact sums avoid introducing a test-side summation error. Each of the
                // two credited accounts, net payout, fee/refund delta, and refund subtraction has
                // one correctly rounded addition/subtraction, bounded by one ulp.
                var accountAllowance=java.math.BigDecimal.ZERO;
                for(var user:after.users()) {
                    assertFalse(user.blocked());
                    accountAllowance=accountAllowance.add(new java.math.BigDecimal(Math.ulp(user.currentBalance())));
                }
                accountAllowance=accountAllowance.add(new java.math.BigDecimal(Math.ulp(preview.totalCommission()+preview.subsidyRefund())))
                        .add(new java.math.BigDecimal(Math.ulp(preview.subsidyRefund())));
                for(var payment:preview.payments())accountAllowance=accountAllowance
                        .add(new java.math.BigDecimal(Math.ulp(payment.netPayout())));
                var exactBefore=new java.math.BigDecimal(active.contractBalance());
                for(var user:before.users())exactBefore=exactBefore.add(new java.math.BigDecimal(user.currentBalance()));
                var exactAfter=java.math.BigDecimal.ZERO;
                for(var user:after.users())exactAfter=exactAfter.add(new java.math.BigDecimal(user.currentBalance()));
                assertTrue(exactAfter.subtract(exactBefore).abs().compareTo(
                        proof.maximumAdjustment().add(accountAllowance))<=0);
                cases++;
            }
        }
        System.out.println("R1 matrix: settlements="+cases+", trades="+trades+", reconciliations="+reconciliations
                +", rejected unrepresentable prices="+rejectedPrices+", rejected unrepresentable debits="+rejectedDebits+", maximum actual adjustment="+maxDeficit+", maximum computed allowance="+maxBound);
        assertEquals(1920,cases);assertEquals(105120,trades+rejectedDebits+rejectedPrices);assertTrue(rejectedDebits>0);
        assertTrue(reconciliations>0);
    }
    @Test void potentialBoundsCoverSymmetryAndIntegerBoundary() {
        var ln2=new java.math.BigDecimal("0.693147180559945309417232121458176568075500134360255254120680009");
        for(int q:new int[]{0,1,126,Integer.MAX_VALUE})for(int b:new int[]{1,3,Integer.MAX_VALUE}) {
            var interval=LmsrSettlementRounding.potential(q,q,b);
            var expected=ln2.multiply(java.math.BigDecimal.valueOf(b)).add(java.math.BigDecimal.valueOf(q));
            assertTrue(new java.math.BigDecimal(interval.lower()).compareTo(expected)<0);
            assertTrue(new java.math.BigDecimal(interval.upper()).compareTo(expected)>0);
        }
        var saturated=LmsrSettlementRounding.potential(Integer.MAX_VALUE,0,1);
        assertEquals((double)Integer.MAX_VALUE,saturated.lower());
        assertTrue(saturated.upper()>saturated.lower());
    }
    @Test void integerLimitSequencesRetainPayoutsAndBlocking() throws Exception {
        var maximumDeficit=java.math.BigDecimal.ZERO;
        int recoveredAccounts=0;
        for(int b:new int[]{1,3,100,Integer.MAX_VALUE}) {
            var event=new WorldEvent(1,"Boundary","Details",List.of("Yes","No"),0,CommissionMode.ON_PURCHASE,
                    "Owner",new LmsrConfiguration(b));
            var world=new MarketWorld(Map.of(1,event),Map.of("Owner",new MarketUser("Owner",2_000_000_000,List.of(1)),
                    "Buyer",new MarketUser("Buyer",2_000_000_000,List.of()),
                    "Other",new MarketUser("Other",2_000_000_000,List.of()))).openLmsrEvent("Owner",1)
                    .purchaseLmsrShares("Buyer",1,1,Integer.MAX_VALUE-7)
                    .purchaseLmsrShares("Other",1,1,7);
            for(int phase=0;phase<2;phase++) {
                var before=world.snapshot();
                var proof=LmsrSettlementRounding.verify(reconstruct(before.events().getFirst()));
                for(int winner=1;winner<=2;winner++) {
                    var preview=world.previewLmsrClose("Owner",1,winner,0);
                    assertEquals(winner==1||phase==1?(double)Integer.MAX_VALUE:0,preview.totalGrossPayout());
                    var deficit=new java.math.BigDecimal(preview.totalGrossPayout())
                            .subtract(new java.math.BigDecimal(before.events().getFirst().contractBalance()));
                    maximumDeficit=maximumDeficit.max(deficit);
                    assertTrue(deficit.compareTo(proof.maximumAdjustment())<=0);
                    var after=world.closeLmsrEvent("Owner",1,winner).snapshot();
                    assertEquals(0,after.events().getFirst().contractBalance());
                    var receipts=new java.util.LinkedHashMap<String,Double>();
                    for(var payment:preview.payments())receipts.merge(payment.userName(),payment.netPayout(),Double::sum);
                    receipts.merge("Owner",preview.totalCommission()+preview.subsidyRefund(),Double::sum);
                    for(var user:before.users()){
                        double receipt=receipts.getOrDefault(user.name(),0.0);
                        boolean expected=user.blocked()&&!(receipt>0&&user.currentBalance()+receipt>0);
                        var settled=after.users().stream().filter(u->u.name().equals(user.name())).findFirst().orElseThrow();
                        assertEquals(expected,settled.blocked());
                        if(user.blocked()&&!settled.blocked())recoveredAccounts++;
                    }
                }
                if(phase==0)world=world.purchaseLmsrShares("Other",1,2,Integer.MAX_VALUE);
            }
        }
        assertTrue(recoveredAccounts>0);
        System.out.println("R1 integer limits: settlements=16, recovered accounts="+recoveredAccounts+", maximum actual adjustment="+maximumDeficit);
    }
    @Test void publicCloseRejectsStalePreviewAndPublishesReceiptWithMatchingWorld() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();
        engine.loadWorldFromXml(java.nio.file.Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml"));
        var opening=engine.previewLmsrOpening("Tikva",1);
        engine.openLmsrEvent("Tikva",1,opening.worldRevision());
        var stale=engine.previewLmsrClose("Tikva",1,1);
        var buy=engine.previewLmsrPurchase("Menash",1,1,2);
        engine.purchaseLmsrShares("Menash",1,1,2,buy.worldRevision());
        var before=engine.getWorldSnapshot();
        assertEquals(WorldCommandException.Code.STALE_WORLD,assertThrows(WorldCommandException.class,
                ()->engine.closeLmsrEvent("Tikva",1,1,stale.worldRevision())).getCode());
        assertEquals(before,engine.getWorldSnapshot());
        var preview=engine.previewLmsrClose("Tikva",1,1);
        var result=engine.closeLmsrEvent("Tikva",1,1,preview.worldRevision());
        assertEquals(preview,result.settlement());
        assertEquals(result.world(),engine.getWorldSnapshot());
        assertThrows(WorldCommandException.class,()->engine.closeLmsrEvent("Tikva",1,1,preview.worldRevision()));
        assertEquals(result.world(),engine.getWorldSnapshot());
    }
    private MarketWorld world(CommissionMode mode) {
        return new MarketWorld(Map.of(1,new WorldEvent(1,"Event","Details",List.of("Yes","No"),5,mode,
                "Owner",new LmsrConfiguration(100),WorldEventStatus.ACTIVE,LmsrCalculator.totalCost(0,0,100))),
                Map.of("Owner",new MarketUser("Owner",1000,List.of(1)),
                       "Buyer",new MarketUser("Buyer",1,List.of()),
                       "Other",new MarketUser("Other",100,List.of())));
    }
    private double total(WorldSnapshot world) {
        return world.users().stream().mapToDouble(UserSnapshot::currentBalance).sum()
                +world.events().stream().mapToDouble(EventSnapshot::contractBalance).sum();
    }
    @Test void bothFeeModesPayAndRecoverBlockedWinnerAndTradingOwner() throws Exception {
        for(var mode:CommissionMode.values()) {
            var initial=world(mode);
            var purchased=initial.purchaseLmsrShares("Buyer",1,1,10)
                    .purchaseLmsrShares("Other",1,2,4)
                    .purchaseLmsrShares("Owner",1,1,3);
            var before=purchased.snapshot();
            var preview=purchased.previewLmsrClose("Owner",1,1,2);
            assertEquals(13,preview.totalGrossPayout());
            assertEquals(mode==CommissionMode.ON_CLOSE?.65:0,preview.totalCommission(),1e-12);
            assertEquals(before,purchased.snapshot());
            var closed=purchased.closeLmsrEvent("Owner",1,1);
            var after=closed.snapshot();
            assertEquals(WorldEventStatus.CLOSED,after.events().getFirst().status());
            assertEquals(0,after.events().getFirst().contractBalance());
            assertEquals(total(before),total(after),1e-10);
            var buyer=after.users().stream().filter(u->u.name().equals("Buyer")).findFirst().orElseThrow();
            assertTrue(buyer.currentBalance()>0);
            assertFalse(buyer.blocked());
            assertTrue(buyer.participatingEventIds().isEmpty());
            assertEquals(10,buyer.positions().getFirst().quantityOne());
            assertEquals(3,after.events().getFirst().lmsrTrading().orElseThrow().newestFirstHistory().size());
            assertEquals(1,after.events().getFirst().lmsrTrading().orElseThrow().settlement().orElseThrow().winningOption());
            assertThrows(WorldCommandException.class,()->closed.closeLmsrEvent("Owner",1,1));
            assertThrows(WorldCommandException.class,()->closed.purchaseLmsrShares("Other",1,1,1));
            assertThrows(WorldCommandException.class,()->closed.openLmsrEvent("Owner",1));
            assertEquals(after,closed.snapshot());
        }
    }
    @Test void emptyEventRefundsAllCashAndInvalidClosureIsAtomic() throws Exception {
        var initial=world(CommissionMode.ON_PURCHASE);
        var before=initial.snapshot();
        assertThrows(WorldCommandException.class,()->initial.closeLmsrEvent("Buyer",1,1));
        assertThrows(WorldCommandException.class,()->initial.closeLmsrEvent("Owner",1,0));
        assertEquals(before,initial.snapshot());
        var closed=initial.closeLmsrEvent("Owner",1,2).snapshot();
        assertEquals(0,closed.events().getFirst().contractBalance());
        assertEquals(total(before),total(closed),1e-10);
        assertEquals(LmsrCalculator.totalCost(0,0,100),
                closed.events().getFirst().lmsrTrading().orElseThrow().settlement().orElseThrow().subsidyRefund());
    }
}

