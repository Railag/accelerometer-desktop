package main.scene;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import main.*;
import main.model.Answer;
import main.model.Difficulty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Random;

public class StabilityTestScreen extends BaseScreen implements BluetoothEventListener {

    private final static int MAX_NUMBER = 10;

    private AnchorPane testBackground;
    private Button leftButton;
    private Button rightButton;
    private Label number;
    private Rectangle background;

    private int wins;
    private long errors;
    private long misses;

    private Random random = new Random();

    private int currentNum = -1;

    private int progressTime = 0;

    private boolean active;

    private long time;

    private ArrayList<Answer> answers;

    private ArrayList<KeyFrame> keyframes;

    public StabilityTestScreen(Main main) {
        super(main, "stability-test.fxml");
    }

    @Override
    protected void initViews() {
        main.registerBluetoothListener(this);

        testBackground = (AnchorPane) scene.lookup("#testBackground");
        leftButton = (Button) scene.lookup("#leftButton");
        rightButton = (Button) scene.lookup("#rightButton");
        number = (Label) scene.lookup("#number");
        background = (Rectangle) scene.lookup("#background");

        background.setFill(Color.valueOf(Colors.COLOR_RED_REACTION));
        background.setVisible(false);

        Colors.setBackgroundColor(rightButton, Colors.COLOR_GREEN_STRESS);
        Colors.setBackgroundColor(leftButton, Colors.COLOR_RED_REACTION);

        time = System.nanoTime();

        random = new Random();

        answers = new ArrayList<>();

        keyframes = new ArrayList<>();

        Random randomTime = new Random();

        Difficulty diff = main.difficulty();

        leftButton.setOnAction(event -> click(false));
        rightButton.setOnAction(event -> click(true));

        for (int i = 0; i < 100 * diff.getLevel(); i++) {
            if (i < 10) { // first 10 numbers
                progressTime += randomTime.nextInt(800);
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    int num = generateRandomNumber();
                    number.setText(String.valueOf(num));
                }));
            } else if (i % 10 == 0) { // first square
                progressTime += 800;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    showRedBackground();
                }));
            } else if (i == 99 * diff.getLevel()) {
                progressTime += 250;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    toNextTest();
                }));
            } else if (i % 10 == 1) { // number between
                progressTime += 700;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    currentNum = generateRandomNumber(); // number before second red square
                    number.setText(String.valueOf(currentNum));
                    showNumber();
                    time = System.nanoTime();
                    active = true;
                }));
            } else if (i % 10 == 2) { // second square
                progressTime += 300;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    showRedBackground();
                }));
            } else if (i % 10 == 3) { // hide second square
                progressTime += 700;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    showNumber();
                }));
            } else if (i % 10 == 4) {
                progressTime += 50;
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    int num = generateRandomNumber();
                    number.setText(String.valueOf(num));
                }));
            } else {
                progressTime += randomTime.nextInt(800);
                keyframes.add(new KeyFrame(Duration.millis(progressTime), e -> {
                    int num = generateRandomNumber();
                    number.setText(String.valueOf(num));
                    if (active) {
                        misses++;
                        active = false;
                    }
                }));
            }

            final Timeline timeline = new Timeline();
            timeline.getKeyFrames().addAll(keyframes);
            Platform.runLater(timeline::play);
        }
    }

    private int generateRandomNumber() {
        return random.nextInt(MAX_NUMBER);
    }

    private void showRedBackground() {
        background.setVisible(true);
    }

    private void showNumber() {
        background.setVisible(false);
    }

    public void click(boolean right) {

        if (!active)
            return;

        double resultTime = Utils.calcTime(time);
        Answer answer = new Answer();
        answer.setTime(resultTime);

        boolean evenNumber = currentNum % 2 == 0; // четное

        if (right) {
            if (evenNumber) {
                errors++;
                answer.setErrorValue(1);
            } else {
                wins++;
                answer.setErrorValue(0);
            }
        } else {
            if (!evenNumber) {
                errors++;
                answer.setErrorValue(1);
            } else {
                wins++;
                answer.setErrorValue(0);
            }
        }

        answer.setNumber(answers.size());
        answers.add(answer);

        active = false;

        //    Toast.makeText(getActivity(), "Wins = " + wins + ", Fails = " + errors, Toast.LENGTH_SHORT).show();
    }


    private void toNextTest() {
        ArrayList<Double> times = new ArrayList<>();

        for (Answer a : answers) {
            times.add(a.getTime());
        }

        startLoading();
        sendResults(times, errors, misses);
    }

    private void sendResults(ArrayList<Double> times, long errors, long misses) {
        Call<Result> call = restService().sendAttentionStabilityResults(User.get().getId(), times, misses, errors);
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
        Colors.setBackgroundColor(testBackground, Colors.COLOR_GREY_REACTION);
        click(false);
    }

    @Override
    public void onRight() {
        Colors.setBackgroundColor(testBackground, Colors.COLOR_GREY_REACTION);
        click(true);
    }

    @Override
    public void onTop() {
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
        Colors.setBackgroundColor(testBackground, Colors.COLOR_DARKER_GRAY);
    }
}