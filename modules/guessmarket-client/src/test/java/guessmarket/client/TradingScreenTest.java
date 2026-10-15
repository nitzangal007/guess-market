package guessmarket.client;
import guessmarket.client.ui.*;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import guessmarket.dto.world.OrderSide;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
@Tag("native-ui")
class TradingScreenTest {
    private static final AtomicBoolean STARTED=new AtomicBoolean();
    static <T> T fx(Callable<T> call)throws Exception{if(STARTED.compareAndSet(false,true)){Platform.startup(()->{});Platform.setImplicitExit(false);}var task=new FutureTask<>(call);Platform.runLater(task);return task.get(20,TimeUnit.SECONDS);}
    @Test void realNativeFormPreservesRawInputThroughDetailTabsAndRerender()throws Exception {
        fx(()->{
            var session=new RemoteMarketSession();var state=new EventNavigationState();var view=new EventDetailView(session,state);
            var summary=new EventSummaryDto(1,"Long event name","Description","ORDER_BOOK","ACTIVE","Dana",5,"ON_PURCHASE","100.0");
            var book=new BookDetailDto(1,100,true,100,List.of(),List.of(),List.of(),List.of());
            var detail=new EventDetailDto(summary,List.of("",""),null,book,null,null,false,true,true);
            var stage=new Stage();stage.setScene(new Scene(view,680,602));stage.show();
            try{
                view.showDetail(detail,"Dana");view.applyCss();view.layout();
                var quantity=(TextField)view.lookup("#quantity");var limit=(TextField)view.lookup("#limit-price");assertNotNull(quantity);assertNotNull(limit);quantity.setText("7");limit.setText(".4250");
                @SuppressWarnings("unchecked") var option=(ComboBox<Integer>)view.lookup("#option");@SuppressWarnings("unchecked") var side=(ComboBox<OrderSide>)view.lookup("#side");option.setValue(2);side.setValue(OrderSide.SELL);
                view.showDetail(detail,"Dana");assertEquals(".4250",((TextField)view.lookup("#limit-price")).getText());assertEquals("7",((TextField)view.lookup("#quantity")).getText());assertEquals(2,((ComboBox<?>)view.lookup("#option")).getValue());assertEquals(OrderSide.SELL,((ComboBox<?>)view.lookup("#side")).getValue());
            }finally{stage.close();session.close();}return null;
        });
    }
}
