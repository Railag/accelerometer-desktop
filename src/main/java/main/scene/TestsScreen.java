package main.scene;

import javafx.scene.control.Button;
import main.Main;


public class TestsScreen extends BaseScreen {

    private Button focusingButton;
    private Button distributionButton;
    private Button stabilityButton;
    private Button volumeButton;
    private Button ramVolumeButton;

    public TestsScreen(Main main) {
        super(main, "tests.fxml");
    }

    @Override
    protected void initViews() {
        focusingButton = (Button) scene.lookup("#focusingButton");
        distributionButton = (Button) scene.lookup("#distributionButton");
        stabilityButton = (Button) scene.lookup("#stabilityButton");
        volumeButton = (Button) scene.lookup("#volumeButton");
        ramVolumeButton = (Button) scene.lookup("#ramVolumeButton");

        focusingButton.setOnAction(event -> {
            main.toFocusingTest();
        });

        distributionButton.setOnAction(event -> {
            main.toDistributionTest();
        });

        stabilityButton.setOnAction(event -> {
            main.toStabilityTest();
        });

        volumeButton.setOnAction(event -> {
            main.toVolumeTest();
        });

        ramVolumeButton.setOnAction(event -> {
            main.toRamTest();
        });
    }
}
