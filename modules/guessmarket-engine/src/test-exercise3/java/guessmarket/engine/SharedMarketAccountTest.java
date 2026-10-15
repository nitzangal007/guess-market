package guessmarket.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.UUID;

class SharedMarketAccountTest {
    @Test void emptyAccountAndFundsPublishTogether() throws Exception {
        var engine=new SharedMarketEngineImpl("test");
        engine.ensureAccount("Dana");
        var empty=engine.dashboard("Dana",null);
        assertEquals("0.0",empty.account().balance());
        assertTrue(empty.events().isEmpty());
        var receipt=engine.addFunds("Dana","100.5",UUID.randomUUID().toString());
        assertEquals("100.5",receipt.balanceAfter());
        assertEquals("FUND_LOAD",receipt.ownTransactionEntries().getFirst().kind());
        var before=engine.dashboard("Dana",null);
        for(String amount:new String[]{"0","-1","NaN","Infinity","1e400","bad"}) {
            assertThrows(WorldCommandException.class,()->engine.addFunds("Dana",amount,UUID.randomUUID().toString()));
            assertEquals(before,engine.dashboard("Dana",null));
        }
        assertEquals("0.0",empty.account().balance());
    }
    @Test void unrepresentedCreditAndJournalPreparationFailureAreAtomic() throws Exception {
        var engine=new SharedMarketEngineImpl("test");engine.ensureAccount("Dana");
        engine.addFunds("Dana","1e100",UUID.randomUUID().toString());
        var before=engine.dashboard("Dana",null);
        assertThrows(WorldCommandException.class,()->engine.addFunds("Dana","1",UUID.randomUUID().toString()));
        assertEquals(before,engine.dashboard("Dana",null));
        var journal=new AccountJournal(Long.MAX_VALUE);
        var component=new CashComponent("Dana","FUND_LOAD",null,null,null,BigDecimal.ONE);
        assertThrows(ArithmeticException.class,()->journal.prepare(java.util.List.of(
                new AccountCashEffect("Dana",0,1,java.util.List.of(component))),"tx"));
        assertTrue(journal.entries("Dana").isEmpty());
    }
    @Test void representedJournalReconcilesExactly() {
        var journal=new AccountJournal();
        var effects=java.util.List.of(new AccountCashEffect("Dana",100,99.55375,java.util.List.of(
            new CashComponent("Dana","SHARE_PURCHASE",1,"event",1,new BigDecimal("-0.425")),
            new CashComponent("Dana","COMMISSION_PAID",1,"event",1,new BigDecimal("-0.02125")))));
        var next=journal.prepare(effects,"tx");
        assertEquals(2,next.entries("Dana").size());
        assertEquals(0,new BigDecimal("99.55375").compareTo(new BigDecimal(next.entries("Dana").getLast().journalRunningBalance())));
        assertTrue(journal.entries("Dana").isEmpty());
    }
}
