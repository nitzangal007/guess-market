package guessmarket.server;
import java.util.*;
final class SessionRegistry {
    private final Map<String,String> displayNames=new HashMap<>(),activeNames=new HashMap<>(),bindings=new HashMap<>();
    private final Set<String> revoked=new HashSet<>();
    String reserve(String session,String key,MarketApplication app){
        if(session==null||revoked.contains(session))throw app.failure(401,"SESSION_REVOKED","Use a new login session.",null);
        String bound=bindings.get(session);if(bound!=null&&!bound.equals(key))throw app.failure(409,"SESSION_ALREADY_BOUND","Log out before changing identity.",null);
        String owner=activeNames.get(key);if(owner!=null&&!owner.equals(session))throw app.failure(409,"USER_ALREADY_CONNECTED","This name is already connected. Close that client or wait for session expiry.",null);
        return displayNames.get(key);
    }
    void bind(String session,String key,String display){displayNames.putIfAbsent(key,display);bindings.put(session,key);activeNames.put(key,session);}
    String actor(String session){if(session==null||revoked.contains(session))return null;return displayNames.get(bindings.get(session));}
    void revoke(String session){if(session==null)return;revoked.add(session);String key=bindings.remove(session);if(key!=null)activeNames.remove(key,session);}
}
