package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OrderBookOpeningTest {
    private static final Path SMALL=Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml");
    private MarketWorld world(int initial,int d,double cash,boolean blocked,WorldEventStatus status) {
        var event=new WorldEvent(2,"Book","Details",List.of("Yes","No"),90,CommissionMode.ON_PURCHASE,
                "Owner",new OrderBookConfiguration(initial,d,true),status,0);
        return new MarketWorld(Map.of(2,event),Map.of("Owner",new MarketUser("Owner",cash,List.of(2),blocked)));
    }
    @Test void realPreviewAndOpeningTransferFundingAndCreateImmutablePairs() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();
        var before=engine.loadWorldFromXml(SMALL);
        var preview=engine.previewOrderBookOpening("Avrum",2);
        assertEquals(100,preview.requiredFunding());
        assertEquals(1000,preview.currentBalance());
        assertEquals(900,preview.balanceAfterOpening());
        assertEquals(before,engine.getWorldSnapshot());
        var after=engine.openOrderBookEvent("Avrum",2,preview.worldRevision());
        var event=after.events().stream().filter(e->e.id()==2).findFirst().orElseThrow();
        var state=event.orderBook().orElseThrow();
        assertEquals(WorldEventStatus.ACTIVE,event.status());
        assertEquals(100,event.contractBalance());
        assertEquals(100,state.issuedPairs());
        assertEquals(1,state.holdings().size());
        assertEquals("Avrum",state.holdings().getFirst().userName());
        assertEquals(100,state.holdings().getFirst().optionOneShares());
        assertEquals(100,state.holdings().getFirst().optionTwoShares());
        assertEquals(900,after.users().getFirst().currentBalance());
        assertEquals(1000,after.users().getFirst().currentBalance()+event.contractBalance());
        assertEquals(before.events().getFirst(),after.events().getFirst());
        assertEquals(before.users().get(1),after.users().get(1));
        assertEquals(before.users().get(2),after.users().get(2));
        assertEquals(0,before.events().get(1).orderBook().orElseThrow().issuedPairs());
        assertThrows(UnsupportedOperationException.class,()->state.holdings().clear());
        assertTrue(event.lmsrTrading().isEmpty());
    }
    @Test void denominatorAndZeroFundingHaveExactIntegerInventory() throws Exception {
        for(int[] values:List.of(new int[]{100,5,20},new int[]{0,3,0},new int[]{Integer.MAX_VALUE,1,Integer.MAX_VALUE})) {
            var initial=world(values[0],values[1],values[0],false,WorldEventStatus.NOT_STARTED);
            var before=initial.snapshot();
            var after=initial.openOrderBookEvent("Owner",2).snapshot();
            assertEquals(0,after.users().getFirst().currentBalance());
            assertEquals(values[0],after.events().getFirst().contractBalance());
            var book=after.events().getFirst().orderBook().orElseThrow();
            assertEquals(values[2],book.issuedPairs());
            assertEquals(values[2],book.holdings().stream().mapToLong(OrderBookHolding::optionOneShares).sum());
            assertEquals(values[2],book.holdings().stream().mapToLong(OrderBookHolding::optionTwoShares).sum());
            assertEquals(before,initial.snapshot());
        }
    }
    @Test void insufficientBlockedAndClosedRejectWithoutMutation() throws Exception {
        for(var entry:List.of(
                Map.entry(world(100,1,Math.nextDown(100.0),false,WorldEventStatus.NOT_STARTED),WorldCommandException.Code.INSUFFICIENT_FUNDS),
                Map.entry(world(100,1,1000,true,WorldEventStatus.NOT_STARTED),WorldCommandException.Code.USER_BLOCKED),
                Map.entry(world(0,1,1000,true,WorldEventStatus.NOT_STARTED),WorldCommandException.Code.USER_BLOCKED),
                Map.entry(world(100,1,1000,false,WorldEventStatus.CLOSED),WorldCommandException.Code.WRONG_STATUS))) {
            var before=entry.getKey().snapshot();
            assertEquals(entry.getValue(),assertThrows(WorldCommandException.class,
                    ()->entry.getKey().openOrderBookEvent("Owner",2)).getCode());
            assertEquals(before,entry.getKey().snapshot());
        }
    }
    @Test void wrongIdentityMethodAndRevisionCannotPublish() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();var before=engine.loadWorldFromXml(SMALL);
        var preview=engine.previewOrderBookOpening("Avrum",2);
        assertEquals(WorldCommandException.Code.NOT_OWNER,assertThrows(WorldCommandException.class,
                ()->engine.openOrderBookEvent("Menash",2,preview.worldRevision())).getCode());
        assertEquals(WorldCommandException.Code.USER_NOT_FOUND,assertThrows(WorldCommandException.class,
                ()->engine.previewOrderBookOpening("missing",2)).getCode());
        assertEquals(WorldCommandException.Code.EVENT_NOT_FOUND,assertThrows(WorldCommandException.class,
                ()->engine.previewOrderBookOpening("Avrum",999)).getCode());
        assertEquals(WorldCommandException.Code.WRONG_METHOD,assertThrows(WorldCommandException.class,
                ()->engine.previewOrderBookOpening("Tikva",1)).getCode());
        assertEquals(before,engine.getWorldSnapshot());
        engine.loadWorldFromXml(SMALL);
        assertEquals(WorldCommandException.Code.STALE_WORLD,assertThrows(WorldCommandException.class,
                ()->engine.openOrderBookEvent("Avrum",2,preview.worldRevision())).getCode());
        assertEquals(before,engine.getWorldSnapshot());
    }
    @Test void openingCannotRepeatAndInvalidCommandDoesNotConsumePreview() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(SMALL);
        var preview=engine.previewOrderBookOpening("Avrum",2);
        assertThrows(WorldCommandException.class,()->engine.openOrderBookEvent("Menash",2,preview.worldRevision()));
        var after=engine.openOrderBookEvent("Avrum",2,preview.worldRevision());
        assertEquals(WorldCommandException.Code.STALE_WORLD,assertThrows(WorldCommandException.class,
                ()->engine.openOrderBookEvent("Avrum",2,preview.worldRevision())).getCode());
        assertEquals(WorldCommandException.Code.WRONG_STATUS,assertThrows(WorldCommandException.class,
                ()->engine.previewOrderBookOpening("Avrum",2)).getCode());
        assertEquals(after,engine.getWorldSnapshot());
    }
    @Test void pendingNondivisiblePolicyIsNotXmlValidationOrRounding(@org.junit.jupiter.api.io.TempDir Path temp) throws Exception {
        var pending=world(10,3,1000,false,WorldEventStatus.NOT_STARTED);var before=pending.snapshot();
        assertEquals(WorldCommandException.Code.INVALID_QUANTITY,assertThrows(WorldCommandException.class,
                ()->pending.openOrderBookEvent("Owner",2)).getCode());
        assertEquals(before,pending.snapshot());
        String xml=java.nio.file.Files.readString(SMALL);
        String changed=xml.replace("initial=\"100\" d=\"1\"","initial=\"10\" d=\"3\"");
        assertNotEquals(xml,changed);
        Path fixture=java.nio.file.Files.writeString(temp.resolve("nondivisible.xml"),changed);
        var engine=new GuessMarketWorldEngineImpl();var loaded=engine.loadWorldFromXml(fixture);
        assertEquals(WorldCommandException.Code.INVALID_QUANTITY,assertThrows(WorldCommandException.class,
                ()->engine.previewOrderBookOpening("Avrum",2)).getCode());
        assertEquals(loaded,engine.getWorldSnapshot());
    }
    @Test void existingLmsrCommandsPreserveOpenedOrderBookInventory() throws Exception {
        var engine=new GuessMarketWorldEngineImpl();engine.loadWorldFromXml(SMALL);
        var opening=engine.previewOrderBookOpening("Avrum",2);
        var after=engine.openOrderBookEvent("Avrum",2,opening.worldRevision());
        var book=after.events().get(1);
        var lmsr=engine.previewLmsrOpening("Tikva",1);
        engine.openLmsrEvent("Tikva",1,lmsr.worldRevision());
        var purchase=engine.previewLmsrPurchase("Menash",1,1,2);
        engine.purchaseLmsrShares("Menash",1,1,2,purchase.worldRevision());
        var close=engine.previewLmsrClose("Tikva",1,1);
        engine.closeLmsrEvent("Tikva",1,1,close.worldRevision());
        assertEquals(book,engine.getWorldSnapshot().events().get(1));
        assertEquals(after.users().getFirst(),engine.getWorldSnapshot().users().getFirst());
    }
    @Test void noLoadedWorldRejectsBothOperations() {
        var engine=new GuessMarketWorldEngineImpl();
        assertThrows(EngineOperationException.class,()->engine.previewOrderBookOpening("Owner",2));
        assertThrows(EngineOperationException.class,()->engine.openOrderBookEvent("Owner",2,0));
    }
    @Test void unrepresentablePositiveOpeningDebitRejectsWithoutPublishing(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        String first="<GM-event name=\"Large\"><id>1</id><description>Boundary</description><commission type=\"on-purchase\">0</commission>"
                +"<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method>"
                +"<GM-order-book initial=\"0\" d=\"2147483647\" allow-mint=\"true\"/></GM-method></GM-event>";
        String second="<GM-event name=\"Small\"><id>2</id><description>Boundary</description><commission type=\"on-purchase\">0</commission>"
                +"<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method>"
                +"<GM-order-book initial=\"1\" d=\"1\" allow-mint=\"false\"/></GM-method></GM-event>";
        String third="<GM-event name=\"Revision probe\"><id>3</id><description>Boundary</description><commission type=\"on-purchase\">0</commission>"
                +"<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method>"
                +"<GM-order-book initial=\"0\" d=\"1\" allow-mint=\"false\"/></GM-method></GM-event>";
        String xml="<Guess-Market><GM-events>"+first+second+third+"</GM-events><GM-users>"
                +"<GM-user name=\"Owner\"><initial-cash>1000</initial-cash><GM-market-maker><event id=\"1\"/><event id=\"2\"/><event id=\"3\"/></GM-market-maker></GM-user>"
                +"<GM-user name=\"A\"><initial-cash>1000</initial-cash></GM-user></GM-users></Guess-Market>";
        var engine=new GuessMarketWorldEngineImpl();
        engine.loadWorldFromXml(java.nio.file.Files.writeString(temp.resolve("large.xml"),xml));
        var open=engine.previewOrderBookOpening("Owner",1);
        engine.openOrderBookEvent("Owner",1,open.worldRevision());
        var probeOpening=engine.previewOrderBookOpening("Owner",3);
        engine.openOrderBookEvent("Owner",3,probeOpening.worldRevision());
        var resting=new OrderRequest("A",1,2,OrderSide.BUY,Integer.MAX_VALUE,new BigDecimal("1073741823"));
        engine.submitOrder(resting,engine.previewOrder(resting).worldRevision());
        var incoming=new OrderRequest("Owner",1,1,OrderSide.BUY,Integer.MAX_VALUE,new BigDecimal("1073741824"));
        engine.submitOrder(incoming,engine.previewOrder(incoming).worldRevision());
        var close=engine.previewOrderBookClose("Owner",1,1);
        engine.closeOrderBookEvent("Owner",1,1,close.worldRevision());

        var before=engine.getWorldSnapshot();
        var safePreview=new OrderRequest("Owner",3,1,OrderSide.BUY,1,BigDecimal.ZERO);
        long revision=engine.previewOrder(safePreview).worldRevision();
        var failure=assertThrows(WorldCommandException.class,()->engine.previewOrderBookOpening("Owner",2));
        assertEquals(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,failure.getCode());
        assertTrue(failure.getMessage().contains("represented"));
        assertEquals(before,engine.getWorldSnapshot());
        assertEquals(revision,engine.previewOrder(safePreview).worldRevision());
    }
}

