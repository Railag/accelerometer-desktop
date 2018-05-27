package main.scene;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import main.*;
import main.model.Answer;
import main.model.Sign;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VolumeTestScreen extends BaseScreen implements BluetoothEventListener {

    private final static int MAX_BACKGROUNDS = 1;

    private final static int SIGNS_TYPES = 12;

    private final static int SIGNS_PER_LINE = 5;

    private AnchorPane testBackground;
    private TilePane signsGrid;
    private ImageView sign1, sign2, sign3, sign4, sign5, sign6, sign7, sign8, sign9, sign10, sign11, sign12;
    private ImageView[] signImages;

    private final static String[] backgroundStrings = {"background.png", "background.png", "background.png", "background.png", "background.png",
            "background.png", "background.png", "background.png", "background.png", "background.png"};

    List<Sign> signsCounter;

    int currentBackground = 0;

    private boolean isSelection;

    private int previousSelection;
    private int currentSignSelection;

    Random random;

    private ArrayList<Answer> answers;

    private double resultTime;
    private long winsCount;
    private OnSignClickListener listener;

    private long time;

    public interface OnSignClickListener {
        void onSignSelected(Sign sign);
    }

    public VolumeTestScreen(Main main) {
        super(main, "attention-volume-test.fxml");
    }

    @Override
    protected void initViews() {
        testBackground = (AnchorPane) scene.lookup("#testBackground");
        signsGrid = (TilePane) scene.lookup("#signsGrid");
        sign1 = (ImageView) scene.lookup("#sign1");
        sign2 = (ImageView) scene.lookup("#sign2");
        sign3 = (ImageView) scene.lookup("#sign3");
        sign4 = (ImageView) scene.lookup("#sign4");
        sign5 = (ImageView) scene.lookup("#sign5");
        sign6 = (ImageView) scene.lookup("#sign6");
        sign7 = (ImageView) scene.lookup("#sign7");
        sign8 = (ImageView) scene.lookup("#sign8");
        sign9 = (ImageView) scene.lookup("#sign9");
        sign10 = (ImageView) scene.lookup("#sign10");
        sign11 = (ImageView) scene.lookup("#sign11");
        sign12 = (ImageView) scene.lookup("#sign12");

        signImages = new ImageView[]{sign1, sign2, sign3, sign4, sign5, sign6, sign7, sign8, sign9, sign10, sign11, sign12};

        time = System.nanoTime();

        random = new Random();

        answers = new ArrayList<>();

        signsCounter = Arrays.asList(Sign.values());
        for (Sign sign : signsCounter) {
            sign.setChosen(false);
            sign.setSelected(false);
            sign.setShown(false);
        }

        next();
    }

    private void next() {
        int startTime = 10000; // 10 seconds

        resetSigns();

        setupBackground();
        setupSigns();

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(startTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                double result = Utils.calcTime(time);
                Answer answer = new Answer();
                answer.setTime(result);

                answer.setErrorValue(1); // error

                answer.setNumber(answers.size());

                answers.add(answer);

                time = System.nanoTime();

                nextBackground();

            });
        });

        thread.start();
    }

    private void resetSigns() {
        if (signImages != null && signImages.length > 0) {
            for (ImageView image : signImages) {
                image.setImage(null);
            }
        }
    }

    private void setupSigns() {
        List<Sign> signs = Sign.randomSigns(SIGNS_TYPES);

        for (Sign sign : signs) {
            while (true) {
                int position = random.nextInt(SIGNS_TYPES);
                if (signImages[position].getImage() == null) {
                    signImages[position].setImage(loadImage("image/" + sign.getDrawableString()));
                    break;
                }
            }

            signsCounter.get(signsCounter.indexOf(sign)).setShown(true);
        }
    }

    private void setupBackground() {
        int backgroundNumber = random.nextInt(backgroundStrings.length);
        testBackground.setBackground(new Background(new BackgroundImage(loadImage("image/" + backgroundStrings[backgroundNumber]),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
    }

    private void nextBackground() {
        currentBackground++;

        if (currentBackground >= MAX_BACKGROUNDS) {
            toFinalSelection();
        } else {
            next();
        }
    }

    private void toFinalSelection() {
        for (ImageView sign : signImages) {
            sign.setVisible(false);
        }

        time = System.nanoTime();

        signsCounter.get(0).setSelected(true);

        listener = sign -> {
            sign.setChosen(!sign.isChosen());

            int chosenCounter = 0;
            for (Sign s : signsCounter) {
                if (s.isChosen()) {
                    chosenCounter++;
                }
            }

            if (chosenCounter >= SIGNS_TYPES) {
                toNextTest();
            } else {
                refreshSigns();
            }
        };


        refreshSigns();

        isSelection = true;
    }

    private void refreshSigns() {
        signsGrid.getChildren().clear();

        for (Sign sign : signsCounter) {
            ImageView signImage = new ImageView();

            PseudoClass imageViewBorder = PseudoClass.getPseudoClass("border");

            BorderPane imageViewWrapper = new BorderPane(signImage);
            imageViewWrapper.getStyleClass().add("image-view-wrapper");

            BooleanProperty imageViewBorderActive = new SimpleBooleanProperty() {
                @Override
                protected void invalidated() {
                    imageViewWrapper.pseudoClassStateChanged(imageViewBorder, get());
                }
            };

            signImage.setImage(loadImage("image/" + sign.getDrawableString()));
            signImage.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println("Tile pressed ");
                listener.onSignSelected(sign);
                event.consume();
            });

            BorderPane root = new BorderPane(imageViewWrapper);
            root.setPadding(new Insets(15));

            if (sign.isSelected() || sign.isChosen()) {
                imageViewBorderActive.setValue(true);
            } else {
                imageViewBorderActive.setValue(false);
            }

            signsGrid.getChildren().add(root);
        }
    }

    private void toNextTest() {
        List<Sign> chosenSigns = new ArrayList<>();
        for (Sign s : signsCounter) {
            if (s.isChosen()) {
                chosenSigns.add(s);
            }
        }

        winsCount = 0;

        for (Sign s : chosenSigns) {
            if (s.isChosen() && s.wasShown()) {
                winsCount++;
                s.setChosen(false);
                s.setShown(false);
            }
        }

        resultTime = Utils.calcTime(time);

        startLoading();
        sendResults(resultTime, winsCount);
    }

    private void sendResults(double resultTime, long winsCount) {
        Call<Result> call = restService().sendAttentionVolumeResults(User.get().getId(), resultTime, winsCount);
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
        if (isSelection) {
            if (currentSignSelection > 0) {
                currentSignSelection--;
                refreshSelection();
            }
        }
    }

    private void refreshSelection() {
        signsCounter.get(previousSelection).setSelected(false);
        signsCounter.get(currentSignSelection).setSelected(true);

        previousSelection = currentSignSelection;

        refreshSigns();
    }

    @Override
    public void onRight() {
        if (isSelection) {
            if (currentSignSelection < signsCounter.size() - 1) {
                currentSignSelection++;
                refreshSelection();
            }
        }
    }

    @Override
    public void onTop() {
        if (isSelection) {
            if (currentSignSelection >= SIGNS_PER_LINE) {
                currentSignSelection -= SIGNS_PER_LINE;
                refreshSelection();
            }
        }
    }

    @Override
    public void onBottom() {
        if (isSelection) {
            if (currentSignSelection + SIGNS_PER_LINE < signsCounter.size() - 1) {
                currentSignSelection += SIGNS_PER_LINE;
                refreshSelection();
            }
        }
    }

    @Override
    public void onTopLeft() {
    }

    @Override
    public void onTopRight() {
        if (listener != null) {
            listener.onSignSelected(signsCounter.get(currentSignSelection));
        }
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