package guessmarket.client;
import guessmarket.client.ui.*;
import guessmarket.dto.api.*;
import javafx.scene.*;
import javafx.scene.control.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AccountExactDisclosureTest {
    @Test void hiddenNativeSceneRetainsSignedRawAdjustmentAndEconomicAmounts()throws Exception {
        TradingScreenTest.fx(()->{
            var rows=List.of(new AccountEntryDto(1,"t1","time","REPRESENTATION_ADJUSTMENT",null,null,null,"0.00000000000000017","1.00000000000000017","1.00000000000000017"),new AccountEntryDto(2,"t2","time","REPRESENTATION_ADJUSTMENT",null,null,null,"-0.00000000000000017","1.0","1.0"),new AccountEntryDto(3,"t3","time","SHARE_PURCHASE",1,"event",1,"-0.425","0.575","0.575"));
            var view=new AccountActivityView(id->{});view.update(new AccountDto("Dana","0.575",false,List.of(),List.of(),rows));new Scene(view,1120,700);view.applyCss();view.layout();
            var text=new StringBuilder();collect(view,text);for(String amount:List.of("0.00000000000000017","-0.00000000000000017","-0.425"))assertTrue(text.toString().contains("Signed amount: "+amount),text.toString());assertEquals("+<0.01",WireViewMapper.adjustment(rows.get(0).amount()));assertEquals("-<0.01",WireViewMapper.adjustment(rows.get(1).amount()));assertEquals("-0.43",WireViewMapper.money(rows.get(2).amount()));return null;
        });
    }
    private static void collect(Node node,StringBuilder text){if(node instanceof TextArea area)text.append(area.getText());if(node instanceof TitledPane pane&&pane.getContent()!=null)collect(pane.getContent(),text);else if(node instanceof Parent parent)parent.getChildrenUnmodifiable().forEach(child->collect(child,text));}
}
