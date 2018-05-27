package main;

import com.google.gson.Gson;
import com.intel.bluetooth.RemoteDeviceHelper;
import com.sun.org.glassfish.external.arc.Stability;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import main.model.Difficulty;
import main.scene.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private static Retrofit api;
    private static RConnectorService rConnectorService;

    private final static int PACKAGE_SIZE = 5;

    private static int width = 1920; // default width
    private static int height = 1080; // default height

    private final static int DEGREES_MIN = 1;
    private final static int DEGREES_MAX = 30;

    private final static String BLUETOOTH_TAG = "Bluetooth";
    public final static double THRESHOLD_ACCELEROMETER_MAX = 7.0;
    private final static double THRESHOLD_ACCELEROMETER_MIN = 1.0;

    private ArrayList<Double> x = new ArrayList<>();
    private ArrayList<Double> y = new ArrayList<>();

    private ArrayList<Double> xCurrent;
    private ArrayList<Double> yCurrent;

    private boolean isX = true;
    private int counter = 0;

    private List<BluetoothEventListener> bluetoothListeners = new ArrayList<>();

    private Stage primaryStage;

    private ProcessConnectionThread processConnectionThread;

    private double thresholdMin;
    private double thresholdMax;

    private void replaceScene(Scene scene) {
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setScreen(BaseScreen screen) {
        if (bluetoothListeners != null) {
            bluetoothListeners.clear();
        }

        replaceScene(screen.getScene());
    }

    public void toast(String text) {
        Platform.runLater(() -> Toast.makeText(primaryStage, text, 1000, 500, 750));
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        width = (int) primaryScreenBounds.getWidth();
        height = (int) primaryScreenBounds.getHeight();

        //set Stage boundaries to visible bounds of the main screen
        primaryStage.setX(primaryScreenBounds.getMinX());
        primaryStage.setY(primaryScreenBounds.getMinY());
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);


        User user = User.get();
        String token = user.getToken();

        if (!TextUtils.isEmpty(token))
            startupLogin(user.getLogin(), token);
        else {
            toLogin();
        }

        prepareBluetoothData();
    }

    private void prepareBluetoothData() {
        thresholdMin = calculateThreshold(DEGREES_MAX);
        thresholdMax = calculateThreshold(DEGREES_MAX);
    }

    private static double calculateThreshold(int degrees) { // 3 degrees - ?
        // 90 degrees - MAX_THRESHOLD (7.0)
        double thresholdValue = degrees * THRESHOLD_ACCELEROMETER_MAX / 90.0;
        Log.i(BLUETOOTH_TAG, "Threshold value: " + thresholdValue);
        return thresholdValue;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void startupLogin(String login, String token) {
        Call<UserResult> call = restService().startupLogin(login, token);
        call.enqueue(new Callback<UserResult>() {
            @Override
            public void onResponse(Call<UserResult> call, Response<UserResult> response) {
                if (response != null) {
                    UserResult result = response.body();
                    if (result != null) {
                        if (result.invalid()) {
                            toast(result.error);
                            Platform.runLater(() -> toLogin());
                            return;
                        }
                        User.save(result);
                        Platform.runLater(() -> toBluetooth());
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResult> call, Throwable throwable) {
                throwable.printStackTrace();
                toast(throwable.getMessage());
            }
        });
    }

    public void initBluetooth(String url, ServiceRecord record) {
        try {
            StreamConnection connection = (StreamConnection) Connector.open(url, Connector.READ);

            RemoteDeviceHelper.authenticate(record.getHostDevice());

            processConnectionThread = new ProcessConnectionThread(connection);
            processConnectionThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toLogin() {
        setScreen(new LoginScreen(this));
    }

    public void toBluetooth() {
    //    setScreen(new BluetoothScreen(this));
        toTests();
    }

    public void toRegister() {
        setScreen(new RegisterScreen(this));
    }

    public void toTests() {
        setScreen(new TestsScreen(this));
    }

    public void toFocusingTest() {
        setScreen(new FocusingTestScreen(this));
    }

    public void toDistributionTest() {
        setScreen(new DistributionTestScreen(this));
    }

    public void toStabilityTest() {
        setScreen(new StabilityTestScreen(this));
    }

    public void toVolumeTest() {
        setScreen(new VolumeTestScreen(this));
    }

    public void toRamTest() {
        setScreen(new RamVolumeTestScreen(this));
    }

    class ProcessConnectionThread extends Thread {

        private StreamConnection mConnection;

        // Constant that indicate command from devices
        private static final int EXIT_CMD = -1000;

        public ProcessConnectionThread(StreamConnection connection) {
            mConnection = connection;
        }

        @Override
        public void run() {
            try {
                // prepare to receive data
                System.out.println("waiting for input");

                DataInputStream dataInputStream = mConnection.openDataInputStream();
                while (true) {

                    double value = dataInputStream.readDouble();
                    //        System.out.println("Received: " + value);

                    if (isX) {
                        x.add(value);
                    } else {
                        y.add(value);
                    }

                    counter++;
                    if (counter >= PACKAGE_SIZE) {
                        isX = !isX;
                        counter = 0;
                        if (isX) { // when we have x + y arrays
                            xCurrent = new ArrayList<>(x.subList(x.size() - PACKAGE_SIZE, x.size()));
                            yCurrent = new ArrayList<>(y.subList(y.size() - PACKAGE_SIZE, y.size()));
                            System.out.println("moveCircle");

                            sendToBluetoothListeners(xCurrent, yCurrent);
                        }
                    }

                    if (value == EXIT_CMD) {
                        System.out.println("finish process");
                        break;
                    }

/*                    if (!discovered) {
                        // stopped
                        break;
                    }*/

                }
            } catch (EOFException eofException) {
                // socket closed on server side
                //    eofException.printStackTrace();
                stopBluetooth();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void cancel() {
            if (mConnection != null) {
                try {
                    mConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean bluetoothLock = false;

    public void registerBluetoothListener(BluetoothEventListener listener) {
        bluetoothListeners.add(listener);
    }

    private void sendToBluetoothListeners(ArrayList<Double> xValues, ArrayList<Double> yValues) {
        if (bluetoothListeners != null && bluetoothListeners.size() > 0) {
            for (int i = 0; i < xValues.size(); i++) {
                for (BluetoothEventListener listener : bluetoothListeners) {
                    double currentX = xValues.get(i);
                    double currentY = yValues.get(i);

                    if (currentX < thresholdMin && currentX > -thresholdMin && currentY < thresholdMin && currentY > -thresholdMin) {
                        Log.i(BLUETOOTH_TAG, "onCenter");
                        bluetoothLock = false;
                        listener.onCenter();
                    }

                    if (bluetoothLock) {
                        return;
                    }

                    if (bluetoothLock) {
                        return;
                    }

                    if (currentX > thresholdMax && currentY < thresholdMax && currentY > -thresholdMax) { // left
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onLeft");
                        listener.onLeft();
                    } else if (currentX > thresholdMax && currentY > thresholdMax) { // bottom left
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onBottomLeft");
                        listener.onBottomLeft();
                    } else if (currentX > thresholdMax && currentY < -thresholdMax) { // top left
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onTopLeft");
                        listener.onTopLeft();
                    } else if (currentX < -thresholdMax && currentY < thresholdMax && currentY > -thresholdMax) { // right
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onRight");
                        listener.onRight();
                    } else if (currentX < -thresholdMax && currentY > thresholdMax) { // bottom right
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onBottomRight");
                        listener.onBottomRight();
                    } else if (currentX < -thresholdMax && currentY < -thresholdMax) { // top right
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onTopRight");
                        listener.onTopRight();
                    } else if (currentY > thresholdMax && currentX < thresholdMax && currentX > -thresholdMax) { // bottom
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onBottom");
                        listener.onBottom();
                    } else if (currentY < -thresholdMax && currentX < thresholdMax && currentX > -thresholdMax) { // top
                        bluetoothLock = true;
                        Log.i(BLUETOOTH_TAG, "onTop");
                        listener.onTop();
                    }


                }
            }
        }
    }

    private float adjust(double paramToAdjust, double maxValue, boolean inverse) { // 7 - xResolutionMax (e.g. 1920), 3 - y?
        //    7-1920
        //    paramToAdjust-x
        double value = (maxValue / 2) + (float) (paramToAdjust * maxValue / (THRESHOLD_ACCELEROMETER_MAX * 2));
        return inverse ? (float) Math.abs(maxValue - value) : (float) value;
    }

    private Retrofit api() {
        if (api == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);


            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            OkHttpClient client = httpClient.build();

            Gson gson = new Gson();

            api = new Retrofit.Builder()
                    .baseUrl(RConnectorService.API_ENDPOINT)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return api;
    }

    public RConnectorService restService() {
        if (rConnectorService == null)
            rConnectorService = createRetrofitService(RConnectorService.class);

        return rConnectorService;
    }

    private <T> T createRetrofitService(final Class<T> clazz) {
        return api().create(clazz);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void stopBluetooth() {

        if (processConnectionThread != null) {
            processConnectionThread.cancel();
        }

    }

    public Difficulty difficulty() {
        return Difficulty.MEDIUM;
    }
}