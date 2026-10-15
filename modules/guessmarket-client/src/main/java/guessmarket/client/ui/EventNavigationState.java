package guessmarket.client.ui;
import guessmarket.dto.world.OrderSide;
import java.util.*;
public final class EventNavigationState {
    public record Draft(int option,String quantity,OrderSide side,String rawLimit,int winner,String tab) {}
    private record Key(String actor,int event) {}
    private final Map<Key,Draft> drafts=new HashMap<>();
    public void save(String actor,int event,Draft draft){drafts.put(new Key(actor.trim().toLowerCase(Locale.ROOT),event),draft);}
    public Draft draft(String actor,int event){return drafts.getOrDefault(new Key(actor.trim().toLowerCase(Locale.ROOT),event),new Draft(1,"1",OrderSide.BUY,"0.50",1,"Overview"));}
    public void clear(){drafts.clear();}
}
