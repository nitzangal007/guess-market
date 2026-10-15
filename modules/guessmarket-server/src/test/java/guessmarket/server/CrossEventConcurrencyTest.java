package guessmarket.server;
import org.junit.jupiter.api.Test;
import guessmarket.dto.api.*;
import guessmarket.engine.xml.ex3.*;
import guessmarket.dto.world.*;
import guessmarket.dto.CommissionMode;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class CrossEventConcurrencyTest {
    @Test void concurrentFundsAndCanonicalLoginNeverLoseUpdates()throws Exception{
        var app=new MarketApplication();app.login("a","Dana");var gate=new CyclicBarrier(8);
        try(var pool=Executors.newFixedThreadPool(8)){
            var tasks=new ArrayList<Future<?>>();for(int i=0;i<8;i++)tasks.add(pool.submit(()->{gate.await();app.addFunds("a",new FundsRequest(UUID.randomUUID().toString(),"1"));return null;}));for(var f:tasks)f.get();
        }
        var snapshot=app.dashboard("a",null);assertEquals("8.0",snapshot.account().balance());assertEquals(8,snapshot.account().entries().size());
        app.addFunds("a",new FundsRequest(UUID.randomUUID().toString(),"1"));assertEquals("8.0",snapshot.account().balance());assertEquals(8,snapshot.account().entries().size());
    }
    @Test void racingBatchesPublishAtMostOneWithoutPartialLeftovers()throws Exception{
        var app=new MarketApplication();app.login("a","Dana");app.login("b","Bob");var gate=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var one=pool.submit(()->upload(app,gate,"a","one"));var two=pool.submit(()->upload(app,gate,"b","two"));assertEquals(1,one.get()+two.get());
        }
        assertEquals(2,app.dashboard("a",null).events().size());
    }
    @Test void logoutDuringParseAndFilenameChangeCannotAppend() {
        var app=new MarketApplication();app.login("a","Dana");var batch=batch("a");app.logout("a");
        assertEquals(401,assertThrows(ApiFailure.class,()->app.upload("a",UUID.randomUUID().toString(),"digest","a.xml",batch)).status());
        app.login("b","DANA");String id=UUID.randomUUID().toString();var first=app.upload("b",id,"digest","a.xml",batch);assertEquals(first,app.upload("b",id,"digest","a.xml",batch));
        assertEquals("REQUEST_ID_REUSED",assertThrows(ApiFailure.class,()->app.upload("b",id,"digest","b.xml",batch)).error().code());
    }
    @Test void tradeCloseRaceHasOneCommitAndNoPostCloseFill()throws Exception {
        var app=new MarketApplication();app.login("owner","Dana");app.login("buyer","Bob");
        app.addFunds("owner",new FundsRequest(UUID.randomUUID().toString(),"100"));app.addFunds("buyer",new FundsRequest(UUID.randomUUID().toString(),"100"));
        app.upload("owner",UUID.randomUUID().toString(),"digest","a.xml",batch("a"));
        var open=new ActionRequest(ActionKind.OPEN,null,null,null,null);
        app.execute("owner",1,new CommandRequest(UUID.randomUUID().toString(),app.revision(),open));
        long revision=app.revision();var barrier=new CyclicBarrier(2);
        var trade=new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".5");var close=new ActionRequest(ActionKind.CLOSE,1,null,null,null);
        try(var pool=Executors.newFixedThreadPool(2)){
            var one=pool.submit(()->raceCommand(app,barrier,"buyer",trade,revision));var two=pool.submit(()->raceCommand(app,barrier,"owner",close,revision));assertEquals(1,one.get()+two.get());
        }
        var view=app.dashboard("owner",1);
        assertEquals(revision+1,view.revision());
        if(view.selectedEvent().summary().status().equals("ACTIVE"))app.execute("owner",1,new CommandRequest(UUID.randomUUID().toString(),app.revision(),close));
        assertEquals("WRONG_STATUS",assertThrows(ApiFailure.class,()->app.execute("buyer",1,new CommandRequest(UUID.randomUUID().toString(),app.revision(),trade))).error().code());
    }
    private static int raceCommand(MarketApplication app,CyclicBarrier barrier,String session,ActionRequest action,long revision)throws Exception{barrier.await();try{app.execute(session,1,new CommandRequest(UUID.randomUUID().toString(),revision,action));return 1;}catch(ApiFailure problem){assertEquals("STALE_WORLD",problem.error().code());return 0;}}
    private static int upload(MarketApplication app,CyclicBarrier gate,String session,String name)throws Exception{gate.await();try{app.upload(session,UUID.randomUUID().toString(),name,"sample.xml",batch(name,"common"));return 1;}catch(ApiFailure problem){assertEquals("DUPLICATE_EVENT_NAME",problem.error().code());return 0;}}
    private static ParsedEventBatch batch(String...names){return new ParsedEventBatch(Arrays.stream(names).map(name->new EventDefinition(name,"",List.of("",""),0,CommissionMode.ON_CLOSE,new OrderBookConfiguration(0,1,true))).toList());}
}
