package guessmarket.ui.javafx;

import guessmarket.dto.world.EventSnapshot;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Inspection only: activating a row never opens or funds a market. */
final class EventRowView extends ToggleButton {
    EventRowView(EventSnapshot event,FlowPane rows,ToggleGroup selection,Consumer<EventSnapshot> view,String prefix) {
        setId(prefix+event.id());setToggleGroup(selection);getStyleClass().add("event-row");
        VBox content=new VBox(6,EventDetailsView.label("EVENT "+event.id()+"  /  "+EventDetailsView.status(event)
                +"  /  "+EventDetailsView.method(event),"status"),
                EventDetailsView.label(event.name(),"card-title"),
                EventDetailsView.label(EventDetailsView.commission(event)+"  |  Contract balance "
                        +EventDetailsView.money(event.contractBalance()),"muted"),
                EventDetailsView.label("View event  >","row-action"));
        setGraphic(content);setAccessibleText("Event "+event.id()+", "+event.name()+", "+EventDetailsView.status(event)
                +", "+EventDetailsView.method(event)+", View event");
        setMaxWidth(Double.MAX_VALUE);setMinWidth(0);
        prefWidthProperty().bind(rows.widthProperty().subtract(2));
        content.prefWidthProperty().bind(prefWidthProperty().subtract(40));
        setOnAction(action->{setSelected(true);view.accept(event);});
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,key->{
            if(key.getCode()==KeyCode.UP||key.getCode()==KeyCode.DOWN||key.getCode()==KeyCode.LEFT||key.getCode()==KeyCode.RIGHT){
                int direction=key.getCode()==KeyCode.UP||key.getCode()==KeyCode.LEFT?-1:1;
                int index=rows.getChildren().indexOf(this);
                int next=Math.max(0,Math.min(rows.getChildren().size()-1,index+direction));
                ToggleButton target=(ToggleButton)rows.getChildren().get(next);
                target.setSelected(true);target.requestFocus();key.consume();
            } else if(key.getCode()==KeyCode.ENTER){fire();key.consume();}
        });
    }
    void relationship(String text,String markerId) {
        Label marker=EventDetailsView.label(text,"relationship");marker.setId(markerId);
        ((VBox)getGraphic()).getChildren().add(1,marker);
        setAccessibleText(getAccessibleText()+", "+text);
    }
}
