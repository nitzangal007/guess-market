package guessmarket.client;
import guessmarket.client.http.*;
import guessmarket.client.session.*;
import com.sun.net.httpserver.HttpServer;
import guessmarket.dto.api.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
class PollCoordinatorTest {
    @Test void blockedReadKeepsOnePollInFlightAndCapturesSelection()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var received=new CountDownLatch(2);var active=new AtomicInteger();var maximum=new AtomicInteger();
        server.createContext("/api/dashboard",exchange->{int n=active.incrementAndGet();maximum.accumulateAndGet(n,Math::max);entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException problem){Thread.currentThread().interrupt();}int id=Integer.parseInt(exchange.getRequestURI().getQuery().split("=")[1]);var summary=new EventSummaryDto(id,"event","","ORDER_BOOK","ACTIVE","Dana",0,"ON_CLOSE","0");var detail=new EventDetailDto(summary,List.of("Red","Blue"),null,new BookDetailDto(1,0,false,0,List.of(),List.of(),List.of(),List.of()),null,null,false,true,true);var dashboard=new DashboardResponse("s",1,List.of(summary),List.of(),new AccountDto("Dana","0.0",false,List.of(),List.of(),List.of()),detail);byte[] body=new com.google.gson.Gson().toJson(dashboard).getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);try(var output=exchange.getResponseBody()){output.write(body);}active.decrementAndGet();});server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
        try(var http=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){
            var lastSelection=new AtomicLong();try(var poll=new PollCoordinator(http,(q,r)->{lastSelection.set(q.selection());received.countDown();},(q,e)->fail(e))){poll.selectEvent(1,1);poll.start();assertTrue(entered.await(5,TimeUnit.SECONDS));poll.selectEvent(2,2);poll.requestNow();poll.requestNow();release.countDown();assertTrue(received.await(5,TimeUnit.SECONDS));assertEquals(1,maximum.get());assertEquals(2,lastSelection.get());}
        }finally{release.countDown();server.stop(0);}
    }
}
