package guessmarket.ui.javafx;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

final class InputDialogPresentationTest {
    @BeforeAll static void start() throws Exception {FxTestSupport.start();}
    private EventSnapshot event(){
        return new EventSnapshot(1,"Review fixture","Details",List.of("Yes","No"),5,CommissionMode.ON_PURCHASE,
                WorldEventStatus.ACTIVE,"Owner",100,new LmsrConfiguration(100));
    }
    private DialogPane pane(String id){
        return (DialogPane)Window.getWindows().stream().filter(w->w.getScene()!=null
                &&id.equals(w.getScene().getRoot().getId())).findFirst().orElseThrow().getScene().getRoot();
    }
    private Button review(DialogPane pane){
        return (Button)pane.lookupButton(pane.getButtonTypes().stream().filter(t->t!=ButtonType.CANCEL).findFirst().orElseThrow());
    }
    private Label error(DialogPane pane){
        return ((VBox)pane.getContent()).getChildren().stream().filter(n->n instanceof Label l
                &&l.getStyleClass().contains("warning")).map(n->(Label)n).reduce((a,b)->b).orElseThrow();
    }
    private void save(DialogPane pane,String name) throws Exception {
        pane.applyCss();pane.layout();
        Path path=Path.of("private/coordination/assignment-2/ui-evidence-input-fix-"+System.getProperty("glass.win.uiScale","native"),name);
        Files.createDirectories(path.getParent());
        var parameters=new javafx.scene.SnapshotParameters();
        double scale=pane.getScene().getWindow().getOutputScaleX();
        parameters.setTransform(javafx.scene.transform.Transform.scale(scale,scale));
        var snap=pane.snapshot(parameters,null);
        java.awt.image.BufferedImage image=new java.awt.image.BufferedImage((int)snap.getWidth(),(int)snap.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,snap.getPixelReader().getArgb(x,y));
        javax.imageio.ImageIO.write(image,"png",path.toFile());
        System.out.println(name+" logical="+pane.getWidth()+"x"+pane.getHeight()+" outputScale="+pane.getScene().getWindow().getOutputScaleX());
    }
    private void close(DialogPane pane){((Button)pane.lookupButton(ButtonType.CANCEL)).fire();}
    @Test void purchaseErrorHiddenUntilInvalidAndClearsOnValidInput() throws Exception {
        FxTestSupport.fx(()->{
            AtomicReference<PurchaseDialog.Choice> result=new AtomicReference<>();
            PurchaseDialog.prompt(null,event(),"Owner",result::set);var pane=pane("purchaseInput");
            try{
                pane.applyCss();pane.layout();Label error=error(pane);
                save(pane,"purchase-valid.png");
                assertFalse(error.isVisible(),"Empty purchase validation must be hidden");
                assertFalse(error.isManaged(),"Empty purchase validation must reserve no space");
                var quantity=(TextField)pane.lookup("#purchaseQuantity");
                quantity.setText("0");review(pane).fire();pane.applyCss();pane.layout();
                assertTrue(error.isVisible());assertTrue(error.isManaged());assertFalse(error.getText().isBlank());
                assertNull(result.get());save(pane,"purchase-invalid.png");
                quantity.setText("2");
                assertFalse(error.isVisible());assertFalse(error.isManaged());
                review(pane).fire();assertEquals(new PurchaseDialog.Choice(1,2),result.get());
            }finally{if(pane.getScene()!=null&&pane.getScene().getWindow().isShowing())close(pane);}
            return null;
        });
    }
    @Test void closeWarningFullyRendersAndValidationClearsOnSelection() throws Exception {
        FxTestSupport.fx(()->{
            AtomicReference<Integer> result=new AtomicReference<>();
            CloseDialog.prompt(null,event(),"Owner",result::set);var pane=pane("closeInput");
            try{
                pane.setPrefSize(680,420);((Stage)pane.getScene().getWindow()).sizeToScene();pane.applyCss();pane.layout();
                var warning=((VBox)pane.getContent()).getChildren().stream().filter(n->n instanceof Label l
                        &&l.getText().startsWith("Closing ends")).map(n->(Label)n).findFirst().orElseThrow();
                save(pane,"close-valid.png");
                assertTrue(warning.getHeight()+1>=warning.prefHeight(warning.getWidth()),"Full close warning must fit");
                assertEquals(warning.getText(),((Text)warning.lookup(".text")).getText(),"Warning must not be ellipsized");
                Label error=error(pane);assertFalse(error.isVisible());assertFalse(error.isManaged());
                review(pane).fire();pane.applyCss();pane.layout();
                assertTrue(error.isVisible());assertTrue(error.isManaged());assertNull(result.get());
                assertTrue(error.localToScene(error.getBoundsInLocal()).getMaxY()
                        <=review(pane).localToScene(review(pane).getBoundsInLocal()).getMinY(),"Error and buttons must not overlap");
                save(pane,"close-invalid.png");
                ((ComboBox<?>)pane.lookup("#winningOption")).getSelectionModel().select(1);
                assertFalse(error.isVisible());assertFalse(error.isManaged());
                review(pane).fire();assertEquals(2,result.get());
            }finally{if(pane.getScene()!=null&&pane.getScene().getWindow().isShowing())close(pane);}
            return null;
        });
    }
}

