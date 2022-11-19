package test.beeloggerbluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import test.beeloggerbluetooth.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private BluetoothAdapter BA;
    private TextView textViewReceivedData;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private List<String> readMessagesList;
    private String readMessageBuffer;
    private final static String TAG = "BTConnectServ";
    private final static String appName = "beeloggerBluetooth";
    //private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-0800200c9a66");
    private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // meine alte app

    private EditText etSend;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;

    int debugInt = 0;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    String url1 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD1_1"; //beeloggerD1_2 beeloggerD2_1 beeloggerD2_2

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState

    ) {
        BluetoothManager BM = requireActivity().getSystemService(BluetoothManager.class);
        BA = BM.getAdapter();
        if (BA == null) {
            Toast.makeText(requireActivity().getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            // Device doesn't support Bluetooth
        }

        readMessageBuffer = "";
        readMessagesList = new ArrayList<>();
        mBTDevices = new ArrayList<>();
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        MyReceiver myReceiver = new MyReceiver();
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewReceivedData = (TextView) requireActivity().findViewById(R.id.textView_ReceivedData);
        textViewReceivedData.setMovementMethod(new ScrollingMovementMethod());

        etSend = (EditText) requireActivity().findViewById(R.id.editText_SendToDevice);


        binding.buttonSecond.setOnClickListener(view17 -> NavHostFragment.findNavController(SecondFragment.this)
                .navigate(R.id.action_SecondFragment_to_FirstFragment));

        binding.btConnect.setOnClickListener(view16 -> {

            if (mmDevice != null) {

                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }
                Log.d(TAG, "Connect to device:" + mmDevice.getName());
                start();
                startClient(mmDevice, UUIDString);

            } else {
                Log.d(TAG, "Connect to device error: device null");

            }
        });

        binding.btGetLastTime.setOnClickListener(view15 -> getWebsiteData());

        binding.btBT.setOnClickListener(view14 -> {
            if (BA.isEnabled()) {
                disableBT();
            } else {
                enableBT();
            }
        });

        binding.btSend.setOnClickListener(view13 -> {
            byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());

            write(bytes);

            String temp = textViewReceivedData.getText().toString();
            temp += "Send: " + etSend.getText().toString();
            temp += '\n';
            textViewReceivedData.setText(temp);
        });

        binding.btListBtDevices.setOnClickListener(view12 -> listBTDevices());

        pairedDevicesArrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.device_name);
        ListView listView = (ListView) requireActivity().findViewById(R.id.ListBtDevices);
        listView.setAdapter(pairedDevicesArrayAdapter);
        listView.setOnItemClickListener((av, view1, i, l) -> {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
                Log.d(TAG, "checkSelfPermission failed ");
            }
            BA.cancelDiscovery();

            Log.d(TAG, "onItemClick: You Clicked on a device.");
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                Log.d(TAG, "checkSelfPermission failed ");
            }
            String deviceName = mBTDevices.get(i).getName();
            String deviceAddress = mBTDevices.get(i).getAddress();

            Log.d(TAG, "onItemClick: deviceName = " + deviceName);
            Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

            //listView.;
            mmDevice = BA.getRemoteDevice(deviceAddress);

        });

        if (BA.isEnabled()) {
            binding.btBT.setTextColor(Color.BLUE);
        } else {
            binding.btBT.setTextColor(Color.LTGRAY);
        }

    }

    private void getWebsiteData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());


        executor.execute(() -> {

            String data = "";
            //Background work here
            try {
                Document document = Jsoup.connect(url1).get();
                data = document.select(".beeloggerD1_1_Sensor_Aktualisierung").text();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //GET /Mauchel1/Duo2/beelogger_log.php?

            String finalData = data;
            handler.post(() -> {

                showWebsiteData(finalData);
                //UI Thread work here
            });
        });
    }

    private void showWebsiteData(String data) {
        if (data.equals("")) {
            Toast.makeText(requireActivity().getApplicationContext(), "no data found", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireActivity().getApplicationContext(), data, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    ActivityResultLauncher<Intent> getResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                //val value = it.data?.getStringExtra("input")
            } else {

                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Please enable Bluetooth",
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    });


    private void enableBT() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getResult.launch(enableBtIntent);

    }

    private void disableBT() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        BA.disable();

    }

    private void listBTDevices() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }


        if (!BA.isEnabled()) {
            enableBT();
        }

        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();


        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            pairedDevicesArrayAdapter.clear();
            mBTDevices.clear();
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTDevices.add(device);
            }
        }
    }


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    //listBTDevices();
                    // Permission is granted. Continue the action or workflow in your app
                } else {

                    Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Needed for pairing with Beelogger BT Device. ",
                            Snackbar.LENGTH_SHORT).show();

                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    public class MyReceiver extends BroadcastReceiver {
        public MyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    binding.btBT.setTextColor(Color.LTGRAY);
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    binding.btBT.setTextColor(Color.BLUE);
                }
            }
        }
    }

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


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
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
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
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
                Log.d(TAG, "try to BT Connect... ");
                mmSocket.connect();
                binding.btConnect.setTextColor(Color.BLUE);
                Log.d(TAG, "BT Connected! ");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                Log.d(TAG, "BT Connection Failed! ");
                binding.btConnect.setTextColor(Color.LTGRAY);

                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connected(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                binding.btConnect.setTextColor(Color.LTGRAY);
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
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
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
            readMessagesList.clear();
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int numBytes; // bytes returned from read()

            while (true) {
                try {
                    numBytes = mmInStream.read(buffer);

                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            buffer);
                    readMsg.sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "Error reading Inputstream" + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            if (mmSocket != null && mmSocket.isConnected()) {
                Log.d(TAG, "write: Writing to outputstream: " + text);
                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "Error writing Outputstream" + e.getMessage());
                }
            } else {
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "Not connected to BT Device",
                        Snackbar.LENGTH_SHORT).show();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                binding.btConnect.setTextColor(Color.LTGRAY);
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

        mConnectedThread.write(out);

    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MessageConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if(readMessage.contains("\n")) {

                        readMessagesList.add(readMessageBuffer.concat(readMessage));
                        debugInt ++;
                        Log.d(TAG, "Receivcounter: " + debugInt);

                        //String temp = textViewReceivedData.getText().toString();
                        //temp += "Received: " + readMessageBuffer.concat(readMessage) + '\n';
                        //textViewReceivedData.setText(temp);

                        readMessageBuffer = "";

                    } else {
                        readMessageBuffer = readMessageBuffer.concat(readMessage);


            }



                    Log.d(TAG, "Input Stream: " + readMessage);
                    //mConversationArrayAdapter.add(readMessage);
                    break;
            }
        }
    };

}




