package guessmarket.client.http;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MarketHttpClientTest {
    @Test void realHttpClosesBodiesAndPreservesContainerErrorsWithoutRedirect()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);var count=new AtomicInteger();
        server.createContext("/api/session/login",exchange->{int n=count.incrementAndGet();String body=n==1?"{\"serverInstanceId\":\"s\",\"revision\":0,\"username\":\"Dana\",\"createdAccount\":true}":n==2?"<h1>forbidden</h1>":n==3?"bad": "redirect";int status=n==2?403:n==4?302:200;if(n==4)exchange.getResponseHeaders().add("Location","/destination");byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}});server.start();
        try(var client=new MarketHttpClient("http://localhost:"+server.getAddress().getPort()+"/api/")){
            assertEquals("Dana",client.login("Dana").username());assertEquals(403,assertThrows(ApiCallException.class,()->client.login("Dana")).status());assertThrows(java.io.IOException.class,()->client.login("Dana"));assertEquals(302,assertThrows(ApiCallException.class,()->client.login("Dana")).status());assertEquals(4,count.get());
            assertEquals(5000,client.transport.connectTimeoutMillis());assertEquals(10000,client.transport.readTimeoutMillis());assertEquals(10000,client.transport.writeTimeoutMillis());assertEquals(15000,client.transport.callTimeoutMillis());assertFalse(client.transport.retryOnConnectionFailure());assertFalse(client.transport.followRedirects());assertFalse(client.transport.followSslRedirects());assertTrue(client.transport.connectionPool().connectionCount()<=1);
        }finally{server.stop(0);}
    }
    @Test void uploadCapturesOriginalBytesAndMetadataForRetry()throws Exception {
        Path path=Files.createTempFile("market-upload ",".xml");try{Files.writeString(path,"original");var attempt=UploadAttempt.capture(path,UUID.randomUUID().toString());Files.writeString(path,"changed");byte[] copy=attempt.bytes();copy[0]=0;assertEquals("original",new String(attempt.bytes(),StandardCharsets.UTF_8));assertEquals(path.getFileName().toString(),attempt.filename());assertEquals(64,attempt.digest().length());}finally{Files.delete(path);}
    }
}
