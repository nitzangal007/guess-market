package guessmarket.ui.javafx;
import guessmarket.dto.world.*;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

final class OrderDialog {
    enum Decision { CONFIRM, BACK, CANCEL }
    private OrderDialog(){}
    static void prompt(Window owner,EventSnapshot event,UserSnapshot user,OrderRequest initial,Consumer<OrderRequest> selected){
        Dialog<OrderRequest> dialog=new Dialog<>();
        PurchaseDialog.setup(dialog,owner,"Submit order",event.name()+" | Acting as "+user.name(),"orderInput");
        ComboBox<String> option=new ComboBox<>();option.setId("orderOption");option.getItems().setAll(event.optionLabels());option.setMaxWidth(Double.MAX_VALUE);
        ComboBox<OrderSide> side=new ComboBox<>();side.setId("orderSide");side.getItems().setAll(OrderSide.values());side.setMaxWidth(Double.MAX_VALUE);
        TextField quantity=new TextField(initial==null?"1":Integer.toString(initial.quantity()));quantity.setId("orderQuantity");
        TextField price=new TextField(initial==null?"0.50":OrderBookView.exact(initial.limitPrice()));price.setId("orderPrice");
        option.getSelectionModel().select(initial==null?0:initial.optionNumber()-1);side.setValue(initial==null?OrderSide.BUY:initial.side());
        Label available=EventDetailsView.label("","muted");available.setId("orderAvailable");
        Runnable update=()->{
            int index=option.getSelectionModel().getSelectedIndex();
            var p=event.orderBook().orElseThrow().positions().stream().filter(v->v.userName().equals(user.name())).findFirst();
            int shares=p.map(v->index==0?v.availableOne():v.availableTwo()).orElse(0);
            available.setText("Available to SELL for this outcome: "+shares+". Waiting BUY orders do not reserve cash.");
        };
        option.valueProperty().addListener((o,b,a)->update.run());update.run();
        Label error=EventDetailsView.label("","warning");error.setId("orderInputError");
        error.visibleProperty().bind(error.textProperty().isNotEmpty());error.managedProperty().bind(error.visibleProperty());error.setMinHeight(Region.USE_PREF_SIZE);
        Runnable clear=()->{try{parse(event,user,option,side,quantity,price);error.setText("");}catch(IllegalArgumentException ignored){}};
        quantity.textProperty().addListener((o,b,a)->clear.run());price.textProperty().addListener((o,b,a)->clear.run());
        VBox fields=new VBox(9,EventDetailsView.label("Outcome","muted"),option,EventDetailsView.label("Side","muted"),side,
                EventDetailsView.label("Quantity, positive whole shares","muted"),quantity,
                EventDetailsView.label("Unit limit, from 0 through "+(((OrderBookConfiguration)event.pricing()).d()-0.01),"muted"),price,available,error);
        ScrollPane scroll=new ScrollPane(fields);scroll.setFitToWidth(true);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);scroll.setPrefViewportHeight(440);
        dialog.getDialogPane().setContent(scroll);
        ButtonType review=new ButtonType("Review order",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(review,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(review).addEventFilter(ActionEvent.ACTION,action->{
            try{parse(event,user,option,side,quantity,price);}catch(IllegalArgumentException failure){error.setText(failure.getMessage());action.consume();}
        });
        dialog.setResultConverter(button->button==review?parse(event,user,option,side,quantity,price):null);
        dialog.setOnHidden(a->{if(dialog.getResult()!=null)selected.accept(dialog.getResult());});dialog.show();
    }
    private static OrderRequest parse(EventSnapshot event,UserSnapshot user,ComboBox<String> option,ComboBox<OrderSide> side,TextField quantity,TextField price){
        try{
            int q=Integer.parseInt(quantity.getText().trim());BigDecimal p=new BigDecimal(price.getText().trim());
            BigDecimal max=BigDecimal.valueOf(((OrderBookConfiguration)event.pricing()).d()).subtract(new BigDecimal("0.01"));
            if(q<=0||p.signum()<0||p.compareTo(max)>0||option.getSelectionModel().getSelectedIndex()<0||side.getValue()==null)throw new NumberFormatException();
            return new OrderRequest(user.name(),event.id(),option.getSelectionModel().getSelectedIndex()+1,side.getValue(),q,p);
        }catch(NumberFormatException failure){throw new IllegalArgumentException("Enter a positive whole quantity and a decimal price within the displayed range.");}
    }
    static void show(Window owner,EventSnapshot event,OrderPreview preview,Consumer<Decision> decision){
        Dialog<Decision> dialog=new Dialog<>();
        var request=preview.request();
        PurchaseDialog.setup(dialog,owner,"Review order",event.name()+" | Acting as "+request.userName(),"orderDialog");
        dialog.getDialogPane().setPrefWidth(940);
        VBox fields=new VBox(12,
                PurchaseDialog.field("Order",request.side()+" / "+event.optionLabels().get(request.optionNumber()-1)+" / "+request.quantity()+" shares","orderSelection"),
                PurchaseDialog.field("Unit limit",OrderBookView.exact(request.limitPrice()),"orderLimit"),
                EventDetailsView.label("Planned executions, ordinary first, then mint. Mint has one purchase row per buyer; fees shown belong to that row's buyer.","muted"));
        var fills=OrderBookView.tradeTable(event);fills.setId("orderFills");fills.getItems().setAll(preview.fills());
        if(preview.fills().isEmpty())fields.getChildren().add(EventDetailsView.label("No immediate execution. The unfilled order waits unless final blocking cancels it. No BUY cash is reserved.","muted"));
        else {OrderBookView.table(fills,240);fields.getChildren().add(fills);}
        fields.getChildren().addAll(PurchaseDialog.field("Unfilled quantity before cancellation",Integer.toString(preview.incomingRemaining()),"orderRemaining"),
                PurchaseDialog.field("Current cash",EventDetailsView.money(preview.balanceBefore()),"orderBefore"),
                PurchaseDialog.field("Final cash after this order",EventDetailsView.money(preview.balanceAfter()),"orderAfter"));
        String consequence=(preview.blockedAfter()?"This account ends blocked. Buying and opening remain blocked until a positive receipt leaves cash above zero. You can inspect records and close events you own.":"This account remains eligible after the complete order.")
                +" Cancelled waiting orders across affected accounts: "+preview.cancelledOrderCount()+". Released SELL shares: "+preview.releasedReservationQuantity()+".";
        Label warning=EventDetailsView.label(consequence,preview.blockedAfter()||preview.cancelledOrderCount()>0?"warning":"notice");warning.setId("orderWarning");warning.setMinHeight(Region.USE_PREF_SIZE);
        ScrollPane scroll=new ScrollPane(fields);scroll.setFitToWidth(true);scroll.setMinHeight(0);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        BorderPane content=new BorderPane(scroll);content.setBottom(warning);content.setPrefHeight(450);dialog.getDialogPane().setContent(content);
        ButtonType confirm=new ButtonType("Confirm order",ButtonBar.ButtonData.OK_DONE),back=new ButtonType("Back to edit",ButtonBar.ButtonData.BACK_PREVIOUS);
        dialog.getDialogPane().getButtonTypes().setAll(confirm,back,ButtonType.CANCEL);
        dialog.setResultConverter(button->button==confirm?Decision.CONFIRM:button==back?Decision.BACK:Decision.CANCEL);
        dialog.setOnHidden(a->decision.accept(dialog.getResult()==null?Decision.CANCEL:dialog.getResult()));dialog.show();
    }
}
