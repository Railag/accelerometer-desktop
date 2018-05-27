package main.scene;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import main.*;
import main.model.Difficulty;
import main.model.Figure;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DistributionTestScreen extends BaseScreen implements BluetoothEventListener {

    private final static int MAX_FIGURES = 100;

    private final static int BASE_TIME = 2100;

    private Button button;
    private ImageView firstImage;
    private ImageView secondImage;

    private int wins;
    private int fails;
    private int misses;

    Figure figure1;
    Figure figure2;

    private boolean active;

    private int currentFigure = 0;

    private Difficulty currentDiff;

    private long time;


    public DistributionTestScreen(Main main) {
        super(main, "distribution-test.fxml");
    }

    @Override
    protected void initViews() {
        button = (Button) scene.lookup("#button");
        firstImage = (ImageView) scene.lookup("#firstImage");
        secondImage = (ImageView) scene.lookup("#secondImage");

        button.setOnAction(event -> click());

        figure1 = Figure.random();
        figure2 = Figure.random();
        currentDiff = main.difficulty();
        next();

        time = System.nanoTime();
    }

    private void next() {
        Thread thread = new Thread(() -> {

            try {
                Thread.sleep(BASE_TIME / currentDiff.getLevel());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                if (active && figure1.equals(figure2)) {
                    misses++;
                }

                currentFigure++;
                if (currentFigure >= MAX_FIGURES) {
                    toast("Wins = " + wins + ", Fails = " + fails);
                    toNextTest();
                    return;
                }

                Figure temp1, temp2;

                while (true) {
                    temp1 = Figure.random();
                    temp2 = Figure.random();

                    if (!figure1.equals(temp1) && !figure2.equals(temp2)) {
                        break;
                    }
                }

                figure1 = temp1;
                figure2 = temp2;

                firstImage.setImage(loadImage("image/" + figure1.getDrawableString()));
                secondImage.setImage(loadImage("image/" + figure2.getDrawableString()));

                active = true;
                Log.i("DEBUG", currentFigure + " w:" + wins + " f:" + fails);

                next();

            });

        });

        thread.start();
    }

    public void click() {

        if (!active) {
            return;
        }

        if (currentFigure > MAX_FIGURES) {
            return;
        }

        if (figure1.equals(figure2)) {
            wins++;
        } else {
            fails++;
        }

        active = false;
    }


    private void toNextTest() {
        startLoading();
        sendResults(wins, fails, misses);
    }

    private void sendResults(int wins, int fails, int misses) {
        Call<Result> call = restService().sendAttentionDistributionResults(User.get().getId(), wins, fails, misses);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                if (response != null) {
                    Result result = response.body();
                    if (result != null) {
                        if (result.invalid()) {
                            toast(result.error);
                            return;
                        }

                        stopLoading();
                        Platform.runLater(() -> main.toTests());
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable throwable) {
                throwable.printStackTrace();
                toast(throwable.getMessage());
            }
        });
    }

    @Override
    public void onLeft() {

    }

    @Override
    public void onRight() {

    }

    @Override
    public void onTop() {
        click();
    }

    @Override
    public void onBottom() {

    }

    @Override
    public void onTopLeft() {

    }

    @Override
    public void onTopRight() {

    }

    @Override
    public void onBottomLeft() {

    }

    @Override
    public void onBottomRight() {

    }

    @Override
    public void onCenter() {

    }

    @Override
    public void bluetoothListener() {
        main.registerBluetoothListener(this);
    }
}
