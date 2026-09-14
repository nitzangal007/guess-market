package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.nio.file.*;
import java.util.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class TransientFeedbackContextTest {
    private enum Navigation { TOP_LEVEL, SECTION, EVENT, DETAIL_TAB, USER }
    private record Scenario(WorldSession session,Parent root,Stage stage) {}
    @TempDir Path directory;
    @BeforeAll static void start()throws Exception{FxTestSupport.start();}

    @Test void successAndCommandErrorClearAcrossEveryExplicitContextSwitch()throws Exception{
        var failures=new ArrayList<String>();
        for(var navigation:Navigation.values()){
            var scenario=open(navigation);
            try{
                submitSell(scenario);
                assertFalse(FxTestSupport.fx(()->scenario.session().noticeProperty().get()).isEmpty());
                navigate(scenario,navigation);
                if(!FxTestSupport.fx(()->scenario.session().noticeProperty().get()).isEmpty())
                    failures.add(navigation+" retained success notice");

                returnToOwnerEventOne(scenario,navigation);
                submitSelfMatch(scenario);
                assertFalse(FxTestSupport.fx(()->scenario.session().errorProperty().get()).isEmpty());
                navigate(scenario,navigation);
                if(!FxTestSupport.fx(()->scenario.session().errorProperty().get()).isEmpty())
                    failures.add(navigation+" retained command error");
            }finally{close(scenario);}
        }
        assertTrue(failures.isEmpty(),String.join("; ",failures));
    }

    @Test void loadValidationSurvivesNavigationUntilAnotherOperationBegins()throws Exception{
        var scenario=open(null);
        try{
            Path invalid=Files.writeString(directory.resolve("invalid.xml"),"<not-guess-market/>");
            FxTestSupport.fx(()->{scenario.session().load(invalid);return null;});FxTestSupport.awaitIdle(scenario.session());
            String loadError=FxTestSupport.fx(()->scenario.session().errorProperty().get());
            assertFalse(loadError.isEmpty());
            FxTestSupport.fx(()->{((TabPane)scenario.root().lookup("#navigation")).getSelectionModel().select(0);return null;});
            assertEquals(loadError,FxTestSupport.fx(()->scenario.session().errorProperty().get()));
        }finally{close(scenario);}
    }

    private Scenario open(Navigation navigation)throws Exception{
        String event="<GM-event name=\"Book%s\"><id>%s</id><description>Context</description><commission type=\"on-purchase\">0</commission>"
                +"<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options><GM-method>"
                +"<GM-order-book initial=\"10\" d=\"1\" allow-mint=\"false\"/></GM-method></GM-event>";
        String xml="<Guess-Market><GM-events>"+event.formatted(1,1)+event.formatted(2,2)+"</GM-events><GM-users>"
                +"<GM-user name=\"Owner\"><initial-cash>1000</initial-cash><GM-market-maker><event id=\"1\"/><event id=\"2\"/></GM-market-maker></GM-user>"
                +"<GM-user name=\"Buyer\"><initial-cash>100</initial-cash></GM-user></GM-users></Guess-Market>";
        Path fixture=Files.writeString(directory.resolve("context-"+navigation+".xml"),xml);
        var session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        var stage=new Stage[1];
        Parent root=FxTestSupport.fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
            loader.setControllerFactory(type->{try{return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}});
            Parent view=loader.load();stage[0]=new Stage();stage[0].setScene(new Scene(view,1120,820));stage[0].show();
            session.load(fixture);return view;
        });
        FxTestSupport.awaitIdle(session);
        for(int id:new int[]{1,2}){final int eventId=id;
            FxTestSupport.fx(()->{session.previewOpening("Owner",eventId,p->session.completeOpening(true));return null;});
            FxTestSupport.awaitIdle(session);
        }
        FxTestSupport.fx(()->{
            ((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);
            ((ToggleButton)root.lookup("#ownedSection")).fire();
            ((ToggleButton)root.lookup("#user-event-1")).fire();root.applyCss();root.layout();return null;
        });
        return new Scenario(session,root,stage[0]);
    }
    private static void submitSell(Scenario scenario)throws Exception{
        FxTestSupport.fx(()->{((Button)scenario.root().lookup("#submitOrder")).fire();return null;});
        var input=dialog("orderInput");
        FxTestSupport.fx(()->{
            ((ComboBox<?>)input.lookup("#orderSide")).getSelectionModel().select(1);
            ((TextField)input.lookup("#orderQuantity")).setText("1");
            ((TextField)input.lookup("#orderPrice")).setText("0.5");press(input,"Review order");return null;
        });
        var review=dialog("orderDialog");
        FxTestSupport.fx(()->{press(review,"Confirm order");return null;});FxTestSupport.awaitIdle(scenario.session());
    }
    private static void submitSelfMatch(Scenario scenario)throws Exception{
        FxTestSupport.fx(()->{((Button)scenario.root().lookup("#submitOrder")).fire();return null;});
        var input=dialog("orderInput");
        FxTestSupport.fx(()->{
            ((ComboBox<?>)input.lookup("#orderSide")).getSelectionModel().select(0);
            ((TextField)input.lookup("#orderQuantity")).setText("1");
            ((TextField)input.lookup("#orderPrice")).setText("0.5");press(input,"Review order");return null;
        });
        FxTestSupport.awaitIdle(scenario.session());
    }
    private static void navigate(Scenario scenario,Navigation navigation)throws Exception{
        FxTestSupport.fx(()->{
            switch(navigation){
                case TOP_LEVEL->((TabPane)scenario.root().lookup("#navigation")).getSelectionModel().select(0);
                case SECTION->((ToggleButton)scenario.root().lookup("#availableSection")).fire();
                case EVENT->((ToggleButton)scenario.root().lookup("#user-event-2")).fire();
                case DETAIL_TAB->FxTestSupport.userDetail(scenario.root(),"Order books");
                case USER->FxTestSupport.chooseUser(scenario.root(),1);
            }
            return null;
        });
    }
    private static void returnToOwnerEventOne(Scenario scenario,Navigation navigation)throws Exception{
        FxTestSupport.fx(()->{
            if(navigation==Navigation.TOP_LEVEL)((TabPane)scenario.root().lookup("#navigation")).getSelectionModel().select(1);
            if(navigation==Navigation.USER)FxTestSupport.chooseUser(scenario.root(),0);
            ((ToggleButton)scenario.root().lookup("#ownedSection")).fire();
            ((ToggleButton)scenario.root().lookup("#user-event-1")).fire();
            scenario.root().applyCss();scenario.root().layout();return null;
        });
    }
    private static DialogPane dialog(String id)throws Exception{
        for(int i=0;i<100;i++){
            var pane=FxTestSupport.fx(()->Window.getWindows().stream().filter(w->w.getScene()!=null&&id.equals(w.getScene().getRoot().getId()))
                    .map(w->(DialogPane)w.getScene().getRoot()).findFirst().orElse(null));
            if(pane!=null)return pane;Thread.sleep(20);
        }
        throw new AssertionError("Missing dialog "+id);
    }
    private static void press(DialogPane pane,String text){
        var type=pane.getButtonTypes().stream().filter(t->t.getText().equals(text)).findFirst().orElseThrow();
        ((Button)pane.lookupButton(type)).fire();
    }
    private static void close(Scenario scenario)throws Exception{
        FxTestSupport.fx(()->{
            for(var window:List.copyOf(Window.getWindows()))if(window!=scenario.stage())window.hide();
            scenario.session().close();scenario.stage().close();return null;
        });
    }
}
