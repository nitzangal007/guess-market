package guessmarket.client.http;
import okhttp3.*;
import java.util.*;
public final class SessionCookieJar implements CookieJar {
    private record Key(String name,String domain,String path) {}
    private final Map<Key,Cookie> cookies=new LinkedHashMap<>();
    private boolean closed;
    @Override public synchronized void saveFromResponse(HttpUrl url,List<Cookie> values){
        if(closed)return;
        for(var cookie:values){if(cookie.hostOnly()?!url.host().equals(cookie.domain()):!(url.host().equals(cookie.domain())||url.host().endsWith("."+cookie.domain())))continue;
            var key=new Key(cookie.name(),cookie.domain(),cookie.path());if(cookie.expiresAt()<=System.currentTimeMillis())cookies.remove(key);else cookies.put(key,cookie);}
    }
    @Override public synchronized List<Cookie> loadForRequest(HttpUrl url){if(closed)return List.of();cookies.values().removeIf(c->c.expiresAt()<=System.currentTimeMillis());return cookies.values().stream().filter(c->c.matches(url)).toList();}
    public synchronized void clear(){cookies.clear();}
    public synchronized void close(){closed=true;cookies.clear();}
}
