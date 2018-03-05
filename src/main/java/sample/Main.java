package sample;

import com.google.gson.Gson;
import com.intel.bluetooth.RemoteDeviceHelper;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.shape.Circle;
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

    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;

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

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        //    Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        //    primaryStage.setTitle("Hello World");


        // init circle
        circle = new Circle();
        circle.setCenterX(0.0f);
        circle.setCenterY(0.0f);
        circle.setRadius(15.0f);
        circle.setVisible(false);

        // init loading indicator
        progress = new ProgressIndicator();
        progress.setPrefSize(100, 100);
        progress.setLayoutX(WIDTH / 2);
        progress.setLayoutY(HEIGHT / 2);
        progress.setVisible(false);

        // init bluetooth discover button
        bluetoothButton = new Button("Bluetooth start");
        bluetoothButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                bluetoothInit();
            }
        });
        bluetoothButton.setLayoutX(WIDTH / 2);
        bluetoothButton.setLayoutY(HEIGHT / 2);

        Group root = new Group(circle, progress, bluetoothButton);

        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
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

            //        broadcastCommand("Hello world!");
        }

        @Override
        public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
            System.out.println("servicesDiscovered");

            serviceRecords.addAll(Arrays.asList(arg1));

            for (ServiceRecord record : serviceRecords) {
                String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                System.out.println(url);
            }

            establishConnection();
        }
    };

    private void establishConnection() {
        for (ServiceRecord record : serviceRecords) {
            String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            System.out.println(url);
            if (url.contains("btspp")) {
                try {
                    StreamConnection connection = (StreamConnection) Connector.open(url, Connector.READ);

                    RemoteDeviceHelper.authenticate(record.getHostDevice());

                    stopLoading();
                    ProcessConnectionThread processConnectionThread = new ProcessConnectionThread(connection);
                    processConnectionThread.start();
                    /*DataInputStream dataInputStream = connection.openDataInputStream();

                    ArrayList<Double> x = new ArrayList<Double>();

                    while (true) { // waiting for data
                        try {
                            for (int i = 0; i < 10; i++) {
                                x.add(dataInputStream.readDouble());
                            }

                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    for (Double d : x) {
                        System.out.println(d);
                    }*/
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
                DiscoveryAgent agent = localDevice.getDiscoveryAgent();

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

                for (RemoteDevice device : remoteDevices) {
                    String name = device.getFriendlyName(false);
                    if (name.equalsIgnoreCase("Railag")) {
                        UUID[] uuidSet = new UUID[1];
                        uuidSet[0] = new UUID(0x0100);
                        int[] attrIds = {0x0100};
                        //       agent.searchServices(attrIds, uuidSet, device, discoveryListener);
                        UUID[] uuids = new UUID[1];
                        uuids[0] = new UUID("0cbb85aa795141a6b891b2ee53960860", false);
                        agent.searchServices(null, uuids, device, discoveryListener);

                       /* String bt_addr = device.getBluetoothAddress(); //remDev is the remote device; I am sure it's the correct one
                        try {
                            StreamConnection connection = (StreamConnection) Connector.open("btspp://" + bt_addr + ":1;authenticate=false;encrypt=false;master=true;");
                            System.out.println("connected");
                            InputStream inputStream = connection.openInputStream();
                            System.out.println("inputStream");
                        } catch (javax.bluetooth.BluetoothConnectionException e) {
                            e.printStackTrace();
                        }*/
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            /*LocalDevice local;

            StreamConnectionNotifier notifier;
            StreamConnection connection;

            // setup the server to listen for connection
            try {
                local = LocalDevice.getLocalDevice();
                local.setDiscoverable(DiscoveryAgent.GIAC);



                UUID uuid = new UUID(80087355); // "04c6093b-0000-1000-8000-00805f9b34fb"
                String url = "btspp://localhost:" + uuid.toString() + ";name=RemoteBluetooth";
                notifier = (StreamConnectionNotifier) Connector.open(url);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            // waiting for connection
            while(true) {
                try {
                    System.out.println("waiting for connection...");
                    connection = notifier.acceptAndOpen();

                    Thread processThread = new Thread(new ProcessConnectionThread(connection));
                    processThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }*/
        }
    }

    class ProcessConnectionThread extends Thread {

        private StreamConnection mConnection;

        // Constant that indicate command from devices
        private static final int EXIT_CMD = -1000;
        private static final int KEY_RIGHT = 1;
        private static final int KEY_LEFT = 2;

        public ProcessConnectionThread(StreamConnection connection) {
            mConnection = connection;
        }

        @Override
        public void run() {
            try {
                // prepare to receive data
                //    InputStream inputStream = mConnection.openInputStream();

                System.out.println("waiting for input");

                DataInputStream dataInputStream = mConnection.openDataInputStream();
                while (true) {

                    //    int command = inputStream.read();
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

                    processCommand(value);
                }
            } catch (EOFException eofException) {
                // socket closed on server side

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Process the command from client
         *
         * @param command the command code
         */
        private void processCommand(double command) {
            try {
               /* Robot robot = new Robot();
                switch (command) {
                    case KEY_RIGHT:
                        robot.keyPress(KeyEvent.VK_RIGHT);
                        System.out.println("Right");
                        break;
                    case KEY_LEFT:
                        robot.keyPress(KeyEvent.VK_LEFT);
                        System.out.println("Left");
                        break;
                }*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void moveCircle() {
        //    for (int i = 0; i < xCurrent.size() && i < yCurrent.size(); i++) {
        double x = adjust(xCurrent.get(xCurrent.size() - 1), WIDTH, true);
        double y = adjust(yCurrent.get(yCurrent.size() - 1), HEIGHT, false);

        circle.setLayoutX(x);
        circle.setLayoutY(y);
        System.out.println("X: " + x + " Y: " + y);
        //     }

     /*   try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    private void bluetoothInit() {
        startLoading();

        WaitThread thread = new WaitThread();
        thread.start();

       /* RConnectorService restService = restService();
        Call<AccelerometerResult> call = restService.fetchAccelerometerData(userId); // 292
        call.enqueue(new Callback<AccelerometerResult>() {
            @Override
            public void onResponse(Call<AccelerometerResult> call, Response<AccelerometerResult> response) {
                AccelerometerResult result = response.body();
                if (result == null) {
                    //            onError(new IllegalArgumentException());
                    return;
                }
                if (result.invalid()) {
                    //            toast(result.error);
                    return;
                }

                List<AccelerometerResult.AccelerometerResults> resultsList = result.accelerometerResults;
                for (AccelerometerResult.AccelerometerResults r : resultsList) {
                    int eventId = r.id;
                    if (eventId > latestEventId) {
                        latestEventId = eventId;
                    } else if (latestEventId == eventId) {
                        continue;
                    }

                    List<Double> xArray = r.x;
                    List<Double> yArray = r.y;
                    for (int i = 0; i < xArray.size() && i < yArray.size(); i++) {
                        double x = adjust(xArray.get(i), WIDTH, true);
                        double y = adjust(yArray.get(i), HEIGHT, false);

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        circle.setLayoutX(x);
                        circle.setLayoutY(y);
                        System.out.println("X: " + x + " Y: " + y);
                    }
                }

                bluetoothInit();
            }

            @Override
            public void onFailure(Call<AccelerometerResult> call, Throwable throwable) {
                throwable.printStackTrace();
            }
        });*/
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
}