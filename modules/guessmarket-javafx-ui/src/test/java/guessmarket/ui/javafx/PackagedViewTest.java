package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class PackagedViewTest {
    @Test void manyUserPickerPreservesFullNamesAndCancel(@TempDir Path directory) throws Exception {
        String fullName="Alexandra Elizabeth Montgomery with a long account name";
        String users="<GM-user name=\""+fullName+"\"><initial-cash>100</initial-cash></GM-user>";
        for(int i=0;i<30;i++)users+="<GM-user name=\"Sample User "+i+"\"><initial-cash>100</initial-cash></GM-user>";
        Path input=directory.resolve("many users.xml");
        Files.writeString(input,Files.readString(fixture("small.xml")).replace("</GM-users>",users+"</GM-users>"));
        WorldSession session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> stage=new AtomicReference<>();
        try {
            Parent root=open(session,stage);
            FxTestSupport.fx(()->{session.load(input);return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                tab(root,1);FxTestSupport.chooseUser(root,3);assertEquals(fullName,text(root,"userName"));
                ((javafx.scene.control.Button)root.lookup("#changeUser")).fire();
                var pane=(javafx.scene.control.DialogPane)javafx.stage.Window.getWindows().stream().filter(w->w.getScene()!=null
                        &&"userPicker".equals(w.getScene().getRoot().getId())).findFirst().orElseThrow().getScene().getRoot();
                assertEquals(34,((ListView<?>)pane.lookup("#userList")).getItems().size());
                save(pane,"many-user-picker.png");
                ((javafx.scene.control.Button)pane.lookupButton(javafx.scene.control.ButtonType.CANCEL)).fire();
                assertEquals(fullName,text(root,"userName"));stage.get().setWidth(1024);stage.get().setHeight(768);
                System.out.println("TERMINAL native output scale: "+stage.get().getOutputScaleX()+" x "+stage.get().getOutputScaleY());
                return null;
            });
            Thread.sleep(150);
            FxTestSupport.fx(()->{save(root,"long-user-1024.png");return null;});
        } finally {FxTestSupport.fx(()->{session.close();if(stage.get()!=null)stage.get().close();return null;});}
    }
    @Test void usersStartAvailableAndSearchChangesRealIdentityWithoutActivating() throws Exception {
        WorldSession session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> stage=new AtomicReference<>();
        try {
            Parent root=open(session,stage);
            FxTestSupport.fx(()->{session.load(fixture("multiple.xml"));return null;});FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                tab(root,1);
                assertNotNull(root.lookup("#changeUser"),"Approved searchable user context replaces the sidebar");
                assertTrue(((ToggleButton)root.lookup("#availableSection")).isSelected());
                assertNull(root.lookup("#user-detailName"));
                ((javafx.scene.control.Button)root.lookup("#changeUser")).fire();
                var picker=javafx.stage.Window.getWindows().stream().filter(w->w.getScene()!=null
                        && w.getScene().getRoot() instanceof javafx.scene.control.DialogPane
                        && "userPicker".equals(w.getScene().getRoot().getId())).findFirst().orElseThrow().getScene().getRoot();
                ((javafx.scene.control.TextField)picker.lookup("#userSearch")).setText("no such user");
                assertEquals(0,((ListView<?>)picker.lookup("#userList")).getItems().size());
                ((javafx.scene.control.TextField)picker.lookup("#userSearch")).setText("tik");
                var results=(ListView<?>)picker.lookup("#userList");assertEquals(1,results.getItems().size());
                results.getSelectionModel().selectFirst();
                var pane=(javafx.scene.control.DialogPane)picker;
                var choose=pane.getButtonTypes().stream().filter(b->b!=javafx.scene.control.ButtonType.CANCEL).findFirst().orElseThrow();
                ((javafx.scene.control.Button)pane.lookupButton(choose)).fire();
                assertEquals("Tikva",text(root,"userName"));
                assertNull(root.lookup("#user-detailName"));
                ((ToggleButton)root.lookup("#ownedSection")).fire();
                assertEquals(3,((FlowPane)root.lookup("#userEventRows")).getChildren().size());
                assertNull(root.lookup("#user-detailName"));
                return null;
            });
        } finally {FxTestSupport.fx(()->{session.close();if(stage.get()!=null)stage.get().close();return null;});}
    }
    @Test void focusedEventsBackPreservesFiltersAndNeverMutates() throws Exception {
        WorldSession session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> stage=new AtomicReference<>();
        try {
            Parent root=open(session,stage);
            FxTestSupport.fx(()->{session.load(fixture("multiple.xml"));return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(()->{
                var before=session.worldProperty().get();
                assertNull(root.lookup("#detailName"),"A list selection must not automatically activate details");
                var method=(javafx.scene.control.ComboBox<?>)root.lookup("#methodFilter");
                method.getSelectionModel().select(1);
                assertEquals("2 / 4 events",text(root,"eventCount"));
                var first=(ToggleButton)root.lookup("#event-card-1");
                javafx.event.Event.fireEvent(first,new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,
                        "","",javafx.scene.input.KeyCode.DOWN,false,false,false,false));
                assertTrue(((ToggleButton)root.lookup("#event-card-4")).isSelected());
                assertNull(root.lookup("#detailName"));
                javafx.event.Event.fireEvent(root.lookup("#event-card-4"),new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,
                        "","",javafx.scene.input.KeyCode.ENTER,false,false,false,false));
                assertFalse(root.lookup("#eventCatalogue").isVisible());
                assertEquals("Will it rain tomorrow ?",text(root,"detailName"));
                var sections=(TabPane)root.lookup("#eventSections");
                assertEquals(java.util.List.of("Overview","Trade history","Settlement"),
                        sections.getTabs().stream().map(javafx.scene.control.Tab::getText).toList());
                assertNull(root.lookup("#buyShares"));assertNull(root.lookup("#closeEvent"));
                ((javafx.scene.control.Button)root.lookup("#eventsBack")).fire();
                assertTrue(root.lookup("#eventCatalogue").isVisible());
                assertEquals("LMSR",method.getValue());
                assertTrue(((ToggleButton)root.lookup("#event-card-4")).isSelected());
                assertSame(before,session.worldProperty().get());return null;
            });
        } finally {FxTestSupport.fx(()->{session.close();if(stage.get()!=null)stage.get().close();return null;});}
    }
    @Test void eventFiltersCombineAndAllRestoresTheListWithoutChangingWorld() throws Exception {
        WorldSession session=FxTestSupport.fx(()->new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> ref=new AtomicReference<>();
        try {
            Parent root=open(session,ref);
            FxTestSupport.fx(()->{session.load(fixture("multiple.xml"));return null;});
            FxTestSupport.awaitIdle(session);
            var before=FxTestSupport.fx(()->session.worldProperty().get());
            FxTestSupport.fx(()->{
                var method=(javafx.scene.control.ComboBox<?>)root.lookup("#methodFilter");
                var status=(javafx.scene.control.ComboBox<?>)root.lookup("#statusFilter");
                var fee=(javafx.scene.control.ComboBox<?>)root.lookup("#commissionFilter");
                method.getSelectionModel().select(1);
                status.getSelectionModel().select(1);
                fee.getSelectionModel().select(2);
                assertEquals(1,((FlowPane)root.lookup("#eventCards")).getChildren().size());
                status.getSelectionModel().select(2);
                assertEquals(0,((FlowPane)root.lookup("#eventCards")).getChildren().size());
                method.getSelectionModel().selectFirst();status.getSelectionModel().selectFirst();fee.getSelectionModel().selectFirst();
                assertEquals(4,((FlowPane)root.lookup("#eventCards")).getChildren().size());
                ((ToggleButton)root.lookup("#event-card-1")).fire();
                assertSame(before,session.worldProperty().get());return null;
            });
        } finally {FxTestSupport.fx(()->{session.close();if(ref.get()!=null)ref.get().close();return null;});}
    }
    @BeforeAll static void startFx() throws Exception { FxTestSupport.start(); }
    private static Path fixture(String name) {
        return Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures", name);
    }
    private Parent open(WorldSession session, AtomicReference<Stage> ref) throws Exception {
        return FxTestSupport.fx(() -> {
            var resource=getClass().getResource("/guessmarket/ui/javafx/main.fxml");
            assertNotNull(resource); assertEquals("jar",resource.getProtocol());
            for(String name:new String[]{"main.css","users.fxml"}) {
                var asset=getClass().getResource("/guessmarket/ui/javafx/"+name);
                assertNotNull(asset); assertEquals("jar",asset.getProtocol());
            }
            assertEquals("jar",getClass().getResource("/guessmarket/engine/xml/ex2/GM-EX2-Schema.xsd").getProtocol());
            FXMLLoader loader=new FXMLLoader(resource);
            loader.setControllerFactory(type -> {
                try {return type.getDeclaredConstructor(WorldSession.class).newInstance(session);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
            Parent root=loader.load();
            Stage stage=new Stage(); ref.set(stage);
            stage.setTitle("Guess Market EX2 automated verification");
            stage.setScene(new Scene(root,1120,820)); stage.show();
            root.applyCss(); root.layout(); return root;
        });
    }
    private static String text(Parent root,String id){return ((Label)root.lookup("#"+id)).getText();}
    private static void tab(Parent root,int index){
        ((TabPane)root.lookup("#navigation")).getSelectionModel().select(index);
        root.applyCss(); root.layout();
    }

    @Test void bothViewsReplaceTogetherAndSurviveSemanticErrors(@TempDir Path directory) throws Exception {
        WorldSession session=FxTestSupport.fx(() -> new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> ref=new AtomicReference<>();
        try {
            Parent root=open(session,ref);
            assertTrue(FxTestSupport.fx(() -> ((TabPane)root.lookup("#navigation")).getTabs().get(1).isDisable()));
            Path input=Files.copy(fixture("multiple.xml"),directory.resolve("EX2 world with spaces.xml"));
            FxTestSupport.fx(() -> {session.load(input); return null;});
            assertTrue(FxTestSupport.fx(() -> root.lookup("#loadButton").isDisabled()));
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(() -> {
                assertEquals(4,((FlowPane)root.lookup("#eventCards")).getChildren().size());
                ((ToggleButton)root.lookup("#event-card-1")).fire();
                root.applyCss();root.layout();
                assertEquals("Mujtaba is Dead",text(root,"detailName"));
                assertEquals("Hell Yea !",text(root,"optionLabel1"));
                assertEquals("Contract balance",text(root,"accountMeaning"));
                ((ToggleButton)root.lookup("#event-card-2")).fire();
                assertEquals("World Cap Winner",text(root,"detailName"));
                assertEquals("Order Book",text(root,"methodName"));
                save(root,"events.png");
                tab(root,1);
                assertEquals("Avrum",text(root,"userName"));
                assertEquals("1,000.00",text(root,"userBalance"));
                FxTestSupport.chooseUser(root,1);
                ((ToggleButton)root.lookup("#ownedSection")).fire();
                assertEquals("Tikva",text(root,"userName"));
                assertEquals("10,000.00",text(root,"userBalance"));
                assertEquals(3,((FlowPane)root.lookup("#userEventRows")).getChildren().size());
                ((ToggleButton)root.lookup("#user-event-4")).fire();
                assertEquals("Will it rain tomorrow ?",text(root,"user-detailName"));
                save(root,"users.png"); return null;
            });
            String previous=FxTestSupport.fx(() -> session.loadedPathProperty().get());
            for(String invalid:new String[]{"error-2.xml","error-3.xml"}) {
                FxTestSupport.fx(() -> {
                    session.load(fixture(invalid));
                    assertTrue(root.lookup("#changeUser").isDisabled());
                    assertTrue(root.lookup("#userEventRows").isDisabled());
                    return null;
                });
                FxTestSupport.awaitIdle(session);
                FxTestSupport.fx(() -> {
                    assertEquals(previous,session.loadedPathProperty().get());
                    assertEquals("Tikva",text(root,"userName"));
                    assertEquals("Will it rain tomorrow ?",text(root,"user-detailName"));
                    assertTrue(root.lookup("#loadError").isVisible());
                    assertFalse(root.lookup("#changeUser").isDisabled()); return null;
                });
            }
            FxTestSupport.fx(() -> {
                session.load(null);
                FxTestSupport.chooseUser(root,2);
                assertEquals("Menash",text(root,"userName"));
                assertEquals("100.00",text(root,"userBalance"));
                assertEquals(0,((FlowPane)root.lookup("#userEventRows")).getChildren().size());
                assertTrue(root.lookup("#userEmpty").isVisible());
                tab(root,0); assertEquals("World Cap Winner",text(root,"detailName"));
                session.load(fixture("small.xml")); return null;
            });
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(() -> {
                assertEquals(2,((FlowPane)root.lookup("#eventCards")).getChildren().size());
                ((ToggleButton)root.lookup("#event-card-1")).fire();
                assertEquals("Mujtaba is Dead",text(root,"detailName"));
                tab(root,1); assertEquals("Avrum",text(root,"userName"));
                assertFalse(root.lookup("#loadError").isVisible());
                ref.get().setWidth(640); ref.get().setHeight(700); return null;
            });
            Thread.sleep(200);
            FxTestSupport.fx(() -> {save(root,"users-narrow.png"); return null;});
        } finally {FxTestSupport.fx(() -> {session.close(); if(ref.get()!=null)ref.get().close(); return null;});}
    }

    @Test void longEventNameStillGrowsItsCard(@TempDir Path directory) throws Exception {
        String name="A detailed event title for a long-running question ".repeat(5).trim();
        Path file=directory.resolve("long names.xml");
        Files.writeString(file,Files.readString(fixture("small.xml")).replace("Mujtaba is Dead",name));
        WorldSession session=FxTestSupport.fx(() -> new WorldSession(new GuessMarketWorldEngineImpl()));
        AtomicReference<Stage> ref=new AtomicReference<>();
        try {
            Parent root=open(session,ref);
            FxTestSupport.fx(() -> {session.load(file); return null;});
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(() -> {
                root.applyCss(); root.layout();
                var row=(ToggleButton)root.lookup("#event-card-1");
                assertTrue(row.getHeight()>=row.getGraphic().getLayoutBounds().getHeight()+30);
                row.fire();
                assertEquals(name,text(root,"detailName"));
                save(root,"long-text.png"); return null;
            });
        } finally {FxTestSupport.fx(() -> {session.close(); if(ref.get()!=null)ref.get().close(); return null;});}
    }
    private static void save(Parent root,String name) throws Exception {
        root.applyCss(); root.layout(); var image=root.snapshot(null,null);
        int w=(int)image.getWidth(),h=(int)image.getHeight();
        BufferedImage png=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Path output=Path.of("private/coordination/assignment-2/ui-evidence-terminal",name);
        Files.createDirectories(output.getParent()); assertTrue(ImageIO.write(png,"png",output.toFile()));
    }
}

