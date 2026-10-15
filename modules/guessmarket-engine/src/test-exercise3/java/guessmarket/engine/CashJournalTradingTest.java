package guessmarket.engine;
import guessmarket.dto.api.*;
import guessmarket.dto.world.*;
import guessmarket.dto.CommissionMode;
import guessmarket.engine.xml.ex3.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CashJournalTradingTest {
    @Test void orderBookExactTradeMintCloseAndJournalMatchOriginalDomain()throws Exception {
        var engine=engine(new OrderBookConfiguration(100,1,true));
        var legacy=new MarketWorld(Map.of(1,new WorldEvent(1,"event","",List.of("Red","Blue"),5,CommissionMode.ON_PURCHASE,"Dana",new OrderBookConfiguration(100,1,true))),Map.of("Dana",new MarketUser("Dana",1000,List.of(1)),"Bob",new MarketUser("Bob",100,List.of())));
        legacy=legacy.openOrderBookEvent("Dana",1);command(engine,"Dana",new ActionRequest(ActionKind.OPEN,null,null,null,null));
        ActionRequest sell=new ActionRequest(ActionKind.ORDER,1,1,OrderSide.SELL,"0.425");
        legacy=legacy.planOrder(new OrderRequest("Dana",1,1,OrderSide.SELL,1,new BigDecimal(".425")),engine.revision()).world();command(engine,"Dana",sell);
        ActionRequest buy=new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".425");
        legacy=legacy.planOrder(new OrderRequest("Bob",1,1,OrderSide.BUY,1,new BigDecimal(".425")),engine.revision()).world();
        var receipt=command(engine,"Bob",buy);assertEquals("99.55375",receipt.balanceAfter());assertEquals("0.425",receipt.confirmedReview().executions().getFirst().principal());
        assertEquals(List.of("SHARE_PURCHASE","COMMISSION_PAID"),receipt.ownTransactionEntries().stream().map(AccountEntryDto::kind).toList());
        assertEquals(Double.toString(legacy.users().get("Dana").currentBalance()),engine.dashboard("Dana",null).account().balance());
        assertEquals(Double.toString(legacy.users().get("Bob").currentBalance()),engine.dashboard("Bob",null).account().balance());
        command(engine,"Dana",new ActionRequest(ActionKind.ORDER,2,2,OrderSide.BUY,".6"));
        var minted=command(engine,"Bob",new ActionRequest(ActionKind.ORDER,1,2,OrderSide.BUY,".5"));assertEquals(2,minted.confirmedReview().executions().size());assertTrue(minted.confirmedReview().executions().stream().allMatch(t->t.counterpartyRole()==CounterpartyRole.OPPOSITE_BUYER));
        var close=command(engine,"Dana",new ActionRequest(ActionKind.CLOSE,1,null,null,null));assertFalse(close.confirmedReview().payouts().isEmpty());
        assertEquals("CLOSED",engine.dashboard("Dana",1).selectedEvent().summary().status());
        for(String actor:List.of("Dana","Bob"))reconcile(engine.dashboard(actor,null).account());
        assertEquals(1,receipt.confirmedReview().executions().size());assertEquals(buy,receipt.confirmedAction());
    }
    @Test void lmsrSelfCommissionBlockedRecoveryAndSettlementPreserveDomainOrder()throws Exception {
        var engine=engine(new LmsrConfiguration(100));command(engine,"Dana",new ActionRequest(ActionKind.OPEN,null,null,null,null));
        var preview=engine.preview("Dana",1,new ActionRequest(ActionKind.LMSR_BUY,2,10,null,null));
        var before=engine.dashboard("Dana",null);assertEquals(before,engine.dashboard("Dana",null));
        var receipt=command(engine,"Dana",preview.action());assertEquals(preview.balanceAfter(),receipt.balanceAfter());
        assertEquals(List.of("SHARE_PURCHASE","COMMISSION_PAID","COMMISSION_RECEIVED"),receipt.ownTransactionEntries().stream().filter(e->!e.kind().equals("REPRESENTATION_ADJUSTMENT")).map(AccountEntryDto::kind).toList());
        command(engine,"Bob",new ActionRequest(ActionKind.LMSR_BUY,1,1000,null,null));assertTrue(engine.dashboard("Bob",null).account().blocked());
        double debt=-Double.parseDouble(engine.dashboard("Bob",null).account().balance());engine.addFunds("Bob",Double.toString(debt),UUID.randomUUID().toString());assertTrue(engine.dashboard("Bob",null).account().blocked());engine.addFunds("Bob","1",UUID.randomUUID().toString());assertFalse(engine.dashboard("Bob",null).account().blocked());
        command(engine,"Dana",new ActionRequest(ActionKind.CLOSE,1,null,null,null));reconcile(engine.dashboard("Dana",null).account());reconcile(engine.dashboard("Bob",null).account());
    }
    @Test void staleAndJournalPreparationFailurePublishNothing()throws Exception {
        var engine=engine(new OrderBookConfiguration(100,1,true));var before=engine.dashboard("Dana",null);
        assertThrows(WorldCommandException.class,()->engine.execute("Dana",1,new CommandRequest(UUID.randomUUID().toString(),0,new ActionRequest(ActionKind.OPEN,null,null,null,null))));assertEquals(before,engine.dashboard("Dana",null));
        var field=SharedMarketEngineImpl.class.getDeclaredField("journal");field.setAccessible(true);field.set(engine,new AccountJournal(Long.MAX_VALUE));before=engine.dashboard("Dana",null);
        assertThrows(WorldCommandException.class,()->command(engine,"Dana",new ActionRequest(ActionKind.OPEN,null,null,null,null)));assertEquals(before,engine.dashboard("Dana",null));
    }
    static SharedMarketEngineImpl engine(PricingConfiguration pricing)throws Exception {
        var e=new SharedMarketEngineImpl("test");e.ensureAccount("Dana");e.ensureAccount("Bob");e.addFunds("Dana","1000",UUID.randomUUID().toString());e.addFunds("Bob","100",UUID.randomUUID().toString());e.appendEvents("Dana",new ParsedEventBatch(List.of(new EventDefinition("event","",List.of("Red","Blue"),5,CommissionMode.ON_PURCHASE,pricing))),UUID.randomUUID().toString());return e;
    }
    static CommandResponse command(SharedMarketEngineImpl e,String actor,ActionRequest action)throws Exception{return e.execute(actor,1,new CommandRequest(UUID.randomUUID().toString(),e.revision(),action));}
    static void reconcile(AccountDto account){var groups=new LinkedHashMap<String,List<AccountEntryDto>>();account.entries().forEach(row->groups.computeIfAbsent(row.transactionId(),k->new ArrayList<>()).add(row));BigDecimal running=BigDecimal.ZERO;for(var rows:groups.values()){for(var row:rows){running=running.add(new BigDecimal(row.amount()));assertEquals(0,running.compareTo(new BigDecimal(row.journalRunningBalance())));}assertEquals(0,running.compareTo(new BigDecimal(rows.getLast().storedBalanceAfterTransaction())));}assertEquals(0,running.compareTo(new BigDecimal(account.balance())));}
}
