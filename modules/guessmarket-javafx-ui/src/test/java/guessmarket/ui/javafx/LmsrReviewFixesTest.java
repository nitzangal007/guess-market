package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LmsrReviewFixesTest {
    @BeforeAll static void startFx() throws Exception { FxTestSupport.start(); }
    private static final class View implements AutoCloseable {
        final WorldSession session;
        final Parent root;
        final Stage stage;
        View(String fixture) throws Exception {
            session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
            Stage[] ref=new Stage[1];
            root=FxTestSupport.fx(()->{
                var loader=new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
                loader.setControllerFactory(type->{
                    try{return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                    catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                });
                Parent result=loader.load();ref[0]=new Stage();ref[0].setScene(new Scene(result,1120,850));ref[0].show();return result;
            });
            stage=ref[0];
            act(()->session.load(Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures",fixture)));
            FxTestSupport.fx(()->{((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);root.applyCss();root.layout();return null;});
        }
        void act(Runnable action) throws Exception {
            FxTestSupport.fx(()->{action.run();return null;});FxTestSupport.awaitIdle(session);
        }
        void user(int index) throws Exception {
            FxTestSupport.fx(()->{FxTestSupport.chooseUser(root,index);root.applyCss();root.layout();return null;});
        }
        @Override public void close() {
            try { FxTestSupport.fx(()->{session.close();stage.close();return null;}); }
            catch(Exception failure){throw new IllegalStateException(failure);}
        }
    }
    @Test void nonFirstParticipationIsMarkedAndClosedHistoryRemains() throws Exception {
        try(var view=new View("multiple.xml")){
            view.act(()->view.session.previewOpening("Tikva",1,p->view.session.completeOpening(true)));
            view.act(()->view.session.previewOpening("Tikva",4,p->view.session.completeOpening(true)));
            view.user(2);
            view.act(()->view.session.previewPurchase("Menash",4,1,1,p->view.session.completePurchase(true)));
            FxTestSupport.fx(()->{
                assertEquals(2,((FlowPane)view.root.lookup("#userEventRows")).getChildren().size());
                var marker=(Label)view.root.lookup("#participation-event-4");
                assertNotNull(marker,"The user's non-first participating event must be identifiable without activating every card");
                assertTrue(marker.getText().contains("Participating"));
                assertNull(view.root.lookup("#participation-event-1"));
                assertTrue(((ToggleButton)view.root.lookup("#user-event-4")).getAccessibleText().contains("Participating"));
                save(view.root,"participating-event-4.png");
                return null;
            });
            view.act(()->view.session.previewClose("Tikva",4,1,p->view.session.completeClose(true)));
            view.act(()->view.session.previewPurchase("Menash",1,1,1,p->view.session.completePurchase(true)));
            FxTestSupport.fx(()->{
                assertNull(view.root.lookup("#participation-event-4"));
                assertNotNull(view.root.lookup("#user-event-1"),"First purchase route must remain");
                ((ToggleButton)view.root.lookup("#participationsSection")).fire();
                view.root.applyCss();view.root.layout();
                assertNotNull(view.root.lookup("#history-event-4"));
                assertNotNull(view.root.lookup("#participation-event-1"));
                assertEquals(2,((FlowPane)view.root.lookup("#userEventRows")).getChildren().size());
                save(view.root,"mixed-participations.png");
                ((ToggleButton)view.root.lookup("#user-event-4")).fire();
                FxTestSupport.userDetail(view.root,"Your position");
                assertTrue(((Label)view.root.lookup("#positionSummary")).getText().contains("= 1"));
                assertTrue(((Label)view.root.lookup("#user-eventState")).getText().contains("Closed"));
                save(view.root,"closed-participation-history.png");return null;
            });
        }
    }
    @Test void userSwitchDismissesReceiptButCommandRerenderKeepsItAndHistory() throws Exception {
        try(var view=new View("small.xml")){
            view.act(()->view.session.previewOpening("Tikva",1,p->view.session.completeOpening(true)));
            view.user(2);
            view.act(()->view.session.previewPurchase("Menash",1,1,500,p->view.session.completePurchase(true)));
            var afterPurchase=FxTestSupport.fx(()->view.session.worldProperty().get());
            FxTestSupport.fx(()->{
                assertTrue(((Label)view.root.lookup("#purchaseNotice")).getText().contains("Purchase completed for Menash"),
                        "The command's own world rerender must not erase its receipt");
                assertTrue(view.root.lookup("#blockedStatus").isVisible());return null;
            });
            view.user(1);
            FxTestSupport.fx(()->{
                assertTrue(((Label)view.root.lookup("#purchaseNotice")).getText().isEmpty(),
                        "Tikva must not display Menash's transient receipt");
                assertFalse(view.root.lookup("#purchaseNotice").isVisible());
                assertSame(afterPurchase,view.session.worldProperty().get());
                save(view.root,"receipt-cleared-on-user-switch.png");return null;
            });
            view.user(2);
            FxTestSupport.fx(()->{
                assertTrue(((Label)view.root.lookup("#purchaseNotice")).getText().isEmpty());
                assertTrue(view.root.lookup("#blockedStatus").isVisible());
                FxTestSupport.viewUser(view.root,"participationsSection",1);
                FxTestSupport.userDetail(view.root,"Your position");
                assertTrue(((Label)view.root.lookup("#positionSummary")).getText().contains("500"));
                assertSame(afterPurchase,view.session.worldProperty().get());return null;
            });
        }
    }
    private static void save(Parent root,String name) throws Exception {
        root.applyCss();root.layout();var image=root.snapshot(null,null);
        var png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        var file=Path.of("private/coordination/assignment-2/ui-evidence-terminal",name);
        java.nio.file.Files.createDirectories(file.getParent());assertTrue(javax.imageio.ImageIO.write(png,"png",file.toFile()));
    }
}
