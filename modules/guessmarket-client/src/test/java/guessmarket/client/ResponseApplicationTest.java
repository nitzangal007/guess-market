package guessmarket.client;
import org.junit.jupiter.api.Test;
import guessmarket.client.session.*;
import guessmarket.dto.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ResponseApplicationTest {
    @Test void dashboardsInitializeAtEqualRevisionAndSelectedDetailNeverPaintsOldSelection(){
        var gate=new DashboardGate();gate.reset(1,"s");long selection=gate.select(null);
        var dashboard=new DashboardResponse("s",11,List.of(),List.of(),new AccountDto("Dana","0.0",false,List.of(),List.of(),List.of()),null);
        assertTrue(gate.accept(1,selection,dashboard));assertFalse(gate.accept(1,selection,dashboard));
        assertFalse(gate.accept(0,selection,dashboard));
        long next=gate.select(1);assertFalse(gate.accept(1,selection,dashboard));
        var summary=new EventSummaryDto(1,"event","","ORDER_BOOK","ACTIVE","Dana",0,"ON_CLOSE","0");
        var detail=new EventDetailDto(summary,List.of("",""),null,null,null,null,false,true,true);
        assertTrue(gate.accept(1,next,new DashboardResponse("s",11,List.of(summary),List.of(),dashboard.account(),detail)));
        assertEquals(11,gate.revision());
    }
    @Test void olderReceiptResolvesOperationWithoutAdvancingDashboardRevision(){
        var operation=new OperationGate();operation.reset(2,"s");operation.begin("id");
        assertFalse(operation.resolve(1,"s","id"));assertFalse(operation.resolve(2,"other","id"));assertTrue(operation.pending());
        assertTrue(operation.resolve(2,"s","id"));assertFalse(operation.pending());
        assertFalse(operation.resolve(2,"s","id"));
    }
}
