package guessmarket.client;
import guessmarket.client.http.SessionCookieJar;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SessionCookieJarTest {
    @Test void cookieRulesEnforcePathSecureExpiryReplacementAndIsolation(){
        var jar=new SessionCookieJar();var other=new SessionCookieJar();var url=HttpUrl.get("http://localhost:8080/guessmarket/api/v1/session/login");
        var cookie=new Cookie.Builder().name("JSESSIONID").value("a").hostOnlyDomain("localhost").path("/guessmarket").build();jar.saveFromResponse(url,List.of(cookie));
        assertEquals(List.of(cookie),jar.loadForRequest(url));assertTrue(other.loadForRequest(url).isEmpty());assertTrue(jar.loadForRequest(HttpUrl.get("http://localhost:8080/else")).isEmpty());
        var secure=new Cookie.Builder().name("secure").value("a").hostOnlyDomain("localhost").path("/").secure().build();jar.saveFromResponse(url,List.of(secure));assertEquals(1,jar.loadForRequest(url).size());
        var expired=new Cookie.Builder().name("JSESSIONID").value("b").hostOnlyDomain("localhost").path("/guessmarket").expiresAt(1).build();jar.saveFromResponse(url,List.of(expired));assertTrue(jar.loadForRequest(url).isEmpty());
        jar.close();jar.saveFromResponse(url,List.of(cookie));assertTrue(jar.loadForRequest(url).isEmpty());
    }
}
