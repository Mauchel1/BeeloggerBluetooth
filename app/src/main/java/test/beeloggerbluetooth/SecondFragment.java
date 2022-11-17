package test.beeloggerbluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
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
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import test.beeloggerbluetooth.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter BA;
    private BluetoothManager BM;
    private ListView listView;
    private TextView textViewReceivedData;
    private ArrayAdapter aAdapter;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private static String TAG = "BTConnectServ";
    private static String appName = "beeloggerBluetooth";
    //private static final UUID UUIDString = UUID.fromString("7128be52-61d4-11ed-9c9d-0800200c9a66");
    private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // meine alte app

    private EditText etSend;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;


    String url1 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD1_1";
    String url2 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD1_2";
    String url3 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD2_1";
    String url4 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD2_2";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState

    ) {
        //Context.BLUETOOTH_SERVICE);
        BM = getActivity().getSystemService(BluetoothManager.class);
        BA = BM.getAdapter();
        if (BA == null) {
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            // Device doesn't support Bluetooth
        }


        mBTDevices = new ArrayList<>();
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewReceivedData = (TextView) getActivity().findViewById(R.id.textView_ReceivedData);
        textViewReceivedData.setMovementMethod(new ScrollingMovementMethod());

        etSend = (EditText) getActivity().findViewById(R.id.editText_SendToDevice);



        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        binding.btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mmDevice != null) {

                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);

                        return;
                    }
                    Log.d(TAG, "Connect to device:" + mmDevice.getName());
                    start();
                    startClient(mmDevice, UUIDString);

                } else {
                    Log.d(TAG, "Connect to device error: device null");

                }
            }
        });

        binding.btGetLastTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getWebsiteData();

            }
        });

        binding.btBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BA.isEnabled()) {
                    disableBT();
                } else {
                    enableBT();
                }
            }
        });

        binding.btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());

                write(bytes);

                String temp = textViewReceivedData.getText().toString();
                temp += "Send: " + etSend.getText().toString();
                temp += '\n';
                textViewReceivedData.setText(temp);
            }
        });

        binding.btListBtDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listBTDevices();
            }
        });

        pairedDevicesArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.device_name);
        listView = (ListView) getActivity().findViewById(R.id.ListBtDevices);
        listView.setAdapter(pairedDevicesArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View view, int i, long l) {
//                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                    Log.d(TAG, "checkSelfPermission failed ");
//                    return; //BLUETOOTH_SCAN
//                }
//                BA.cancelDiscovery();

                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Log.d(TAG, "onItemClick: You Clicked on a device.");
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "checkSelfPermission failed ");
                    return;
                }
                String deviceName = mBTDevices.get(i).getName();
                String deviceAddress = mBTDevices.get(i).getAddress();

                Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

                //BluetoothDevice mmtestDevice = mBTDevices.get(i); //Todo eigentlich - testweise die zeile darunter
                mmDevice = BA.getRemoteDevice(deviceAddress);

                // Create the result Intent and include the MAC address
                //Intent intent = new Intent();
                //intent.putExtra("device_address", address);

                // Set result and finish this Activity
                //setResult(Activity.RESULT_OK, intent);
                //finish();
            }
        });


    }

    private void getWebsiteData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());


        executor.execute(new Runnable() {
            @Override
            public void run() {

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
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        showWebsiteData(finalData);
                        //UI Thread work here
                    }
                });
            }
        });
    }

    private void showWebsiteData(String data) {
        if (data.equals("")) {
            Toast.makeText(getActivity().getApplicationContext(), "no data found", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity().getApplicationContext(), data, Toast.LENGTH_SHORT).show();
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

                Snackbar.make(getActivity().findViewById(R.id.coordinatorLayout), "Please enable Bluetooth",
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    });


    private void enableBT() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getResult.launch(enableBtIntent);

    }

    private void disableBT() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            return;
        }
        BA.disable();

    }

    private void listBTDevices() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            return;
        }


        if (!BA.isEnabled()) {
            enableBT();
        }

        pairedDevices = BA.getBondedDevices();
        ArrayList deviceList = new ArrayList();


        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTDevices.add(device);
                //deviceList.add("Name: " + deviceName + " MAC Address: " + deviceHardwareAddress);

            }

            aAdapter = new ArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, deviceList);
            //listView.setAdapter(aAdapter);
            //listView.setOnClickListener();

        }
    }


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    listBTDevices();
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {

                    Snackbar.make(getActivity().findViewById(R.id.coordinatorLayout), "Needed for pairing with Beelogger BT Device. ",
                            Snackbar.LENGTH_SHORT).show();

                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });


    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
                connected(socket, mmDevice);
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
                // MY_UUID is the app's UUID string, also used in the server code.
                Log.d(TAG, "Create RfcommSocket with UUID " + deviceUUID);

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID); //TODO inseccure vs createRfcommSocketToServiceRecord
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            // Cancel discovery because it otherwise slows down the connection.
//            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
//            BA.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.d(TAG, "try to BT Connect... ");
                mmSocket.connect();
                Log.d(TAG, "BT Connected! ");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                Log.d(TAG, "BT Connection Failed! ");

                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connected(mmSocket, mmDevice);
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
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);

                    String temp = textViewReceivedData.getText().toString();
                    temp += "Received: " + incomingMessage + '\n';
                    textViewReceivedData.setText(temp);

                    Log.d(TAG, "Input Stream: " + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading Inputstream" + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing Outputstream" + e.getMessage());
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

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {

        mConnectedThread.write(out);

    }

}




