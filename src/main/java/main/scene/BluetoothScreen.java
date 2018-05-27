package main.scene;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.shape.Circle;
import main.Main;

import javax.bluetooth.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BluetoothScreen extends BaseScreen {

    private Button connectButton;
     ListView<String> list;

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<>();
    private ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();

    private DiscoveryAgent agent;

    private WaitThread waitThread;

    private String deviceName = "";

    private int transactionSearchId;

    public BluetoothScreen(Main main) {
        super(main, "bluetooth.fxml");
    }

    @Override
    protected void initViews() {
        connectButton = (Button) scene.lookup("#connectButton");
        list = (ListView<String>) scene.lookup("#list");

        list.setVisible(false);
        connectButton.setVisible(false);

        startLoading();
        if (discovered) {
            stopBluetooth();
        }

        bluetoothInit();

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
                if (main != null) {
                    Platform.runLater(() -> {
                        main.initBluetooth(url, record);
                        main.toTests();
                    });

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

                connectButton.setVisible(true);
                list.setVisible(true);
                list.setItems(items);
            });

            stopLoading();

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

        discovered = false;
    }
}
