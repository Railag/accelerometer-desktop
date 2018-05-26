package main.scene;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import main.Main;
import main.RConnectorService;

import java.io.IOException;

public class BaseScreen {

    protected Main main;

    protected Parent content;
    protected Scene scene;

    public BaseScreen(Main main, String fxmlFile) {
        this.main = main;
        inflateLayout(fxmlFile);
        initViews();
    }

    protected void initViews() {
    }

    protected void inflateLayout(String fxmlFile) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlFile));
        try {
            content = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scene = new Scene(content);
    }

    public Scene getScene() {
        return scene;
    }

    protected void toast(String message) {
        Platform.runLater(() -> {
            if (main != null) {
                main.toast(message);
            }
        });
    }

    protected RConnectorService restService() {
        return main.restService();
    }
}