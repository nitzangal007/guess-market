package guessmarket.client.http;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class ClientCancellationTest {
    @Test void registrationCloseRaceTerminatesAndNoInvocationStartsAfterClose()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
        server.createContext("/api/session/login",exchange->{exchange.getRequestBody().readAllBytes();byte[] bytes="{\"serverInstanceId\":\"s\",\"revision\":0,\"username\":\"Dana\",\"createdAccount\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);try{exchange.sendResponseHeaders(200,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}}catch(IOException ignored){/* Race may cancel the network. */}});server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
            for(int i=0;i<12;i++){var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/");var barrier=new CyclicBarrier(2);
                var invoke=pool.submit(()->{barrier.await();try{return client.login("Dana");}catch(IOException failure){return null;}});var closing=pool.submit(()->{barrier.await();client.close();return null;});closing.get(3,TimeUnit.SECONDS);invoke.get(3,TimeUnit.SECONDS);assertThrows(IOException.class,()->client.dashboard(null));client.close();}
        }finally{server.stop(0);}
    }
    @Test void closeCancelsStalledBodyAfterHeadersAndRejectsNewCalls()throws Exception{cancel(true);}
    @Test void closeCancelsBeforeHeadersAndRejectsNewCalls()throws Exception{cancel(false);}
    @SuppressWarnings("try") // Explicit close during an active read is the behavior under test.
    private void cancel(boolean headersFirst)throws Exception{
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        server.createContext("/api/session/login",exchange->{try{exchange.getRequestBody().readAllBytes();if(headersFirst){exchange.sendResponseHeaders(200,200);exchange.getResponseBody().write('{');exchange.getResponseBody().flush();}entered.countDown();release.await(15,TimeUnit.SECONDS);}catch(Exception ignored){/* Test closes the socket. */}finally{exchange.close();}});server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
        try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/");var pool=Executors.newVirtualThreadPerTaskExecutor()){
            var future=pool.submit(()->{try{client.login("Dana");return null;}catch(Exception failure){return failure;}});assertTrue(entered.await(3,TimeUnit.SECONDS));
            if(headersFirst){long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(client.transport.dispatcher().runningCallsCount()!=0&&System.nanoTime()<deadline)Thread.sleep(5);}
            long start=System.nanoTime();client.close();var result=future.get(13,TimeUnit.SECONDS);long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);System.out.println("Close headersFirst="+headersFirst+" settledMs="+elapsed);assertInstanceOf(IOException.class,result);assertTrue(elapsed<3000,"Cancellation must settle promptly through body reads");assertThrows(IOException.class,()->client.login("Dana"));
        }finally{release.countDown();server.stop(0);}
    }
}
