package guessmarket.client;
import guessmarket.client.http.*;
import guessmarket.client.session.*;
import guessmarket.client.ui.*;
import guessmarket.dto.api.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
@Tag("live-server")
class NativeRemoteFlowTest {
    private static final String URL=System.getProperty("guessmarket.integrationUrl","http://localhost:18080/guessmarket/api/v1/");
    @Test void realTwoSessionsPollTradeAndShowNativeAccountWithoutDraftLoss()throws Exception{
        String suffix=UUID.randomUUID().toString().substring(0,8);String owner="NativeOwner"+suffix,buyer="NativeBuyer"+suffix;
        int event;
        try(var setup=new MarketHttpClient(URL)){
            setup.login(owner);setup.addFunds(new FundsRequest(UUID.randomUUID().toString(),"1000"));
            String xml="<Guess-Market><GM-events><GM-event name='Native book "+suffix+"'><description>Real native HTTP flow</description><commission type='on-purchase'>5</commission><GM-options><GM-option>Red</GM-option><GM-option>Blue</GM-option></GM-options><GM-method><GM-order-book initial='100' d='1' allow-mint='true'/></GM-method></GM-event></GM-events></Guess-Market>";
            event=setup.upload(new UploadAttempt(UUID.randomUUID().toString(),"native.xml",xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))).addedEvents().getFirst().id();var open=setup.preview(event,new ActionRequest(ActionKind.OPEN,null,null,null,null));setup.execute(event,new CommandRequest(UUID.randomUUID().toString(),open.revision(),open.action()));
            var sell=setup.preview(event,new ActionRequest(ActionKind.ORDER,1,1,guessmarket.dto.world.OrderSide.SELL,".425"));setup.execute(event,new CommandRequest(UUID.randomUUID().toString(),sell.revision(),sell.action()));setup.logout();
        }
        var ownerSession=TradingScreenTest.fx(()->new RemoteMarketSession(URL,Platform::runLater));var buyerSession=TradingScreenTest.fx(()->new RemoteMarketSession(URL,Platform::runLater));
        Stage stage=TradingScreenTest.fx(()->{var s=new Stage();s.setScene(new Scene(new ClientShell(buyerSession),1120,820));s.show();return s;});
        try{
            TradingScreenTest.fx(()->{ownerSession.login(owner);buyerSession.login(buyer);return null;});
            await(ownerSession,d->d.account().username().equals(owner));await(buyerSession,d->d.account().username().equals(buyer));
            TradingScreenTest.fx(()->{buyerSession.addFunds("100");return null;});await(buyerSession,d->d.account().balance().equals("100.0"));
            TradingScreenTest.fx(()->{closeOwnedDialogs(stage);var list=(ListView<?>)stage.getScene().lookup("#events-list");var item=list.getItems().stream().filter(row->((EventSummaryDto)row).id()==event).findFirst().orElseThrow();list.getSelectionModel().select(list.getItems().indexOf(item));return null;});
            await(buyerSession,d->d.selectedEvent()!=null&&d.selectedEvent().summary().id()==event);
            var preview=new ArrayBlockingQueue<ActionPreviewDto>(1);TradingScreenTest.fx(()->{buyerSession.preview(event,new ActionRequest(ActionKind.ORDER,1,1,guessmarket.dto.world.OrderSide.BUY,".425"),preview::add,message->fail(message));return null;});
            var reviewed=preview.poll(10,TimeUnit.SECONDS);assertNotNull(reviewed);TradingScreenTest.fx(()->{buyerSession.execute(reviewed);return null;});
            await(buyerSession,d->d.account().balance().equals("99.55375"));await(ownerSession,d->d.account().entries().stream().anyMatch(row->row.kind().equals("SALE_RECEIPT")));
            TradingScreenTest.fx(()->{
                var root=stage.getScene().getRoot();root.applyCss();root.layout();assertEquals("Balance: 99.55",((Label)root.lookup("#own-balance")).getText());capture(stage,"client-wide.png");
                ((TextField)root.lookup("#quantity")).setText("7");((TextField)root.lookup("#limit-price")).setText(".4250");
                double decorationWidth=stage.getWidth()-stage.getScene().getWidth(),decorationHeight=stage.getHeight()-stage.getScene().getHeight();stage.setWidth(680+decorationWidth);stage.setHeight(602+decorationHeight);return null;
            });
            var resized=new CountDownLatch(1);TradingScreenTest.fx(()->{stage.getScene().widthProperty().addListener((o,a,b)->{if(b.doubleValue()<=681)resized.countDown();});if(stage.getScene().getWidth()<=681)resized.countDown();return null;});assertTrue(resized.await(5,TimeUnit.SECONDS));
            TradingScreenTest.fx(()->{var root=stage.getScene().getRoot();root.applyCss();root.layout();capture(stage,"client-narrow.png");assertTrue(root.lookup("#own-balance").isVisible());assertEquals(680,stage.getScene().getWidth(),1);var pane=(TabPane)root.lookup(".tab-pane");pane.getSelectionModel().select(1);pane.getSelectionModel().select(0);return null;});
            await(buyerSession,d->d.selectedEvent()!=null&&d.selectedEvent().summary().id()==event);
            TradingScreenTest.fx(()->{assertEquals(".4250",((TextField)stage.getScene().lookup("#limit-price")).getText());assertEquals("7",((TextField)stage.getScene().lookup("#quantity")).getText());buyerSession.logout();return null;});
            TradingScreenTest.fx(()->{assertNotNull(stage.getScene().lookup("#login-name"));assertNull(stage.getScene().lookup("#own-balance"));return null;});
        }finally{TradingScreenTest.fx(()->{closeOwnedDialogs(stage);stage.close();ownerSession.close();buyerSession.close();return null;});}
    }
    private static void await(RemoteMarketSession session,Predicate<DashboardResponse> predicate)throws Exception{
        var ready=new CountDownLatch(1);ChangeListener<DashboardResponse> listener=(o,a,b)->{if(b!=null&&predicate.test(b))ready.countDown();};TradingScreenTest.fx(()->{session.dashboardProperty().addListener(listener);if(session.dashboard()!=null&&predicate.test(session.dashboard()))ready.countDown();return null;});
        boolean done=ready.await(15,TimeUnit.SECONDS);TradingScreenTest.fx(()->{session.dashboardProperty().removeListener(listener);return null;});assertTrue(done,"Dashboard condition timed out");
    }
    private static void closeOwnedDialogs(Stage stage){for(var window:List.copyOf(Window.getWindows()))if(window instanceof Stage s&&s.getOwner()==stage)s.close();}
    private static void capture(Stage stage,String name)throws Exception{
        var pixels=stage.getScene().snapshot(null);var buffered=new BufferedImage((int)pixels.getWidth(),(int)pixels.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int y=0;y<buffered.getHeight();y++)for(int x=0;x<buffered.getWidth();x++)buffered.setRGB(x,y,pixels.getPixelReader().getArgb(x,y));Path directory=Path.of(System.getProperty("guessmarket.nativeEvidence","build/exercise3/native"));Files.createDirectories(directory);ImageIO.write(buffered,"png",directory.resolve(name).toFile());
    }
}
