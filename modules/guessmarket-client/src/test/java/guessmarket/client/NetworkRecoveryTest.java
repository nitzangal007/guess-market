package guessmarket.client;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;
class NetworkRecoveryTest {
    @Test void lostCommittedResponseResolvesOlderReceiptWithoutViewRollback()throws Exception{recovery(true);}
    @Test void unknown404ExplicitRetryReusesExactOriginalRequest()throws Exception{recovery(false);}
    private void recovery(boolean firstCommits)throws Exception{
        var gson=new Gson();var revision=new AtomicLong(1);var balance=new AtomicReference<>("0.0");var instance=new AtomicReference<>("s");var fundsCalls=new AtomicInteger();var receipt=new AtomicReference<CommandResponse>();var requests=new CopyOnWriteArrayList<FundsRequest>();
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
        server.createContext("/api/",exchange->{String path=exchange.getRequestURI().getPath();exchange.getRequestBody().readAllBytes();if(path.endsWith("login")){reply(exchange,201,gson.toJson(new LoginResponse(instance.get(),revision.get(),"Dana",true)));return;}if(path.endsWith("logout")){exchange.sendResponseHeaders(204,-1);exchange.close();return;}if(path.endsWith("dashboard")){reply(exchange,200,gson.toJson(new DashboardResponse(instance.get(),revision.get(),List.of(),List.of(),new AccountDto("Dana",balance.get(),false,List.of(),List.of(),List.of()),null)));return;}
            if(path.endsWith("funds"))throw new IllegalStateException("Funds handler is separately installed");
            if(path.contains("/commands/")){var retained=receipt.get();if(retained==null)reply(exchange,404,gson.toJson(new ApiError("REQUEST_NOT_FOUND","Not known yet",null,null,null,instance.get(),revision.get())));else reply(exchange,200,gson.toJson(new CommandStatusDto(instance.get(),revision.get(),retained.requestId(),ReceiptStatus.COMPLETED,retained,null,null)));return;}
            reply(exchange,404,"{}");});
        server.createContext("/api/account/funds",exchange->{var request=gson.fromJson(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),FundsRequest.class);requests.add(request);int number=fundsCalls.incrementAndGet();if(number==1&&!firstCommits){exchange.close();return;}
            revision.set(2);balance.set("1.0");var result=new CommandResponse(instance.get(),2,request.requestId(),OperationKind.FUND_LOAD,null,null,"1.0",null,"0.0","1.0",false,List.of(),"Funds loaded.");receipt.set(result);if(number==1){exchange.close();return;}reply(exchange,200,gson.toJson(result));});
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
        var session=TradingScreenTest.fx(()->new RemoteMarketSession("http://localhost:"+server.getAddress().getPort()+"/api/",Platform::runLater));
        try{
            TradingScreenTest.fx(()->{session.login("Dana");return null;});waitFor(()->TradingScreenTest.fx(()->session.dashboard()!=null));TradingScreenTest.fx(()->{session.addFunds("1.00");return null;});waitFor(()->TradingScreenTest.fx(()->session.unknownProperty().get()));assertEquals(1,fundsCalls.get());
            if(firstCommits){revision.set(3);balance.set("5.0");TradingScreenTest.fx(()->{session.refresh();return null;});waitFor(()->TradingScreenTest.fx(()->session.dashboard()!=null&&session.dashboard().revision()==3));}
            TradingScreenTest.fx(()->{session.checkOrRetryOutcome();return null;});waitFor(()->TradingScreenTest.fx(()->!session.unknownProperty().get()&&!session.busyProperty().get()));
            assertEquals(firstCommits?1:2,fundsCalls.get());if(!firstCommits)assertEquals(requests.getFirst(),requests.getLast());
            if(firstCommits)TradingScreenTest.fx(()->{assertEquals(3,session.dashboard().revision());assertEquals("5.0",session.dashboard().account().balance());return null;});
            instance.set("new-server");TradingScreenTest.fx(()->{session.refresh();return null;});waitFor(()->TradingScreenTest.fx(()->!session.loggedInProperty().get()));TradingScreenTest.fx(()->{assertNull(session.dashboard());assertEquals("",session.usernameProperty().get());assertFalse(session.unknownProperty().get());return null;});assertEquals(firstCommits?1:2,fundsCalls.get());
        }finally{TradingScreenTest.fx(()->{session.close();return null;});server.stop(0);}
    }
    private static void reply(HttpExchange exchange,int status,String body)throws java.io.IOException{byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}}
    private interface Condition{boolean get()throws Exception;}
    private static void waitFor(Condition condition)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(!condition.get()){assertTrue(System.nanoTime()<deadline,"Client state deadline");Thread.sleep(15);}}
}
