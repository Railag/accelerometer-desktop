package main.scene;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import main.*;
import main.model.Answer;
import main.model.Circle;
import main.model.Difficulty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class FocusingTestScreen extends BaseScreen implements BluetoothEventListener {

    private ImageView baseCircleView;
    private TilePane circlesGrid;
    private Button button1, button2, button3, button4, button5, button6, button7, button8, button9, button10;
    private Button[] buttonViews;

    private final static int LINES_VISIBLE = 11;

    private static int LINES_COUNT = 20;
    private final static int CIRCLES_PER_LINE = 15;

    private final static int BUTTONS_COUNT = 10;
    private final static int BUTTONS_PER_LINE = 5;

    private int wins;
    private int fails;

    private boolean locked = false;

    private int currentLine = 0;

    private int previousSelection;
    private int currentButtonSelection;

    private long time;

    private ArrayList<Circle> circles;

    private ArrayList<Answer> answers;

    private Circle baseCircle;

    private BackgroundFill backgroundFillSelected;
    private BackgroundFill backgroundFillUnselected;

    public FocusingTestScreen(Main main) {
        super(main, "focusing-test.fxml");
    }

    @Override
    protected void initViews() {
        baseCircleView = (ImageView) scene.lookup("#baseCircle");
        circlesGrid = (TilePane) scene.lookup("#circlesGrid");
        button1 = (Button) scene.lookup("#button1");
        button2 = (Button) scene.lookup("#button2");
        button3 = (Button) scene.lookup("#button3");
        button4 = (Button) scene.lookup("#button4");
        button5 = (Button) scene.lookup("#button5");
        button6 = (Button) scene.lookup("#button6");
        button7 = (Button) scene.lookup("#button7");
        button8 = (Button) scene.lookup("#button8");
        button9 = (Button) scene.lookup("#button9");
        button10 = (Button) scene.lookup("#button10");

        buttonViews = new Button[]{button1, button2, button3, button4, button5, button6, button7, button8, button9, button10};

        backgroundFillSelected = new BackgroundFill(Color.ANTIQUEWHITE, new CornerRadii(1),
                new Insets(0.0, 0.0, 0.0, 0.0));

        backgroundFillUnselected = new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(1),
                new Insets(0.0, 0.0, 0.0, 0.0));

        for (int i = 0; i < buttonViews.length; i++) {
            final int index = i;
            Button button = buttonViews[index];
            button.setOnAction(event -> {
                click(index);
            });
            
            if (i == 0) {
                button.setBackground(new Background(backgroundFillSelected));
            } else {
                button.setBackground(new Background(backgroundFillUnselected));
            }
        }

        circles = new ArrayList<>();
        answers = new ArrayList<>();

        Difficulty diff = main.difficulty();
        LINES_COUNT *= diff.getLevel();

        baseCircle = Circle.random();
        baseCircleView.setImage(loadImage("image/circle.png"));
        baseCircleView.setRotate(Circle.rotation(baseCircle));

        for (int i = 0; i < LINES_VISIBLE; i++) {
            ArrayList<Circle> line = new ArrayList<>();
            for (int j = 0; j < CIRCLES_PER_LINE; j++) {
                line.add(Circle.random());
            }

            int result = Circle.answer(line, baseCircle);
            if (result > 10 || result < 1) {
                i--;
            } else {
                circles.addAll(line);
            }
        }

        setCircles();

        time = System.nanoTime();
    }

    private void setCircles() {
        circlesGrid.getChildren().clear();

        for (Circle circle : circles) {
            ImageView circleImage = new ImageView(loadImage("image/circle.png"));
            switch (circle) {

                case TOP_RIGHT:
                    circleImage.setRotate(180);
                    break;
                case TOP_LEFT:
                    circleImage.setRotate(90);
                    break;
                case DOWN_RIGHT:
                    circleImage.setRotate(270);
                    break;
                case DOWN_LEFT:
                default:
                    break;
            }

            circlesGrid.getChildren().add(circleImage);
        }
    }

    private void replaceCircleLine() {
        ArrayList<Circle> newCircles = new ArrayList<>(circles.subList(CIRCLES_PER_LINE, circles.size()));

        if (LINES_COUNT - currentLine > LINES_VISIBLE) {
            for (int i = 0; i < CIRCLES_PER_LINE; i++) {
                newCircles.add(Circle.random());
            }
        }

        circles = newCircles;

        setCircles();

        baseCircle = Circle.random();
        baseCircleView.setRotate(Circle.rotation(baseCircle));
    }

    public void click(int count) {
        lock = false;

        if (locked) {
            return;
        }

        if (currentLine == LINES_COUNT) {
            toNextTest();
            return;
        }

        if (currentLine > LINES_COUNT) {
            return;
        }

        List<Circle> lineCircles = circles.subList(0, CIRCLES_PER_LINE);
        int answer = Circle.answer(lineCircles, baseCircle);

        Answer ans = new Answer();
        ans.setNumber(currentLine);

        if (answer == count) {
            wins++;
        } else {
            fails++;
        }

        ans.setErrorValue(Math.abs(answer - count));
        ans.setTime(Utils.calcTime(time));
        answers.add(ans);

        //    Toast.makeText(getActivity(), "Wins = " + wins + ", Fails = " + fails, Toast.LENGTH_SHORT).show();

        replaceCircleLine();
        time = System.nanoTime();

        currentLine++;
        if (currentLine == LINES_COUNT) {
            toNextTest();
        }
    }

    private void toNextTest() {
        locked = true;
        ArrayList<Double> times = new ArrayList<>();
        ArrayList<Long> errors = new ArrayList<>();

        for (Answer a : answers) {
            times.add(a.getTime());

            errors.add((long) a.getErrorValue());
        }

        startLoading();
        sendResults(times, errors);
    }

    private void sendResults(ArrayList<Double> times, ArrayList<Long> errors) {
        Call<Result> call = restService().sendFocusingResults(User.get().getId(), times, errors);
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

                locked = false;
            }
        });
    }


    private void refreshSelection() {
        for (int i = 0; i < buttonViews.length; i++) {
            Button button = buttonViews[i];
            if (currentButtonSelection == i) {
                button.setBackground(new Background(backgroundFillSelected));
            } else {
                button.setBackground(new Background(backgroundFillUnselected));
            }
        }

        lock = false;
    }

    private boolean lock;


    @Override
    public void onLeft() {
        if (lock) {
            return;
        }
        lock = true;

        if (currentButtonSelection > 0) {
            currentButtonSelection--;
            refreshSelection();
        } else {
            lock = false;
        }
    }

    @Override
    public void onRight() {
        if (lock) {
            return;
        }
        lock = true;

        if (currentButtonSelection < BUTTONS_COUNT - 1) {
            currentButtonSelection++;
            refreshSelection();
        } else {
            lock = false;
        }
    }

    @Override
    public void onTop() {
        if (lock) {
            return;
        }
        lock = true;

        if (currentButtonSelection + BUTTONS_PER_LINE < BUTTONS_COUNT) {
            currentButtonSelection += BUTTONS_PER_LINE;
            refreshSelection();
        } else {
            lock = false;
        }
    }

    @Override
    public void onBottom() {
        if (lock) {
            return;
        }
        lock = true;

        if (currentButtonSelection >= BUTTONS_PER_LINE) {
            currentButtonSelection -= BUTTONS_PER_LINE;
            refreshSelection();
        } else {
            lock = false;
        }
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
        if (lock) {
            return;
        }
        lock = true;

        click(currentButtonSelection + 1); // 0-9 -> 1-10
    }

    @Override
    public void onCenter() {
    }

    @Override
    public void bluetoothListener() {
        main.registerBluetoothListener(this);
    }
}
