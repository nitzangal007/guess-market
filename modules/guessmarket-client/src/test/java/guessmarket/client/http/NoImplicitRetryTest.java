package guessmarket.client.http;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import guessmarket.dto.api.*;
import guessmarket.dto.world.OrderSide;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class NoImplicitRetryTest {
    @Test void readsAndOtherFollowupStatusesAlsoEmitOneWireRequest()throws Exception{
        for(int status:List.of(401,408,503,307)){
            var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
            server.createContext("/api/dashboard",exchange->{count.incrementAndGet();exchange.getResponseHeaders().add("Retry-After","0");exchange.getResponseHeaders().add("Location","http://elsewhere.invalid/");byte[] bytes="{\"code\":\"UNAVAILABLE\",\"message\":\"Unavailable\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}});server.start();
            try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){assertEquals(status,assertThrows(ApiCallException.class,()->client.dashboard(null)).status());assertEquals(1,count.get());}finally{server.stop(0);}
        }
    }
    @Test void loginFundsCommandAndUpload503NeverResendWithoutExplicitInvocation()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);var counts=new ConcurrentHashMap<String,AtomicInteger>();var bodies=new ConcurrentHashMap<String,List<String>>();var ids=new ConcurrentHashMap<String,List<String>>();var gson=new Gson();String uuid=UUID.randomUUID().toString();
        server.createContext("/api/",exchange->{String path=exchange.getRequestURI().getPath();int n=counts.computeIfAbsent(path,k->new AtomicInteger()).incrementAndGet();bodies.computeIfAbsent(path,k->new ArrayList<>()).add(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));ids.computeIfAbsent(path,k->new ArrayList<>()).add(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            Object value;if(n==1)value=new ApiError("TEMPORARY_FAILURE","Unavailable",null,null,uuid,"s",1L);else if(path.endsWith("login"))value=new LoginResponse("s",2,"Dana",true);else if(path.endsWith("upload"))value=new UploadResponse("s",2,uuid,OperationKind.UPLOAD,List.of(new AddedEventDto(1,"event")));else if(path.contains("/events/")){var action=new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".4250");var review=new ActionPreviewDto("s",1,1,"event","Dana",null,action,"0","0","0","0",false,List.of(),List.of(),1,0,0,List.of(),null,null,null,"Review");value=new CommandResponse("s",2,uuid,OperationKind.ORDER,1,action,null,review,"0","0",false,List.of(),"done");}else value=new CommandResponse("s",2,uuid,OperationKind.FUND_LOAD,null,null,"1",null,"0","1",false,List.of(),"done");byte[] bytes=gson.toJson(value).getBytes(java.nio.charset.StandardCharsets.UTF_8);if(n==1)exchange.getResponseHeaders().add("Retry-After","0");exchange.sendResponseHeaders(n==1?503:200,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
        });server.start();
        try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){
            assertEquals(503,assertThrows(ApiCallException.class,()->client.login("Dana")).status());assertEquals(1,counts.get("/api/session/login").get());client.login("Dana");
            var funds=new FundsRequest(uuid,"1");assertEquals(503,assertThrows(ApiCallException.class,()->client.addFunds(funds)).status());assertEquals(1,counts.get("/api/account/funds").get());client.addFunds(funds);assertEquals(bodies.get("/api/account/funds").getFirst(),bodies.get("/api/account/funds").getLast());
            var command=new CommandRequest(uuid,1,new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".4250"));assertEquals(503,assertThrows(ApiCallException.class,()->client.execute(1,command)).status());assertEquals(1,counts.get("/api/events/1/commands").get());client.execute(1,command);assertEquals(bodies.get("/api/events/1/commands").getFirst(),bodies.get("/api/events/1/commands").getLast());
            var upload=new UploadAttempt(uuid,"original.xml","original bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));assertEquals(503,assertThrows(ApiCallException.class,()->client.upload(upload)).status());assertEquals(1,counts.get("/api/events/upload").get());client.upload(upload);assertEquals(List.of(uuid,uuid),ids.get("/api/events/upload"));assertTrue(bodies.get("/api/events/upload").stream().allMatch(body->body.contains("original.xml")&&body.contains("original bytes")));
        }finally{server.stop(0);}
    }
}
