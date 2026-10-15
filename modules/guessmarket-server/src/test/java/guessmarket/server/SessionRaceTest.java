package guessmarket.server;
import org.junit.jupiter.api.Test;
import guessmarket.dto.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class SessionRaceTest {
    @Test void canonicalNameHasOneConcurrentOwnerAndRetainsAccount() throws Exception {
        var app=new MarketApplication();var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var futures=List.of(pool.submit(()->login(app,barrier,"a","Dana")),pool.submit(()->login(app,barrier,"b","dAnA")));
            var results=Arrays.asList(futures.get(0).get(),futures.get(1).get());
            assertEquals(1,results.stream().filter(Objects::nonNull).count());
            String session=results.get(0)!=null?"a":"b";
            String display=app.dashboard(session,null).account().username();
            String id=UUID.randomUUID().toString();
            var receipt=app.addFunds(session,new FundsRequest(id,"10"));
            app.logout(session);
            assertThrows(ApiFailure.class,()->app.login(session,"Dana"));
            app.login("c","DANA");app.logout(session);
            assertEquals(display,app.dashboard("c",null).account().username());
            assertEquals(receipt,app.commandStatus("c",id).command());
            assertEquals(1,app.dashboard("c",null).users().size());
        }
    }
    private static LoginResponse login(MarketApplication app,CyclicBarrier barrier,String session,String name)throws Exception {
        barrier.await();try{return app.login(session,name);}catch(ApiFailure failure){assertEquals("USER_ALREADY_CONNECTED",failure.error().code());return null;}
    }
    @Test void oneCookieCannotBindTwoActors() throws Exception {
        var app=new MarketApplication();app.login("a","Dana");
        assertThrows(ApiFailure.class,()->app.login("a","Bob"));
        app.logout("a");assertThrows(ApiFailure.class,()->app.addFunds("a",new FundsRequest(UUID.randomUUID().toString(),"100")));
    }
    @Test void receiptsDeduplicateAndRejectChangedPayloadAndCrossActor() {
        var app=new MarketApplication();app.login("a","Dana");app.login("b","Bob");
        String id=UUID.randomUUID().toString();var request=new FundsRequest(id,"100.5");
        var first=app.addFunds("a",request);assertEquals(first,app.addFunds("a",request));
        assertEquals("100.5",app.dashboard("a",null).account().balance());
        assertEquals("REQUEST_ID_REUSED",assertThrows(ApiFailure.class,()->app.addFunds("a",new FundsRequest(id,"2"))).error().code());
        assertEquals(404,assertThrows(ApiFailure.class,()->app.commandStatus("b",id)).status());
    }
}
