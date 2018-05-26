package main.scene;

import com.intel.bluetooth.RemoteDeviceHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.shape.Circle;
import main.Main;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BluetoothScreen extends BaseScreen {

    private Button connectButton;
    private Button discoverButton;
    private Circle circle;
    private ProgressIndicator progressIndicator;
    private ListView<String> list;

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<>();
    private ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();

    private DiscoveryAgent agent;

    private WaitThread waitThread;
    private ProcessConnectionThread processConnectionThread;

    private String deviceName = "";

    private int transactionSearchId;

    public BluetoothScreen(Main main) {
        super(main, "bluetooth.fxml");
    }

    @Override
    protected void initViews() {
        connectButton = (Button) scene.lookup("#connectButton");
        discoverButton = (Button) scene.lookup("#discoverButton");
        circle = (Circle) scene.lookup("#circlee");
        progressIndicator = (ProgressIndicator) scene.lookup("#progress");
        list = (ListView) scene.lookup("#list");

        progressIndicator.setVisible(false);
        list.setVisible(false);


        discoverButton.setOnAction(event -> {
            if (discovered) {
                stopBluetooth();
            }

            bluetoothInit();
        });

        connectButton.setOnAction(event -> {
            deviceName = list.getSelectionModel().getSelectedItem();
            System.out.println("Device name: " + deviceName);
            discover(deviceName);
        });
    }

    private void bluetoothInit() {
        waitThread = new WaitThread();
        waitThread.start();
    }

    private void establishConnection() {
        for (ServiceRecord record : serviceRecords) {
            String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            System.out.println(url);
            if (url.contains("btspp")) {
                try {
                    StreamConnection connection = (StreamConnection) Connector.open(url, Connector.READ);

                    RemoteDeviceHelper.authenticate(record.getHostDevice());

            //        Platform.runLater(this::stopLoading);
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

                Platform.runLater(() -> connectButton.setVisible(true));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            if (agent != null) {
                agent.cancelInquiry(discoveryListener);
                //    agent.cancelServiceSearch(transactionSearchId);
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

                  /*  if (isX) {
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
                            Platform.runLater(Main.this::moveCircle);
*//*                            javafx.animation.Timeline timeline = new Timeline();
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
                            timeline.setOnFinished(event -> timeline.playFromStart());*//*
                        }
                    }
*/
                    if (value == EXIT_CMD) {
                        System.out.println("finish process");
                        break;
                    }

                    if (!discovered) {
                        // stopped
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
                connectButton.setVisible(false);
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
                    uuids[0] = new UUID("0cbb85aa795141a6b891b2ee53960860", false);
                    // TODO fix transaction ids, multiple javax.bluetooth.BluetoothStateException: Already running 7 service discovery transactions
                    transactionSearchId = agent.searchServices(null, uuids, device, discoveryListener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopBluetooth() {
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
