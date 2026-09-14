package guessmarket.ui.javafx;

import guessmarket.dto.world.ClosePreview;
import guessmarket.dto.world.EventSnapshot;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

final class CloseDialog {
    private CloseDialog() {}
    static void prompt(Window owner,EventSnapshot event,String user,Consumer<Integer> selected) {
        Dialog<Integer> dialog=new Dialog<>();
        PurchaseDialog.setup(dialog,owner,"Close event",event.name()+" | Market maker: "+user,"closeInput");
        ComboBox<String> winner=new ComboBox<>();winner.setId("winningOption");
        winner.getItems().setAll(event.optionLabels());winner.setPromptText("Choose the winning outcome");
        winner.setMaxWidth(Double.MAX_VALUE);
        Label error=EventDetailsView.label("","warning");
        error.setId("closeInputError");
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        error.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        winner.getSelectionModel().selectedIndexProperty().addListener((observable,before,after)->{
            if(after.intValue()>=0)error.setText("");
        });
        Label warning=EventDetailsView.label("Closing ends trading permanently. The selected outcome determines the payouts.","warning");
        warning.setId("closeInputWarning");
        warning.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        dialog.getDialogPane().setContent(new VBox(10,winner,
                warning,error));
        ButtonType review=new ButtonType("Review payouts",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(review,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(review).addEventFilter(ActionEvent.ACTION,action->{
            if(winner.getSelectionModel().getSelectedIndex()<0){error.setText("Choose the winning outcome.");action.consume();}
        });
        dialog.setResultConverter(button->button==review?winner.getSelectionModel().getSelectedIndex()+1:null);
        dialog.setOnHidden(action->{if(dialog.getResult()!=null)selected.accept(dialog.getResult());});
        dialog.show();
    }
    static void show(Window owner,ClosePreview preview,Consumer<Boolean> decision) {
        show(owner,preview,false,decision);
    }
    static void show(Window owner,ClosePreview preview,boolean orderBook,Consumer<Boolean> decision) {
        Dialog<Boolean> dialog=new Dialog<>();
        PurchaseDialog.setup(dialog,owner,"Review payouts",preview.eventName()+" | Acting as "+preview.actingUser()
                +" | Winner: "+preview.winningLabel(),"closeDialog");
        VBox rows=new VBox(12);
        rows.getChildren().add(EventDetailsView.label("Closing ends trading permanently.","warning"));
        if(preview.payments().isEmpty())rows.getChildren().add(EventDetailsView.label(orderBook?"No winning holdings. No payout is due.":"No winning shares. The contract funds return to the market maker.","muted"));
        for(var payment:preview.payments())rows.getChildren().add(PurchaseDialog.field(payment.userName(),
                "Shares: "+payment.winningShares()+" | Gross: "+EventDetailsView.money(payment.grossPayout())
                +" | Fee: "+EventDetailsView.money(payment.commission())+" | Net payout: "+EventDetailsView.money(payment.netPayout()),"payout-"+payment.userName()));
        rows.getChildren().addAll(PurchaseDialog.field("Total gross payouts",EventDetailsView.money(preview.totalGrossPayout()),"closingGross"),
                PurchaseDialog.field("Closing fees to market maker",EventDetailsView.money(preview.totalCommission()),"closingFees"));
        if(!orderBook)rows.getChildren().add(PurchaseDialog.field("Unused subsidy returned to market maker",EventDetailsView.money(preview.subsidyRefund()),"closingRefund"));
        else rows.getChildren().add(EventDetailsView.label("All remaining orders will be cancelled and SELL reservations released. Winning holdings consume the pair backing; no unused subsidy is returned.","muted"));
        ScrollPane scroll=new ScrollPane(rows);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(380);
        dialog.getDialogPane().setContent(scroll);
        PurchaseDialog.confirm(dialog,"Close event and pay out",decision);
    }
}
