package test.beeloggerbluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import test.beeloggerbluetooth.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private BluetoothAdapter BA;
    private TextView textViewReceivedData;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private DeviceListAdapter pairedDevicesArrayAdapter;
    private List<String> readMessagesList;
    private final static String TAG = "Beelogger MainFragment";
    private final static String appName = "beeloggerBluetooth";
    private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // meine alte app

    public boolean isDebugMode() {
        return debugMode;
    }

    private boolean debugMode = false;

    private TextView etFilename;

    //private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    boolean inProgress;
    Menu menu;
    MyReceiver myReceiver;

    private LocalDateTime lastUploadTime;
    ProgressBar pb;
    TextView pbText;
    int lastArraySize;
    String filename;
    String currentData;

    public interface MessageConstants {
        int MESSAGE_LOG_ERROR = 0;
        int MESSAGE_LOG = 1;
        int MESSAGE_TOAST = 2;
        int RESPONSE_MESSAGE = 3;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        this.menu = menu;
        if (myReceiver != null) {
            myReceiver.setMenu(menu);
        }
        BluetoothButtonDisplay(menu.findItem(R.id.action_bt));
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState

    ) {
        setHasOptionsMenu(true);
        BluetoothManager BM = requireActivity().getSystemService(BluetoothManager.class);
        BA = BM.getAdapter();
        if (BA == null) {
            Toast.makeText(requireActivity().getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            // Device doesn't support Bluetooth
        }

        //TODO buttons disablen / enablen je nach nutzungserlaubnis

        lastUploadTime = LocalDateTime.MIN;
        inProgress = false;
        filename = "";
        readMessagesList = new ArrayList<>();
        mBTDevices = new ArrayList<>();
        binding = FragmentMainBinding.inflate(inflater, container, false);

        myReceiver = new MyReceiver(binding);
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewReceivedData = requireActivity().findViewById(R.id.textView_ReceivedData);
        textViewReceivedData.setMovementMethod(new ScrollingMovementMethod());

        etFilename = requireActivity().findViewById(R.id.etFilename);
        pb = requireActivity().findViewById(R.id.progressBar);
        pbText = requireActivity().findViewById(R.id.textView_Progressbar);

        binding.btConnect.setOnClickListener(view16 -> {

            if (mmDevice != null) {

                if (mmSocket != null && mmSocket.isConnected()) {
                    if (mConnectedThread != null && mConnectedThread.isAlive()) {
                        mConnectedThread.cancel();
                    }
                    if (mConnectThread != null && mConnectThread.isAlive()) {
                        mConnectThread.cancel();
                    }
                } else {
                    if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                    }
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "Connect to device:" + mmDevice.getName()).sendToTarget();
                    myReceiver.setBTDevice(mmDevice);
                    start();
                    startClient(mmDevice, UUIDString);
                }
            } else {
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "Connect to device error: device null").sendToTarget();
            }
        });

        binding.buttonUploadData.setEnabled(false);
        binding.buttonUploadData.setOnClickListener(view1 ->
        {
            int index = getIndexOfFirstListelementToSend();
            new HttpThread(requireActivity(), index, readMessagesList).start();
        });

        binding.buttonSave.setEnabled(false);
        binding.buttonSave.setOnClickListener(view1 -> {
            if (readMessagesList.size() > 1) {

                if (filename.endsWith(".csv") && !readMessagesList.get(0).contains(".csv")) {

                    File path = requireActivity().getFilesDir();
                    File filepath = new File(path, filename);

                    if (!filepath.exists()) {

                        try {
                            FileOutputStream outStream = new FileOutputStream(new File(path, filename));
                            for (String item : readMessagesList) {
                                outStream.write(item.getBytes());
                            }
                            outStream.close();
                            messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Data saved in File: " + filepath).sendToTarget();
                            messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, 1, 0, "Data saved in File: " + filepath).sendToTarget();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, 1, 0, "File already exists" + filepath).sendToTarget();
                        messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "File already exists: " + filepath).sendToTarget();
                    }
                } else {
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, 1, 0, "Filename not set or no Data to save").sendToTarget();
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Filename not set or no Data to save").sendToTarget();
                }
            }
        });

        binding.btGetLastTime.setOnClickListener(view15 -> getWebsiteData());

        binding.btConnect.setEnabled(false);
        binding.btSendFn.setEnabled(false);
        binding.btSendData.setEnabled(false);
        binding.btSendNf.setEnabled(false);
        binding.btSendFn.setOnClickListener(view1 -> sendToBTDevice("#"));
        binding.btSendData.setOnClickListener(view1 -> sendToBTDevice("?"));
        binding.btSendNf.setOnClickListener(view1 -> sendToBTDevice("*"));

        binding.btListBtDevices.setOnClickListener(view12 -> listBTDevices());

        pairedDevicesArrayAdapter = new DeviceListAdapter(requireActivity(), R.layout.device_name, mBTDevices);
        ListView lv_BtDevices = requireActivity().findViewById(R.id.ListBtDevices);
        lv_BtDevices.setAdapter(pairedDevicesArrayAdapter);
        myReceiver.setInitHeight(lv_BtDevices.getLayoutParams().height);
        myReceiver.setPairedDeviceAdapter(pairedDevicesArrayAdapter, lv_BtDevices);
        lv_BtDevices.setOnItemClickListener((av, view1, i, l) -> {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "checkSelfPermission failed").sendToTarget();
            }
            BA.cancelDiscovery();

            messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "You Clicked on a device:").sendToTarget();
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "checkSelfPermission failed").sendToTarget();
            }

            String deviceName = pairedDevicesArrayAdapter.getItem(i).getName();
            String deviceAddress = pairedDevicesArrayAdapter.getItem(i).getAddress();

            messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "deviceName = " + deviceName).sendToTarget();
            messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "deviceAddress = " + deviceAddress).sendToTarget();

            mmDevice = BA.getRemoteDevice(deviceAddress);
            if(mmDevice != null){
                binding.btConnect.setEnabled(true);
            }

        });

    }

    private void BluetoothButtonDisplay(MenuItem item) {

        if (BA.isEnabled()) {
            if (item != null) {
                item.setIcon(R.drawable.my_bluetooth);
            }
        } else {
            if (item != null) {
                item.setIcon(R.drawable.my_bluetooth_disabled);
            }
        }

    }

    public void BluetoothButtonHandling(MenuItem item) {

        if (BA.isEnabled()) {
            disableBT();
        } else {
            enableBT();
        }

        BluetoothButtonDisplay(item);
    }

    private void sendToBTDevice(String data) {
        if (!inProgress) {
            readMessagesList.clear();
            binding.buttonSave.setEnabled(false);
            binding.buttonUploadData.setEnabled(false);

            write(data.getBytes(Charset.defaultCharset()));
        }
    }

    private void enableBTButtons() {
        binding.btSendFn.setEnabled(true);
        binding.btSendNf.setEnabled(true);
        binding.btSendData.setEnabled(true);
    }

    private int getIndexOfFirstListelementToSend() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.GERMAN);
        LocalDateTime _time;
        int i;
        for (i = 0; i < readMessagesList.size(); i++) {

            try {
                _time = LocalDateTime.parse(readMessagesList.get(i).split(",")[0], formatter);
                if (_time.isAfter(lastUploadTime)) {
                    return i;
                }
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }

        }

        return -1;
    }

    private void getWebsiteData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());


        executor.execute(() -> {

            String data = "";
            //Background work here
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                Document document = Jsoup.connect(prefs.getString("ScraperCoreUrl", requireActivity().getResources().getString(R.string.defaultScraperUrl))).get();
                data = document.select(prefs.getString("ScraperSelect", requireActivity().getResources().getString(R.string.ScraperSelect))).text();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String finalData = data;
            handler.post(() -> {

                processWebsiteData(finalData);
                //UI Thread work here
            });
        });
    }

    private void processWebsiteData(String data) {
        if (data.equals("")) {
            Toast.makeText(requireActivity().getApplicationContext(), "no data found", Toast.LENGTH_SHORT).show();
            binding.etWebsiteTime.setText("not found");
        } else {

            Toast.makeText(requireActivity().getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            //11.12.2022 - 21:27:53

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss", Locale.GERMAN);

            try {
                lastUploadTime = LocalDateTime.parse(data, formatter);
                binding.etWebsiteTime.setText(lastUploadTime.toString());
            } catch (DateTimeParseException e) {
                Toast.makeText(requireActivity().getApplicationContext(), "data not parsed to date", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                binding.etWebsiteTime.setText("not parsable");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    ActivityResultLauncher<Intent> getResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK) {
            Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Please enable Bluetooth",
                    Snackbar.LENGTH_SHORT).show();
        }
    });


    private void enableBT() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getResult.launch(enableBtIntent);
    }

    private void disableBT() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            BA.disable();
        }
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "Debug Mode = " + debugMode).sendToTarget();

    }

    private void listBTDevices() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }


        if (!BA.isEnabled()) {
            enableBT(); //TODO besser Button disablen und enablen wenn bt an
        }

        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();


        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            pairedDevicesArrayAdapter.clear();
            binding.btConnect.setEnabled(false);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device);//.getName() + "\n" + device.getAddress());
            }
        }
    }


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {

                    Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Needed for pairing with Beelogger BT Device. ",
                            Snackbar.LENGTH_SHORT).show();

                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    // Not needed in Beelogger app - App always only client, Beelogger is Server
    /*
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                Log.d(TAG, "checkSelfPermission failed ");
            }

            try {
                tmp = BA.listenUsingInsecureRfcommWithServiceRecord(appName, UUIDString);
                Log.d(TAG, "Accept Thread with UUID: " + UUIDString);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "Run server socket");
            BluetoothSocket socket = null;
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
            }

            if (socket != null) {
                connected(socket);
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


     */


    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread started");

            // Use a temporary object that is later assigned to mmSocket because mmSocket is final.
            mmDevice = device;
            deviceUUID = uuid;

        }

        public void run() {

            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                Log.d(TAG, "Create RfcommSocket with UUID " + deviceUUID);

                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            // Cancel discovery because it otherwise slows down the connection.
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
            }
            BA.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "try to BT Connect... ").sendToTarget();
                mmSocket.connect();
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "BT Connected! ").sendToTarget();
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "BT Connected!",
                        Snackbar.LENGTH_SHORT).show();

                requireActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        // Stuff that updates the UI
                        enableBTButtons();
                    }
                });


            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "BT Connection Failed!").sendToTarget();
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "BT Connection Failed",
                        Snackbar.LENGTH_SHORT).show();


                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connected(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        /*
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        */
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            try {
                if (mmOutStream != null) {
                    mmOutStream.flush();
                }
            } catch (IOException e) {
                return;
            }

            readMessagesList.clear();
            binding.buttonSave.setEnabled(false);
            binding.buttonUploadData.setEnabled(false);
        }

        public void run() {
            //byte[] buffer = new byte[1024];

            //int numBytes; // bytes returned from read()
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(mmInStream));
            while (true) {
                try {

                    String resp = br.readLine();
                    //messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "deviceName = " + deviceName).sendToTarget();
                    Log.d(TAG, "Received: " + resp);

                    Message msg = new Message();
                    msg.what = MessageConstants.RESPONSE_MESSAGE;
                    msg.obj = resp;

                    messageHandler.sendMessage(msg);

                    //numBytes = mmInStream.read(buffer);

                    //Message readMsg = messageHandler.obtainMessage(
                    //        MessageConstants.MESSAGE_READ, numBytes, -1,
                    //        buffer);
                    //readMsg.sendToTarget();

                } catch (IOException e) {
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "reading Inputstream " + e.getMessage()).sendToTarget();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            if (mmSocket != null && mmSocket.isConnected()) {
                messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG, "writing to Outputstream: " + text).sendToTarget();
                try {
                    mmOutStream.write(bytes);
                    if (!progressBarHandler.hasCallbacks(runnableProgressBar)) {
                        progressBarHandler.post(runnableProgressBar);
                        lastArraySize = -1;
                        pb.setVisibility(View.VISIBLE);
                        inProgress = true;
                    }
                } catch (IOException e) {
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_LOG_ERROR, "writing Outputstream " + e.getMessage()).sendToTarget();
                }
            } else {
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Not connected to BT Device",
                        Snackbar.LENGTH_SHORT).show();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void connected(BluetoothSocket mmSocket) {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {

        if (mConnectedThread != null) {
            mConnectedThread.write(out);
        }
    }

    // Progressbar
    private final Handler progressBarHandler = new Handler(Looper.getMainLooper()) {
    };

    private final Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {

            inProgress = true;
            pb.setVisibility(View.VISIBLE);
            if (readMessagesList.size() - lastArraySize == 0) {
                textViewReceivedData.setText("");
                for (String rm : readMessagesList) {
                    textViewReceivedData.append(rm);
                }

                stopProgressBar();
            } else {
                lastArraySize = readMessagesList.size();
                pbText.setText(String.valueOf(readMessagesList.size()));
                progressBarHandler.postDelayed(this, 3000);
            }
        }
    };

    private void stopProgressBar() {

        pbText.setText("");
        inProgress = false;
        pb.setVisibility(View.GONE);
        progressBarHandler.removeCallbacks(runnableProgressBar);

        postDataReceivedTasks();
    }

    private void postDataReceivedTasks() {
        if (readMessagesList != null && readMessagesList.size() > 0) {
            if (readMessagesList.get(0).contains(".csv")) {
                filename = readMessagesList.get(0).split(".csv")[0].concat(".csv");
                etFilename.setText(filename);
                if (readMessagesList.size() == 2) {
                    currentData = readMessagesList.get(1);
                    ((MainActivity) requireActivity()).setCurrentData(currentData); // TODO insecure cast
                }
                readMessagesList.clear();
                binding.buttonSave.setEnabled(false);
                binding.buttonUploadData.setEnabled(false);
            } else {
                // datensatz
                if(binding.etWebsiteTime.getText().length() > 1){
                    binding.buttonUploadData.setEnabled(true);
                }
                binding.buttonSave.setEnabled(true);
            }
        }
    }

    public final Handler messageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MESSAGE_LOG:
                    Log.d(TAG, msg.obj.toString());
                    if(debugMode || msg.arg1 == 1)
                    {
                        textViewReceivedData.append(msg.obj.toString() + "\n");
                    }
                    break;
                case MessageConstants.MESSAGE_LOG_ERROR:
                    Log.e(TAG, msg.obj.toString());
                    if(debugMode)
                    {
                        textViewReceivedData.append("ERROR: " + msg.obj.toString() + "\n");
                    }
                    break;
                case MessageConstants.RESPONSE_MESSAGE:
                    readMessagesList.add(msg.obj.toString() + '\n');
                    break;
                case MessageConstants.MESSAGE_TOAST:
                    Toast.makeText(requireActivity().getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /*private class SocketSetup extends Thread {

        public void run() {
            if (ConnectSocket() > 0) {
                new SocketReceiver().start();
                new SocketSender().start();
            }
        }
    }

    private class SocketReceiver extends Thread {

        public void run() {
            try {
                dataInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (socket.isConnected()) {

                try {
                    int messageInt = dataInputStream.read();
                    //String message = String.valueOf(dataInputStream.read());

                    if (messageInt >= 0) { //null){
                        Log.d(TAG, "Received from Server: " + String.valueOf(messageInt));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private class SocketSender extends Thread {

        public void run() {
            SocketOperations();


        }
    }
     */


}




