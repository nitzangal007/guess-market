package guessmarket.ui.javafx;

import guessmarket.dto.world.EventSnapshot;
import guessmarket.dto.world.PurchasePreview;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

final class PurchaseDialog {
    record Choice(int option, int quantity) {}
    private PurchaseDialog() {}
    static void prompt(Window owner, EventSnapshot event, String user, Consumer<Choice> selected) {
        Dialog<Choice> dialog=new Dialog<>();
        setup(dialog,owner,"Buy shares",event.name()+" | Acting as "+user,"purchaseInput");
        ComboBox<String> options=new ComboBox<>(); options.getItems().setAll(event.optionLabels());
        options.setId("purchaseOption"); options.getSelectionModel().selectFirst(); options.setMaxWidth(Double.MAX_VALUE);
        TextField quantity=new TextField("1"); quantity.setId("purchaseQuantity");
        Label error=EventDetailsView.label("","warning");
        error.setId("purchaseInputError");
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        error.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Runnable clearIfValid=()->{
            try {
                if(options.getSelectionModel().getSelectedIndex()>=0&&Integer.parseInt(quantity.getText())>0)
                    error.setText("");
            } catch(NumberFormatException ignored) { /* Keep the existing error until input is valid. */ }
        };
        quantity.textProperty().addListener((observable,before,after)->clearIfValid.run());
        options.getSelectionModel().selectedIndexProperty().addListener((observable,before,after)->clearIfValid.run());
        dialog.getDialogPane().setContent(new VBox(10,EventDetailsView.label("Outcome","muted"),options,
                EventDetailsView.label("Number of shares","muted"),quantity,error));
        ButtonType review=new ButtonType("Review purchase",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(review,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(review).addEventFilter(ActionEvent.ACTION,action->{
            try {
                if(options.getSelectionModel().getSelectedIndex()<0 || Integer.parseInt(quantity.getText())<=0)
                    throw new NumberFormatException();
            } catch(NumberFormatException failure) {error.setText("Enter a positive whole number within the supported range.");action.consume();}
        });
        dialog.setResultConverter(button->button==review
                ?new Choice(options.getSelectionModel().getSelectedIndex()+1,Integer.parseInt(quantity.getText())):null);
        dialog.setOnHidden(action->{if(dialog.getResult()!=null)selected.accept(dialog.getResult());});
        dialog.show();
    }
    static void show(Window owner,PurchasePreview preview,Consumer<Boolean> decision) {
        Dialog<Boolean> dialog=new Dialog<>();
        setup(dialog,owner,"Review purchase",preview.eventName()+" | Acting as "+preview.actingUser(),"purchaseDialog");
        VBox fields=new VBox(10,field("Outcome / quantity",preview.optionLabel()+" / "+preview.quantity(),"purchaseSelection"),
                field("Shares cost",EventDetailsView.money(preview.shareCost()),"purchaseCost"),
                field("Purchase commission",EventDetailsView.money(preview.commission()),"purchaseCommission"),
                field("Total debit",EventDetailsView.money(preview.totalDebit()),"purchaseDebit"),
                field("Current balance",EventDetailsView.money(preview.balanceBefore()),"purchaseBefore"),
                field("Balance afterward",EventDetailsView.money(preview.balanceAfter()),"purchaseAfter"));
        if(preview.commissionReceived()!=0)fields.getChildren().add(field("Commission received as market maker",
                EventDetailsView.money(preview.commissionReceived()),"purchaseFeeReceipt"));
        Label warning=EventDetailsView.label(preview.overdraftBeforeReceipts()
                ?(preview.becomesBlocked()
                    ?"The full debit takes cash below zero. Buying and opening are blocked until a receipt leaves your balance above zero. You can inspect records and close events you own."
                    :"The full debit takes cash below zero. Your commission receipt restores positive cash in this purchase, so buying and opening remain available.")
                :"","warning");
        warning.setId("purchaseWarning"); warning.setVisible(preview.overdraftBeforeReceipts());warning.setManaged(preview.overdraftBeforeReceipts());
        warning.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        ScrollPane scroll=new ScrollPane(fields);scroll.setFitToWidth(true);scroll.setMinHeight(0);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        var content=new javafx.scene.layout.BorderPane(scroll);
        content.setBottom(warning);content.setPrefHeight(390);
        dialog.getDialogPane().setContent(content);
        confirm(dialog,"Confirm purchase",decision);
    }
    static void setup(Dialog<?> dialog,Window owner,String title,String header,String id) {
        dialog.initOwner(owner);dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);dialog.setHeaderText(header);
        dialog.getDialogPane().setId(id);dialog.getDialogPane().setPrefWidth(680);
        dialog.getDialogPane().getStylesheets().add(PurchaseDialog.class.getResource("main.css").toExternalForm());
        dialog.setResizable(true);
    }
    static VBox field(String caption,String value,String id) {
        Label label=EventDetailsView.label(value,"subheading");label.setId(id);
        var grid=new javafx.scene.layout.GridPane();grid.setHgap(20);
        var left=new javafx.scene.layout.ColumnConstraints();left.setPercentWidth(50);
        var right=new javafx.scene.layout.ColumnConstraints();right.setPercentWidth(50);
        grid.getColumnConstraints().setAll(left,right);
        grid.add(EventDetailsView.label(caption,"muted"),0,0);grid.add(label,1,0);
        label.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        VBox field=new VBox(grid);field.getStyleClass().add("review-field");return field;
    }
    static void confirm(Dialog<Boolean> dialog,String title,Consumer<Boolean> decision) {
        ButtonType confirm=new ButtonType(title,ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(confirm,ButtonType.CANCEL);
        dialog.setResultConverter(button->button==confirm);
        dialog.setOnHidden(action->decision.accept(Boolean.TRUE.equals(dialog.getResult())));
        dialog.show();
    }
}
