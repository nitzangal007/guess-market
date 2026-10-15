package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import java.util.*;
public final class TradeReviewDialog extends Dialog<Boolean> {
    public TradeReviewDialog(RemoteMarketSession session,ActionPreviewDto preview){
        setTitle("Review "+WireViewMapper.words(preview.action().action().name()));setHeaderText((preview.eventName().isEmpty()?"Event #"+preview.eventId():preview.eventName())+" - "+preview.actingUser());
        var content=new VBox(12);content.getChildren().add(UiTables.label(preview.explanation(),"muted"));
        if(preview.action().optionNumber()!=null)content.getChildren().add(UiTables.label("Option "+preview.action().optionNumber()+(preview.winningLabel()!=null?": "+(preview.winningLabel().isEmpty()?"(empty label)":preview.winningLabel()):""),"subheading"));
        if(preview.action().quantity()!=null)content.getChildren().add(UiTables.label("Quantity: "+preview.action().quantity(),null));
        if(preview.action().limitPrice()!=null)content.getChildren().add(UiTables.label("Side: "+preview.action().side()+"   Exact limit: "+preview.action().limitPrice(),null));
        content.getChildren().add(UiTables.label("Debit "+WireViewMapper.money(preview.totalDebit())+"   Credit "+WireViewMapper.money(preview.totalCredit())+"\nBalance "+WireViewMapper.money(preview.balanceBefore())+" -> "+WireViewMapper.money(preview.balanceAfter())+(preview.blockedAfter()?"\nThe account will be blocked for opening and trading.":""),"subheading"));
        if(preview.incomingRemaining()!=null)content.getChildren().add(UiTables.label("Remaining: "+preview.incomingRemaining()+"   Canceled orders: "+preview.cancelledOrderCount()+"   Released SELL reservations: "+preview.releasedReservationQuantity(),null));
        if(!preview.executions().isEmpty())content.getChildren().add(EventDetailView.executions(preview.executions()));
        if(!preview.payouts().isEmpty())content.getChildren().add(EventDetailView.payouts(preview.payouts()));
        if(preview.totalGrossPayout()!=null)content.getChildren().add(UiTables.label("Gross payout "+WireViewMapper.money(preview.totalGrossPayout())+"   Closing commission "+WireViewMapper.money(preview.totalCommission())+"   Subsidy refund "+WireViewMapper.money(preview.subsidyRefund()),null));
        content.getChildren().add(UiTables.table("Your cash effects",preview.ownCashEffects(),List.of(UiTables.text("Kind",CashEffectDto::kind),UiTables.money("Amount",CashEffectDto::amount),UiTables.money("Journal running balance",CashEffectDto::journalRunningBalance),UiTables.money("Stored balance after transaction",CashEffectDto::storedBalanceAfterTransaction))));
        var warning=UiTables.label("", "error");warning.setManaged(false);warning.setVisible(false);content.getChildren().add(warning);
        var scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.setPrefViewportWidth(620);scroll.setPrefViewportHeight(480);getDialogPane().setContent(scroll);
        var confirm=new ButtonType("Confirm",ButtonBar.ButtonData.OK_DONE);getDialogPane().getButtonTypes().addAll(new ButtonType("Back",ButtonBar.ButtonData.CANCEL_CLOSE),confirm);
        var button=getDialogPane().lookupButton(confirm);button.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(()->!session.actionAllowed(preview),session.dashboardProperty(),session.connectedProperty(),session.busyProperty(),session.unknownProperty(),session.loggedInProperty()));
        button.addEventFilter(ActionEvent.ACTION,event->{if(!session.actionAllowed(preview)){event.consume();warning.setText("Reconnect and resolve the pending operation before confirming.");warning.setManaged(true);warning.setVisible(true);}});
        setResultConverter(type->type==confirm);getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/guessmarket/client/client.css")).toExternalForm());
        javafx.beans.value.ChangeListener<Boolean> expired=(o,a,b)->{if(!b)close();};session.loggedInProperty().addListener(expired);setOnHidden(e->session.loggedInProperty().removeListener(expired));
    }
}
