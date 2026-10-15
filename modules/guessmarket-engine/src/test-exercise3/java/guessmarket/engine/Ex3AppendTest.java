package guessmarket.engine;
import org.junit.jupiter.api.Test;
import guessmarket.engine.xml.ex3.*;
import guessmarket.dto.world.*;
import guessmarket.dto.CommissionMode;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class Ex3AppendTest {
    @Test void uploadsAppendWithServerOwnerAndDuplicateRejectsEntireBatch()throws Exception{
        var engine=new SharedMarketEngineImpl("test");engine.ensureAccount("Dana");engine.ensureAccount("Bob");
        var first=engine.appendEvents("Dana",batch("a"),UUID.randomUUID().toString());assertEquals(1,first.addedEvents().getFirst().id());
        engine.appendEvents("Bob",batch("b"),UUID.randomUUID().toString());var before=engine.dashboard("Bob",null);
        assertEquals(2,before.events().size());assertEquals("0.0",before.account().balance());assertTrue(before.account().entries().isEmpty());
        assertThrows(WorldCommandException.class,()->engine.appendEvents("Bob",batch("c","a"),UUID.randomUUID().toString()));assertEquals(before,engine.dashboard("Bob",null));
        assertEquals("Dana",engine.dashboard("Dana",1).selectedEvent().summary().owner());
        assertEquals("NOT_STARTED",before.events().getFirst().status());
    }
    static ParsedEventBatch batch(String...names){return new ParsedEventBatch(Arrays.stream(names).map(n->new EventDefinition(n,"",List.of("Red","Blue"),5,CommissionMode.ON_PURCHASE,new OrderBookConfiguration(100,1,true))).toList());}
}
