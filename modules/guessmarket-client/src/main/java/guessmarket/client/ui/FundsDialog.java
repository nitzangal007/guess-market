package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import java.math.BigDecimal;
import java.util.Objects;
public final class FundsDialog extends Dialog<String> {
    public FundsDialog(RemoteMarketSession session){
        setTitle("Load funds");setHeaderText("Confirm a funds credit");var amount=new TextField();amount.setId("funds-amount");amount.setPromptText("Positive amount");amount.setAccessibleText("Funds amount");var error=UiTables.label("","error");error.setVisible(false);error.setManaged(false);
        getDialogPane().setContent(new VBox(12,UiTables.label("Enter the amount to add. The server will return the actual credited amount and account balance.","muted"),UiTables.label("Amount",null),amount,error));
        var confirm=new ButtonType("Confirm entered amount",ButtonBar.ButtonData.OK_DONE);getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,confirm);var button=getDialogPane().lookupButton(confirm);button.disableProperty().bind(session.connectedProperty().not().or(session.busyProperty()).or(session.unknownProperty()).or(session.loggedInProperty().not()));
        button.addEventFilter(ActionEvent.ACTION,event->{try{var value=new BigDecimal(amount.getText());if(value.signum()<=0||!Double.isFinite(value.doubleValue())||value.doubleValue()==0||!session.mutationAllowed())throw new NumberFormatException();}catch(NumberFormatException failure){event.consume();error.setText("Enter a positive finite amount while connected.");error.setManaged(true);error.setVisible(true);amount.requestFocus();}});
        setResultConverter(type->type==confirm?amount.getText():null);setOnShown(event->amount.requestFocus());getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/guessmarket/client/client.css")).toExternalForm());
        javafx.beans.value.ChangeListener<Boolean> expired=(o,a,b)->{if(!b)close();};session.loggedInProperty().addListener(expired);setOnHidden(e->session.loggedInProperty().removeListener(expired));
    }
}
