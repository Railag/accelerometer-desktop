package main.scene;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
    private GridPane circlesGrid;
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

    CirclesAdapter adapter;

    private int currentLine = 0;

    private int previousSelection;
    private int currentButtonSelection;

    private long time;

    private ArrayList<Circle> circles = new ArrayList<>();

    private ArrayList<Answer> answers = new ArrayList<>();

    private Circle baseCircle;


    public FocusingTestScreen(Main main) {
        super(main, "focusing-test.fxml");
    }

    @Override
    protected void initViews() {
        main.registerBluetoothListener(this);

        baseCircleView = (ImageView) scene.lookup("#baseCircle");
        circlesGrid = (GridPane) scene.lookup("#circlesGrid");
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

        for (int i = 0; i < buttonViews.length; i++) {
            final int index = i;
            Button button = buttonViews[index];
            button.setOnAction(event -> {
                click(index);
            });
        }

        Difficulty diff = main.difficulty();
        LINES_COUNT *= diff.getLevel();

        baseCircle = Circle.random();
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

      /*  adapter = new CirclesAdapter();
        adapter.setCircles(circles);

        GridLayoutManager manager = new GridLayoutManager(getActivity(), CIRCLES_PER_LINE);
        circlesGrid.setLayoutManager(manager);

        circlesGrid.setAdapter(adapter);
*/
        time = System.nanoTime();

/*        createAccountButton.setOnAction(event -> {
            main.toRegister();
        });*/
    }

    private void replaceCircleLine() {
        ArrayList<Circle> newCircles = new ArrayList<>(circles.subList(CIRCLES_PER_LINE, circles.size()));

        if (LINES_COUNT - currentLine > LINES_VISIBLE) {
            for (int i = 0; i < CIRCLES_PER_LINE; i++) {
                newCircles.add(Circle.random());
            }
        }

        circles = newCircles;
        adapter.setCircles(circles);

        baseCircle = Circle.random();
        baseCircleView.setRotate(Circle.rotation(baseCircle));
    }

    public void click(int count) {

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

    private void sendResults(ArrayList<Double> login, ArrayList<Long> password) {
        Call<Result> call = restService().sendFocusingResults(User.get().getId(), login, password);
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
                        main.toTests();
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

    @Override
    public void onLeft() {
        if (currentButtonSelection > 0) {
            currentButtonSelection--;
            refreshSelection();
        }
    }

    private void refreshSelection() {
        buttonViews[previousSelection].setBackground(android.R.color.darker_gray);

        previousSelection = currentButtonSelection;

        buttonViews[currentButtonSelection].setBackgroundResource(R.drawable.outline);
    }

    @Override
    public void onRight() {
        if (currentButtonSelection < BUTTONS_COUNT - 1) {
            currentButtonSelection++;
            refreshSelection();
        }
    }

    @Override
    public void onTop() {
        if (currentButtonSelection + BUTTONS_PER_LINE < BUTTONS_COUNT) {
            currentButtonSelection += BUTTONS_PER_LINE;
            refreshSelection();
        }
    }

    @Override
    public void onBottom() {
        if (currentButtonSelection >= BUTTONS_PER_LINE) {
            currentButtonSelection -= BUTTONS_PER_LINE;
            refreshSelection();
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
        click(currentButtonSelection + 1); // 0-9 -> 1-10
    }

    @Override
    public void onCenter() {
    }
}
