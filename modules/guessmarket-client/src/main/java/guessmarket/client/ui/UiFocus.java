package guessmarket.client.ui;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.beans.value.ChangeListener;
final class UiFocus {
    private UiFocus() {}
    static void keepVisible(ScrollPane scroll){
        ChangeListener<Node> focused=(o,a,node)->{
            Node content=scroll.getContent();if(node==null||content==null)return;Node ancestor=node;while(ancestor!=null&&ancestor!=content)ancestor=ancestor.getParent();if(ancestor==null)return;
            var bounds=content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));double viewport=scroll.getViewportBounds().getHeight(),range=content.getLayoutBounds().getHeight()-viewport;if(range<=0)return;double top=scroll.getVvalue()*range;
            if(bounds.getMinY()<top)scroll.setVvalue(Math.max(0,bounds.getMinY()/range));else if(bounds.getMaxY()>top+viewport)scroll.setVvalue(Math.min(1,(bounds.getMaxY()-viewport)/range));
        };
        scroll.sceneProperty().addListener((o,oldScene,newScene)->{if(oldScene!=null)oldScene.focusOwnerProperty().removeListener(focused);if(newScene!=null)newScene.focusOwnerProperty().addListener(focused);});
    }
}
