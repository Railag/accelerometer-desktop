package main.scene;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import main.Main;
import main.RConnectorService;

import java.io.IOException;

public class BaseScreen {

    private ProgressIndicator progressIndicator;

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

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(100, 100);
        progressIndicator.setLayoutX(main.getWidth() / 2);
        progressIndicator.setLayoutY(main.getHeight() / 2);
        progressIndicator.setVisible(false);

        Group group = new Group(content, progressIndicator);
        scene = new Scene(group, main.getWidth(), main.getHeight());

        scene.getStylesheets().add(
                getClass().getResource("/css/application.css").toExternalForm());
    }

    public Scene getScene() {
        return scene;
    }

    protected void toast(String message) {
        if (main != null) {
            main.toast(message);
        }
    }

    protected RConnectorService restService() {
        return main.restService();
    }

    protected void startLoading() {
        progressIndicator.setVisible(true);
    }

    protected void stopLoading() {
        progressIndicator.setVisible(false);
    }

    protected Image loadImage(String path) {
        //Image image = new Image("/drawIcon.png");
        return new Image("/" + path);
    }

    public void bluetoothListener() {
        // implement if needed
    }
}