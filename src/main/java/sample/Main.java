package sample;

import com.google.gson.Gson;
import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.RemoteDeviceHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Main extends Application {

    private final static String SEPARATOR = "\r\n";

    private static Retrofit api;
    private static RConnectorService rConnectorService;

    private final static int PACKAGE_SIZE = 4; // 10 x and 10 y

    private static int width = 1920; // default width
    private static int height = 1080; // default height

    //private final static double THRESHOLD_ACCELEROMETER_MAX = 10.0; REAL
    private final static double THRESHOLD_ACCELEROMETER_MAX = 5.0;

    private long userId = -1;

    private long latestEventId = -1;

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<>();
    private ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();

    private ArrayList<Double> x = new ArrayList<>();
    private ArrayList<Double> y = new ArrayList<>();

    private ArrayList<Double> xCurrent;
    private ArrayList<Double> yCurrent;

    private boolean isX = true;
    private int counter = 0;

    private Stage primaryStage;
    private Circle circle;
    private ProgressIndicator progress;
    private Button bluetoothButton, listButton;
    private ListView<String> list;

    private DiscoveryAgent agent;

    private WaitThread waitThread;
    private ProcessConnectionThread processConnectionThread;
    private int transactionSearchId;

    private String deviceName = "";

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

        // init circle
        circle = new Circle();
        circle.setCenterX(0.0f);
        circle.setCenterY(0.0f);
        circle.setLayoutX(width / 2);
        circle.setLayoutY(height / 2);
        circle.setRadius(15.0f);
        circle.setVisible(false);

        // init loading indicator
        progress = new ProgressIndicator();
        progress.setPrefSize(100, 100);
        progress.setLayoutX(width / 2);
        progress.setLayoutY(height / 2);
        progress.setVisible(false);

        list = new ListView<String>();
        list.setLayoutX(200);
        list.setLayoutY(300);
        list.setVisible(false);


        // init bluetooth discover button
        bluetoothButton = new Button("Discover");
        bluetoothButton.setOnAction(event -> {
            if (discovered) {
                stopBluetooth();
            }

            bluetoothInit();
        });
        bluetoothButton.setLayoutX(width / 2);
        bluetoothButton.setLayoutY(height / 2 - 50);

        listButton = new Button("Connect");
        listButton.setOnAction(event -> {
            deviceName = list.getSelectionModel().getSelectedItem();
            System.out.println("Device name: " + deviceName);
            discover(deviceName);
        });
        listButton.setLayoutX(width / 2);
        listButton.setLayoutY(height / 2 - 100);
        listButton.setVisible(false);

        Group root = new Group(circle, progress, bluetoothButton, list, listButton);

        primaryStage.setScene(new Scene(root, width, height));
        primaryStage.show();

        bluetoothInit();

        login();
    }

    private void startLoading() {
        progress.setVisible(true);
        circle.setVisible(false);
        bluetoothButton.setVisible(false);
    }

    private void stopLoading() {
        progress.setVisible(false);
        circle.setVisible(true);
        bluetoothButton.setVisible(true);
    }

    private void login() {
        RConnectorService restService = restService();
        Call<UserResult> call = restService.login("test@gmail.com", "test");
        call.enqueue(new Callback<UserResult>() {
            @Override
            public void onResponse(Call<UserResult> call, Response<UserResult> response) {
                if (response != null) {
                    UserResult result = response.body();
                    if (result != null) {
                        userId = result.id;
                        // TODO fix flow
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResult> call, Throwable throwable) {

            }
        });
    }

    public static final Object lock = new Object();
    private boolean discovered;
    private DiscoveryListener discoveryListener = new DiscoveryListener() {

        @Override
        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass arg1) {
            String name;
            try {
                name = btDevice.getFriendlyName(false);
            } catch (Exception e) {
                name = btDevice.getBluetoothAddress();
            }

            remoteDevices.add(btDevice);

            Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList(remoteDevices.stream().map(device -> {
                    try {
                        return device.getFriendlyName(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return device.getBluetoothAddress();
                    }
                }).collect(Collectors.toList()));

                list.setVisible(true);
                list.setItems(items);
            });

            System.out.println("device found: " + name);
        }

        @Override
        public void inquiryCompleted(int arg0) {
            synchronized (lock) {
                lock.notify();
            }
        }

        @Override
        public void serviceSearchCompleted(int arg0, int arg1) {
            System.out.println("serviceSearchCompleted");

            if (!discovered) {
                discover(deviceName);
            }
        }

        @Override
        public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
            System.out.println("servicesDiscovered");

            discovered = true;

            Platform.runLater(() -> {
                list.setVisible(false);
                listButton.setVisible(false);
            });


            serviceRecords.addAll(Arrays.asList(arg1));

            for (ServiceRecord record : serviceRecords) {
                String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                System.out.println(url);
            }

            establishConnection();
        }
    };

    private void discover(String deviceName) {
        if (agent == null) {
            System.out.println("Discover: null agent");
            return;
        }

        try {
            for (RemoteDevice device : remoteDevices) {
                String name = device.getFriendlyName(false);
                //    if (name.equalsIgnoreCase("Railag")) {
                if (name.equalsIgnoreCase(deviceName)) {
                    UUID[] uuidSet = new UUID[1];
                    uuidSet[0] = new UUID(0x0100);
                    UUID[] uuids = new UUID[1];
                    // TODO replace for Arduino board UUID        uuids[0] = new UUID("0cbb85aa795141a6b891b2ee53960860", false);
                    // TODO fix transaction ids, multiple javax.bluetooth.BluetoothStateException: Already running 7 service discovery transactions
                    //        transactionSearchId = agent.searchServices(null, uuids, device, discoveryListener);
                    transactionSearchId = agent.searchServices(null, uuidSet, device, discoveryListener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void establishConnection() {
        for (ServiceRecord record : serviceRecords) {
            String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            System.out.println(url);
            if (url.contains("btspp")) {
                try {
                    StreamConnection connection = (StreamConnection) Connector.open(url, Connector.READ);

                    RemoteDeviceHelper.authenticate(record.getHostDevice());

                    Platform.runLater(this::stopLoading);
                    processConnectionThread = new ProcessConnectionThread(connection);
                    processConnectionThread.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class WaitThread extends Thread {

        /**
         * Constructor
         */
        public WaitThread() {
        }

        @Override
        public void run() {
            waitForConnection();
        }

        /**
         * Waiting for connection from devices
         */
        private void waitForConnection() {
            // retrieve the local Bluetooth device object

            try {
                // 1
                LocalDevice localDevice = LocalDevice.getLocalDevice();

                // 2
                agent = localDevice.getDiscoveryAgent();

                // 3
                agent.startInquiry(DiscoveryAgent.GIAC, discoveryListener);

                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Device Inquiry Completed. ");

               /* UUID[] uuids = new UUID[1];
                uuids[0] = new UUID(0x1101);*/

                Platform.runLater(() -> listButton.setVisible(true));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            if (agent != null) {
                agent.cancelInquiry(discoveryListener);
                //    agent.cancelServiceSearch(transactionSearchId);
                transactionSearchId = -1;
            }
        }
    }

    class ProcessConnectionThread extends Thread {

        private StreamConnection mConnection;

        // Constant that indicate command from devices
        private static final int EXIT_CMD = -1000;

        public ProcessConnectionThread(StreamConnection connection) {
            mConnection = connection;
        }


        int counter2 = 0;

        @Override
        public void run() {
            try {
                // prepare to receive data
                System.out.println("waiting for input");

                byte[] buffer = new byte[1024];
                int bytes;

                boolean halfDataEnd = false;
                boolean halfDataStart = false;
                String halfDouble = "";

                DataInputStream dataInputStream = mConnection.openDataInputStream();
                while (true) {

                    bytes = dataInputStream.read(buffer);

                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(buffer, 0, bytes);
                    System.out.println(readMessage);

                    counter2++;
                    System.out.println("Read calls: " + counter2);

                    if (readMessage.length() > 0) {
                        try {
                            for (int i = 0; i <= readMessage.length(); ) {
                                int doubleEndIndex = readMessage.indexOf(SEPARATOR, i);
                                if (doubleEndIndex == -1) {
                                    int doublePartLength = readMessage.length() - i;
                                    if (doublePartLength <= 0) {
                                        break; // end of line
                                    } else { // 1 char left, halfDataEnd stuff
                                        String substring = readMessage.substring(i, readMessage.length());
                                        halfDataEnd = true;
                                        halfDouble = substring;
                                        break;
                                    }
                                }

                                String substring = readMessage.substring(i, doubleEndIndex);
                                if (substring.length() == 0) { // "\r\n 0.47 ...", delimiter comes first -> ignore it
                                    i = doubleEndIndex + SEPARATOR.length();
                                    continue;
                                }
                                if (halfDataEnd) {
                                    halfDataEnd = false;
                                    if (!substring.contains(".")) { // e.g. when first number comes as ".40" and then full number "5.30" comes, ignore first occurence
                                        substring = halfDouble + substring; // usually just combine "temp" + "new number part"
                                    }
                                    halfDouble = "";
                                } else if (halfDataStart) {
                                    halfDataStart = false;
                                    substring = substring + halfDouble;
                                    halfDouble = "";
                                } else if (substring.length() == 3 || (substring.startsWith("-") && substring.length() == 4)) { // one char lost at the end, e.g. "0.3" and then goes "1" instead of "0.31"
                                    halfDataStart = true;                                                                         // additional case with "-" with format like "-0.3" and "1"
                                    halfDouble = substring;
                                    i = doubleEndIndex + SEPARATOR.length();
                                    continue;
                                } else if (substring.length() == 1) { // one char to be attached, e.g. "-" from "-0.39" or "0" from "0.25" and then will be followed with the second half of the data
                                    halfDataEnd = true;
                                    halfDouble = substring;
                                    i = doubleEndIndex + SEPARATOR.length();
                                    continue;
                                }

                                double value = Double.parseDouble(substring);

                                if (isX) {
                                    x.add(value);
                                } else {
                                    y.add(value);
                                }

                                isX = !isX;

                                i = doubleEndIndex + SEPARATOR.length();

                                counter++;

                                if (counter >= PACKAGE_SIZE) {
                                    counter = 0;
                                    xCurrent = new ArrayList<>(x.subList(x.size() - PACKAGE_SIZE / 2, x.size()));
                                    yCurrent = new ArrayList<>(y.subList(y.size() - PACKAGE_SIZE / 2, y.size()));
                                    System.out.println("moveCircle");

                                    Platform.runLater(() -> moveCircle(new ArrayList<>(xCurrent), new ArrayList<>(yCurrent)));

/*                            javafx.animation.Timeline timeline = new Timeline();
                            timeline.setCycleCount(Timeline.INDEFINITE);
                            timeline.setAutoReverse(true);

                            double previousX = circle.getLayoutX();
                            final KeyValue kv = new KeyValue(circle.layoutXProperty(), 300, Interpolator.LINEAR);
                            final KeyFrame kf = new KeyFrame(Duration.millis(500), kv);

                            final KeyValue kv2 = new KeyValue(circle.layoutXProperty(), previousX, Interpolator.LINEAR);
                            final KeyFrame kf2 = new KeyFrame(Duration.millis(600), kv2);
                            timeline.getKeyFrames().add(kf);
                            timeline.getKeyFrames().add(kf2);
                            timeline.play();
                            timeline.setOnFinished(event -> timeline.playFromStart());*/

                                }
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Stopped10");
                            e.printStackTrace();
                        }
                    }
                    //    double value = dataInputStream.readDouble();
                    //        System.out.println("Received: " + value);

       /*             if (value == EXIT_CMD) {
                        System.out.println("finish process");
                        break;
                    }
*/
                    if (!discovered) {
                        System.out.println("Stopped");
                        // stopped
                        break;
                    }

                }
            } catch (EOFException eofException) {
                // socket closed on server side
                //    eofException.printStackTrace();
                System.out.println("Bluetooth stopped EOF exception");
                stopBluetooth();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Stopped3");
            }
        }

        private void cancel() {
            System.out.println("Stopped4");

            if (mConnection != null) {
                try {
                    mConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void moveCircle(ArrayList<Double> xArray, ArrayList<Double> yArray) {
        for (int i = 0; i < xArray.size() && i < yArray.size(); i++) {
            double x = adjust(xArray.get(xArray.size() - 1), width, true);
            double y = adjust(yArray.get(yArray.size() - 1), height, false);

            // borders X
            if (x > width) {
                x = width - 30;
            } else if (x < 0) {
                x = 30;
            }

            // borders Y
            if (y > height) {
                y = height - 30;
            } else if (y < 0) {
                y = 30;
            }

            circle.setLayoutX(x);
            circle.setLayoutY(y);
            System.out.println("X: " + x + " Y: " + y);
        }
    }

    private void bluetoothInit() {
        BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG_STDOUT, "true");

        startLoading();

        waitThread = new WaitThread();
        waitThread.start();
    }

    private float adjust(double paramToAdjust, double maxValue, boolean inverse) { // 7 - xResolutionMax (e.g. 1920), 3 - y?
        //    7-1920
        //    paramToAdjust-x
        double value = (maxValue / 2) + (float) (paramToAdjust * maxValue / (THRESHOLD_ACCELEROMETER_MAX * 2));
        return inverse ? (float) Math.abs(maxValue - value) : (float) value;
    }

    private static Retrofit api() {
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

    public static RConnectorService restService() {
        if (rConnectorService == null)
            rConnectorService = createRetrofitService(RConnectorService.class);

        return rConnectorService;
    }

    private static <T> T createRetrofitService(final Class<T> clazz) {
        return api().create(clazz);
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void stopBluetooth() {
        System.out.println("Stopped5");

        if (waitThread != null) {
            waitThread.cancel();
        }

        if (processConnectionThread != null) {
            processConnectionThread.cancel();
        }

        discovered = false;

        Platform.runLater(() -> circle.setVisible(false));
    }
}