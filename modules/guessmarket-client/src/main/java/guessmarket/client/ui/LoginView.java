package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Pos;
public final class LoginView extends VBox {
    public LoginView(RemoteMarketSession session){
        setAlignment(Pos.CENTER);setSpacing(16);setMaxWidth(480);getStyleClass().add("panel");
        var name=new TextField();name.setId("login-name");name.setPromptText("Your English name");name.setAccessibleText("Username");var submit=new Button("Log in");submit.setId("login-submit");submit.getStyleClass().add("primary");submit.setDefaultButton(true);submit.disableProperty().bind(session.busyProperty());name.disableProperty().bind(session.busyProperty());
        var error=UiTables.label("","error");error.textProperty().bind(session.noticeProperty());error.visibleProperty().bind(session.noticeProperty().isNotEmpty());error.managedProperty().bind(error.visibleProperty());
        var check=new Button("Check retained login session");check.setOnAction(e->session.checkLogin());check.visibleProperty().bind(session.loginUnknownProperty());check.managedProperty().bind(check.visibleProperty());check.disableProperty().bind(session.busyProperty());
        submit.setOnAction(e->{String input=name.getText();if(input.trim().isEmpty()){name.requestFocus();return;}session.login(input);});
        getChildren().addAll(UiTables.label("GUESS MARKET","brand"),UiTables.label("One market. Your account.","section-title"),UiTables.label("Log in with your name. First login starts an account at zero. Accounts remain until the server restarts.","muted"),UiTables.label("Username",null),name,submit,check,error);
        session.noticeProperty().addListener((o,a,b)->{if(!session.busyProperty().get())name.requestFocus();});javafx.application.Platform.runLater(name::requestFocus);
    }
}
