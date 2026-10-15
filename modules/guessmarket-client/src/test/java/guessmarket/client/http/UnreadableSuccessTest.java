package guessmarket.client.http;
import com.sun.net.httpserver.HttpServer;
import guessmarket.dto.api.*;
import guessmarket.dto.world.OrderSide;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class UnreadableSuccessTest {
    @Test void genericRecordAdapterFailureWrapsToIOExceptionButFatalErrorsPropagate()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);server.createContext("/api/session/login",exchange->{exchange.getRequestBody().readAllBytes();byte[] bytes="{\"serverInstanceId\":\"s\",\"revision\":1,\"username\":\"Dana\",\"createdAccount\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}});server.start();
        try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){
            var fatal=new java.util.concurrent.atomic.AtomicBoolean();var adapter=new com.google.gson.TypeAdapter<LoginResponse>(){
                @Override public void write(com.google.gson.stream.JsonWriter writer,LoginResponse value){throw new UnsupportedOperationException();}
                @Override public LoginResponse read(com.google.gson.stream.JsonReader reader){if(fatal.get())throw new AssertionError("Fatal test signal");throw new RuntimeException("Record constructor failed");}
            };
            var field=MarketHttpClient.class.getDeclaredField("gson");field.setAccessible(true);field.set(client,new com.google.gson.GsonBuilder().setStrictness(com.google.gson.Strictness.STRICT).registerTypeAdapter(LoginResponse.class,adapter).create());
            assertInstanceOf(RuntimeException.class,assertThrows(IOException.class,()->client.login("Dana")).getCause());fatal.set(true);assertThrows(AssertionError.class,()->client.login("Dana"));
        }finally{server.stop(0);}
    }
    @Test void missingShapesAndMismatchedMetadataAlwaysBecomeIOException()throws Exception{
        var body=new AtomicReference<>("{}");var server=HttpServer.create(new InetSocketAddress("localhost",0),0);server.createContext("/api/",exchange->{exchange.getRequestBody().readAllBytes();byte[] bytes=body.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}});server.start();
        String id=UUID.randomUUID().toString();try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){
            assertThrows(IOException.class,()->client.login("Dana"));assertThrows(IOException.class,()->client.dashboard(null));assertThrows(IOException.class,()->client.commandStatus(id));assertThrows(IOException.class,()->client.addFunds(new FundsRequest(id,"1")));assertThrows(IOException.class,()->client.execute(1,new CommandRequest(id,1,new ActionRequest(ActionKind.ORDER,1,1,OrderSide.BUY,".4250"))));assertThrows(IOException.class,()->client.upload(new UploadAttempt(id,"test.xml",new byte[]{1})));
            for(String value:List.of("{\"revision\":0,\"username\":\"Dana\",\"createdAccount\":true}","{\"serverInstanceId\":\"s\",\"revision\":-1,\"username\":\"Dana\",\"createdAccount\":true}","{\"serverInstanceId\":\"s\",\"revision\":0,\"username\":\"Dana\"}")){body.set(value);assertThrows(IOException.class,()->client.login("Dana"));}
            body.set("{\"serverInstanceId\":\"s\",\"revision\":2,\"requestId\":\"wrong\",\"operation\":\"FUND_LOAD\",\"creditedAmount\":\"1\",\"balanceBefore\":\"0\",\"balanceAfter\":\"1\",\"blockedAfter\":false,\"ownTransactionEntries\":[],\"message\":\"done\"}");assertThrows(IOException.class,()->client.addFunds(new FundsRequest(id,"1")));
            body.set("{\"serverInstanceId\":\"s\",\"revision\":2,\"requestId\":\""+id+"\",\"status\":\"COMPLETED\"}");assertThrows(IOException.class,()->client.commandStatus(id));
            body.set("{\"serverInstanceId\":\"s\",\"revision\":1,\"username\":\"Dana\",\"createdAccount\":true}");assertEquals("Dana",client.login("Dana").username());
        }finally{server.stop(0);}
    }
}
