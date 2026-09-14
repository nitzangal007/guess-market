package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class GuessMarketApplication extends Application {
    private WorldSession session;

    @Override public void start(Stage stage) throws Exception {
        session = new WorldSession(new GuessMarketWorldEngineImpl());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setControllerFactory(type -> { if (type == MainController.class) return new MainController(session); if (type == UsersController.class) return new UsersController(session); throw new IllegalArgumentException("Unknown controller: " + type); });
        Parent root = loader.load();
        stage.setTitle("Guess Market");
        stage.setScene(new Scene(root, 1120, 820));
        stage.setOnCloseRequest(event -> session.close());
        stage.show();
    }

    @Override public void stop() {
        if (session != null) session.close();
    }

    public static void main(String[] args) { launch(args); }
}
