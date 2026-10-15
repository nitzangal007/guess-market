package guessmarket.server;
import guessmarket.dto.api.*;
import guessmarket.dto.world.*;
import guessmarket.dto.CommissionMode;
import guessmarket.engine.xml.ex3.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TransactionReceiptTest {
    @Test void crossActorSameUuidNeverCopiesPriorSaleCommissionOrPayoutIntoReceipt(){
        var app=new MarketApplication();app.login("a","Dana");app.login("b","Bob");
        for(String session:List.of("a","b"))app.addFunds(session,new FundsRequest(UUID.randomUUID().toString(),"100"));
        var definition=new EventDefinition("event","",List.of("Red","Blue"),5,CommissionMode.ON_PURCHASE,new OrderBookConfiguration(10,1,true));app.upload("a",UUID.randomUUID().toString(),"digest","tx.xml",new ParsedEventBatch(List.of(definition)));
        command(app,"a",new ActionRequest(ActionKind.OPEN,null,null,null,null),UUID.randomUUID().toString());command(app,"a",new ActionRequest(ActionKind.ORDER,1,1,OrderSide.SELL,".5"),UUID.randomUUID().toString());
        String same=UUID.randomUUID().toString();var trade=command(app,"b",new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".5"),same);
        var ownerEntries=app.dashboard("a",null).account().entries();String tradeTransaction=trade.ownTransactionEntries().getFirst().transactionId();assertTrue(ownerEntries.subList(ownerEntries.size()-2,ownerEntries.size()).stream().allMatch(e->e.transactionId().equals(tradeTransaction)));
        var request=new FundsRequest(same,"1");var funds=app.addFunds("a",request);assertEquals(1,funds.ownTransactionEntries().size());assertEquals("FUND_LOAD",funds.ownTransactionEntries().getFirst().kind());assertNotEquals(tradeTransaction,funds.ownTransactionEntries().getFirst().transactionId());
        int size=app.dashboard("a",null).account().entries().size();assertEquals(funds,app.addFunds("a",request));assertEquals(size,app.dashboard("a",null).account().entries().size());
        String closeId=UUID.randomUUID().toString();var close=command(app,"a",new ActionRequest(ActionKind.CLOSE,1,null,null,null),closeId);var bobRows=app.dashboard("b",null).account().entries();var payout=bobRows.getLast();assertEquals("WINNING_GROSS",payout.kind());assertEquals(close.ownTransactionEntries().getFirst().transactionId(),payout.transactionId());
        var credit=app.addFunds("b",new FundsRequest(closeId,"1"));assertEquals(1,credit.ownTransactionEntries().size());assertNotEquals(payout.transactionId(),credit.ownTransactionEntries().getFirst().transactionId());assertEquals(trade,app.commandStatus("b",same).command());
    }
    private static CommandResponse command(MarketApplication app,String session,ActionRequest action,String id){return app.execute(session,1,new CommandRequest(id,app.revision(),action));}
}
