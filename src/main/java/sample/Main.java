package sample;

import com.google.gson.Gson;
import com.intel.bluetooth.RemoteDeviceHelper;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

public class Main extends Application {

    private static Retrofit api;
    private static RConnectorService rConnectorService;

    private final static int PACKAGE_SIZE = 5;

    private static int width = 1920; // default width
    private static int height = 1080; // default height

    private final static double THRESHOLD_ACCELEROMETER_MAX = 7.0;

    private Circle circle;
    private long userId = -1;

    private long latestEventId = -1;

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<RemoteDevice>();
    private ArrayList<ServiceRecord> serviceRecords = new ArrayList<ServiceRecord>();

    private ArrayList<Double> x = new ArrayList<Double>();
    private ArrayList<Double> y = new ArrayList<Double>();

    private ArrayList<Double> xCurrent;
    private ArrayList<Double> yCurrent;

    private boolean isX = true;
    private int counter = 0;

    private Stage primaryStage;
    private ProgressIndicator progress;
    private Button bluetoothButton;
    private DiscoveryAgent agent;

    private WaitThread waitThread;
    private ProcessConnectionThread processConnectionThread;
    private int transactionSearchId;

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

        // init bluetooth discover button
        bluetoothButton = new Button("Bluetooth start");
        bluetoothButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                bluetoothInit();
            }
        });
        bluetoothButton.setLayoutX(width / 2);
        bluetoothButton.setLayoutY(height / 2 - 50);

        Group root = new Group(circle, progress, bluetoothButton);

        primaryStage.setScene(new Scene(root, width, height));
        primaryStage.show();

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
                discover();
            }

            //        broadcastCommand("Hello world!");
        }

        @Override
        public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
            System.out.println("servicesDiscovered");

            discovered = true;

            serviceRecords.addAll(Arrays.asList(arg1));

            for (ServiceRecord record : serviceRecords) {
                String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                System.out.println(url);
            }

            establishConnection();
        }
    };

    private void discover() {
        if (agent == null) {
            System.out.println("Discover: null agent");
            return;
        }

        try {
            for (RemoteDevice device : remoteDevices) {
                String name = device.getFriendlyName(false);
                if (name.equalsIgnoreCase("Railag")) {
                    UUID[] uuidSet = new UUID[1];
                    uuidSet[0] = new UUID(0x0100);
                    UUID[] uuids = new UUID[1];
                    uuids[0] = new UUID("0cbb85aa795141a6b891b2ee53960860", false);
                    transactionSearchId = agent.searchServices(null, uuids, device, discoveryListener);
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

                    stopLoading();
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

                discover();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            if (agent != null) {
                agent.cancelInquiry(discoveryListener);
                agent.cancelServiceSearch(transactionSearchId);
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
                            xCurrent = new ArrayList<Double>(x.subList(x.size() - PACKAGE_SIZE, x.size()));
                            yCurrent = new ArrayList<Double>(y.subList(y.size() - PACKAGE_SIZE, y.size()));
                            System.out.println("moveCircle");
                            moveCircle();
                        }
                    }

                    if (value == EXIT_CMD) {
                        System.out.println("finish process");
                        break;
                    }

                }
            } catch (EOFException eofException) {
                // socket closed on server side
            //    eofException.printStackTrace();
                stopBluetooth();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            if (mConnection != null) {
                try {
                    mConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void moveCircle() {
        for (int i = 0; i < xCurrent.size() && i < yCurrent.size(); i++) {
            double x = adjust(xCurrent.get(xCurrent.size() - 1), width, true);
            double y = adjust(yCurrent.get(yCurrent.size() - 1), height, false);

            circle.setLayoutX(x);
            circle.setLayoutY(y);
            System.out.println("X: " + x + " Y: " + y);
        }
    }

    private void bluetoothInit() {
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
        if (waitThread != null) {
            waitThread.cancel();
        }

        if (processConnectionThread != null) {
            processConnectionThread.cancel();
        }

        discovered = false;

        circle.setVisible(false);
    }
}