package guessmarket.client.ui;
import guessmarket.client.session.RemoteMarketSession;
import guessmarket.dto.api.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import java.util.Objects;
public final class ClientShell extends BorderPane {
    private final RemoteMarketSession session;
    private final EventNavigationState navigation=new EventNavigationState();
    private final EventsView events;
    private final AccountView account;
    private final TabPane tabs=new TabPane();
    private final Label balance=UiTables.label("Balance: 0.00","stat-value"),state=UiTables.label("Connecting...","muted"),identity=UiTables.label("","subheading"),notice=UiTables.label("","notice");
    private final VBox authenticated=new VBox(10);
    public ClientShell(RemoteMarketSession session){
        this.session=session;events=new EventsView(session,navigation,true);account=new AccountView(session,navigation);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);tabs.getTabs().addAll(new Tab("Events",events),new Tab("Account",account));VBox.setVgrow(tabs,Priority.ALWAYS);tabs.getSelectionModel().selectedIndexProperty().addListener((o,a,b)->{session.clearNotice();if(b.intValue()==0)events.activate();else account.activate();});
        balance.setId("own-balance");balance.setFocusTraversable(true);identity.textProperty().bind(session.usernameProperty());identity.setMaxWidth(260);
        var upload=new Button("Upload XML");upload.setId("upload-xml");upload.setOnAction(e->{var chooser=new FileChooser();chooser.setTitle("Upload EX3 XML");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files","*.xml"));var file=chooser.showOpenDialog(getScene().getWindow());if(file!=null)session.upload(file.toPath());});
        var funds=new Button("Load funds");funds.setId("load-funds");funds.getStyleClass().add("primary");funds.setOnAction(e->{var dialog=new FundsDialog(session);dialog.initOwner(getScene().getWindow());dialog.showAndWait().ifPresent(session::addFunds);funds.requestFocus();});
        var logout=new Button("Logout");logout.setId("logout");logout.setOnAction(e->session.logout());
        var disabled=session.loggedInProperty().not().or(session.connectedProperty().not()).or(session.busyProperty()).or(session.unknownProperty());upload.disableProperty().bind(disabled);funds.disableProperty().bind(disabled);
        var retry=new Button("Check/retry same request");retry.setId("check-outcome");retry.setOnAction(e->session.checkOrRetryOutcome());retry.visibleProperty().bind(session.unknownProperty());retry.managedProperty().bind(retry.visibleProperty());retry.disableProperty().bind(session.busyProperty());
        var header=new FlowPane(14,10,UiTables.label("GUESS MARKET","brand"),identity,balance,state,upload,funds,logout,retry);header.getStyleClass().add("header");
        var progress=new ProgressBar();progress.setMaxWidth(Double.MAX_VALUE);progress.visibleProperty().bind(session.busyProperty());progress.managedProperty().bind(progress.visibleProperty());notice.textProperty().bind(session.noticeProperty());notice.visibleProperty().bind(session.noticeProperty().isNotEmpty());notice.managedProperty().bind(notice.visibleProperty());notice.setPadding(new Insets(10,20,10,20));
        authenticated.getChildren().addAll(header,progress,notice,tabs);authenticated.setPadding(new Insets(0,16,16,16));
        session.loggedInProperty().addListener((o,a,b)->screen());session.dashboardProperty().addListener((o,a,b)->{if(b==null){events.clear();account.clear();navigation.clear();balance.setText("Balance: 0.00");}else{balance.setText("Balance: "+WireViewMapper.money(b.account().balance()));balance.setTooltip(new Tooltip("Exact stored balance: "+b.account().balance()));balance.setAccessibleText("Own balance "+b.account().balance());events.update(b);account.update(b);}connection();});
        session.connectedProperty().addListener((o,a,b)->connection());session.busyProperty().addListener((o,a,b)->connection());session.unknownProperty().addListener((o,a,b)->connection());session.lastRefreshProperty().addListener((o,a,b)->connection());
        session.receiptProperty().addListener((o,a,b)->{if(b instanceof CommandResponse receipt&&receipt.operation()==OperationKind.FUND_LOAD){var dialog=new Alert(Alert.AlertType.INFORMATION);dialog.setTitle("Funds receipt");dialog.setHeaderText("Funds loaded: "+WireViewMapper.money(receipt.creditedAmount()));dialog.setContentText("Account balance: "+WireViewMapper.money(receipt.balanceAfter())+"\nExact credited amount: "+receipt.creditedAmount()+"\nExact stored balance: "+receipt.balanceAfter());if(getScene()!=null)dialog.initOwner(getScene().getWindow());dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/guessmarket/client/client.css")).toExternalForm());dialog.show();}});
        getStylesheets().add(Objects.requireNonNull(getClass().getResource("/guessmarket/client/client.css")).toExternalForm());screen();
    }
    private void screen(){if(session.loggedInProperty().get()){tabs.getSelectionModel().select(0);setCenter(authenticated);}else{navigation.clear();var login=new LoginView(session);var center=new StackPane(login);center.setAlignment(Pos.CENTER);center.setPadding(new Insets(24));setCenter(center);}}
    private void connection(){state.setText(session.unknownProperty().get()?"Outcome unknown":session.busyProperty().get()?"Working...":!session.connectedProperty().get()?"Disconnected - cached data":session.dashboard()!=null&&session.dashboard().account().blocked()?"Blocked: funds or owner close allowed":"Connected\nUpdated "+session.lastRefreshProperty().get());}
}
