package guessmarket.client;
import guessmarket.client.ui.EventNavigationState;
import guessmarket.dto.world.OrderSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EventNavigationTest {
    @Test void rawDraftSurvivesTabsRoutesBackAndCaseSpellingButNotLogout(){
        var state=new EventNavigationState();var draft=new EventNavigationState.Draft(2,"7",OrderSide.SELL,".4250",2,"Trade history");
        state.save("Dana",1,draft);assertEquals(draft,state.draft("DANA",1));assertEquals(1,state.draft("Dana",2).option());assertEquals(1,state.draft("Bob",1).option());state.clear();assertEquals(1,state.draft("Dana",1).option());
    }
}
