package guessmarket.client;
import javafx.application.Application;
import javafx.scene.Scene;
import guessmarket.client.ui.ClientShell;
import guessmarket.client.session.RemoteMarketSession;
import javafx.stage.Stage;
public final class ClientMain extends Application {
    private RemoteMarketSession session;
    @Override public void start(Stage stage){session=new RemoteMarketSession(System.getProperty("guessmarket.serverUrl",guessmarket.client.http.MarketHttpClient.DEFAULT_URL),javafx.application.Platform::runLater);stage.setTitle("Guess Market - Exercise 3");stage.setScene(new Scene(new ClientShell(session),1120,820));stage.setMinWidth(680);stage.setMinHeight(602);stage.setOnCloseRequest(e->session.close());stage.show();}
    @Override public void stop(){if(session!=null)session.close();}
    public static void main(String[] args){launch(args);}
}
