package guessmarket.server;
import java.util.*;
import guessmarket.dto.api.ApiError;
final class RequestRegistry {
    record Entry(String fingerprint,Object result,ApiError error,int status) {}
    private record Key(String actor,String id) {}
    private final Map<Key,Entry> entries=new HashMap<>();
    Entry lookup(String actor,String id){return entries.get(new Key(actor,id.toLowerCase(Locale.ROOT)));}
    void complete(String actor,String id,String fingerprint,Object result){entries.put(new Key(actor,id.toLowerCase(Locale.ROOT)),new Entry(fingerprint,result,null,200));}
    void reject(String actor,String id,String fingerprint,ApiFailure error){entries.put(new Key(actor,id.toLowerCase(Locale.ROOT)),new Entry(fingerprint,null,error.error(),error.status()));}
}
