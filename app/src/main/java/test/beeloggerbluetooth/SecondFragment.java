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
import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import test.beeloggerbluetooth.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private BufferedReader dataInputStream;
    private PrintWriter dataOutputStream;
    private Socket socket;

    private FragmentSecondBinding binding;
    private BluetoothAdapter BA;
    private TextView textViewReceivedData;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private List<String> readMessagesList;
    private String readMessageBuffer;
    private final static String TAG = "BTConnectServ";
    private final static String appName = "beeloggerBluetooth";
    //private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-0800200c9a66");  //geht nicht
    private static final UUID UUIDString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // meine alte app

    private TextView etFilename;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    boolean inProgress;

    ProgressBar pb;
    TextView pbText;
    int lastArraySize;
    String filename;
    String currentData;

    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
        int RESPONSE_MESSAGE = 3;

        // ... (Add other message types here as needed.)
    }

    String url1 = "https://community.beelogger.de/Mauchel1/scraper.php?pfad=beeloggerD1_1"; //beeloggerD1_2 beeloggerD2_1 beeloggerD2_2
    String url2 = "http://community.beelogger.de/Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=2022/12/11_21:27:53,22.3,,,22.1,,,52.4,,-0.03,4.18,8.31,0.63,0.00,,,26.75"; //beeloggerD1_2 beeloggerD2_1 beeloggerD2_2
    String host = "community.beelogger.de";

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

        inProgress = false;
        readMessageBuffer = "";
        filename = "";
        readMessagesList = new ArrayList<>();
        mBTDevices = new ArrayList<>();
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        MyReceiver myReceiver = new MyReceiver();
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        requireActivity().registerReceiver(myReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewReceivedData = requireActivity().findViewById(R.id.textView_ReceivedData);
        textViewReceivedData.setMovementMethod(new ScrollingMovementMethod());

        etFilename = requireActivity().findViewById(R.id.etFilename);
        pb = requireActivity().findViewById(R.id.progressBar);
        pbText = requireActivity().findViewById(R.id.textView_Progressbar);


        //binding.buttonSecond.setOnClickListener(view17 -> NavHostFragment.findNavController(SecondFragment.this).navigate(R.id.action_SecondFragment_to_FirstFragment));

        binding.btConnect.setOnClickListener(view16 -> { //TODO if connected --> disconnect

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


        binding.buttonUploadData.setOnClickListener(view1 ->
        {
            //new SocketSetup().start(); //TODO welches geht?
            new HttpThread().start();
        });

        binding.buttonSave.setOnClickListener(view1 -> {
            if (readMessagesList.size() > 1) {

                if (filename.contains(".csv") && !readMessagesList.get(0).contains(".csv")) { //TODO zweiter teil so richtig? kommt als 0tes element der dateiname beim senden von #?


                    File path = requireActivity().getFilesDir();
                    //filename = "testfile.csv";
                    File filepath = new File(path + filename);


                    if (!filepath.exists()) {

                        try {
                            FileOutputStream outStream = new FileOutputStream(new File(path, filename));
                            for (String item : readMessagesList) {
                                outStream.write(item.getBytes());
                            }
                            outStream.close();
                            messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Data saved in File: " + filepath).sendToTarget();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "File already exists: " + filepath).sendToTarget(); //TODO Kommt nicht
                    }
                } else {
                    messageHandler.obtainMessage(MessageConstants.MESSAGE_TOAST, "No Data to save").sendToTarget();
                }
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

        binding.btSendFn.setOnClickListener(view1 -> sendToBTDevice("#"));
        binding.btSendData.setOnClickListener(view1 -> {
            sendToBTDevice("?");
        });
        binding.btSendNf.setOnClickListener(view1 -> sendToBTDevice("*"));

        binding.btListBtDevices.setOnClickListener(view12 -> listBTDevices());

        pairedDevicesArrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.device_name);
        ListView listView = requireActivity().findViewById(R.id.ListBtDevices);
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

    private void sendToBTDevice(String data) {
        if (!inProgress) {
            readMessagesList.clear();

            write(data.getBytes(Charset.defaultCharset()));

            /*
            String temp = ""; //textViewReceivedData.getText().toString();
            temp += "Send: " + etFilename.getText().toString();
            temp += '\n';
            textViewReceivedData.setText(temp);
            */
        }
    }

    private void getWebsiteData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());


        executor.execute(() -> {

            String data = "";
            //Background work here
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                Document document = Jsoup.connect(prefs.getString("ScraperCoreUrl", requireActivity().getResources().getString(R.string.defaultScraperUrl))).get(); //TODO change default
                data = document.select(prefs.getString("ScraperSelect", requireActivity().getResources().getString(R.string.ScraperSelect))).text();
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


    private void httpStringRequest() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url2,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());

                    }
                }) {
            /*@Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("data", s);
                return params;
            }*/
        };
        RequestQueue requestQueue = Volley.newRequestQueue(requireActivity());
        requestQueue.add(stringRequest);
    }

    /*private void httpGetRequest() {

        // GET /Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=2022/11/27_21:27:53,22.3,,,22.1,,,52.4,,-0.03,4.18,8.31,0.63,0.00,,,26.752022/11/27_21:27:53,22.3,,,22.1,,,52.4,,-0.03,4.18,8.31,0.63,0.00,,,26.75

        try {
            URL url = new URL("http://community.beelogger.de");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setDoInput(true);
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inStream = new BufferedInputStream(urlConnection.getInputStream());
                    InputStreamReader inReader = new InputStreamReader(inStream);
                    //InputStream in = url.openStream();
                    OutputStream outStream = new BufferedOutputStream(urlConnection.getOutputStream());
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outStream);
                    outputStreamWriter.write("GET /Mauchel1/Duo2/beelogger_log.php?");
                    outputStreamWriter.flush();
                    outputStreamWriter.close();

                    BufferedReader reader = new BufferedReader(inReader);
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    Log.d(TAG, result.toString());
                } else {
                    Log.e(TAG, "Fail (" + responseCode + ")");
                }
*/
            /*try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                InputStream errin = new BufferedInputStream(urlConnection.getErrorStream());
                //readStream(in);
            }*/
/*
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
*/
/*
    private int ConnectSocket() {
        try {
            socket = new Socket(host, 80);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (socket.isConnected()) {
            Log.d(TAG, "Socket connected to " + host);
            return 1;
        }//TODO retry connect x times
        else {
            return -1;
        }
    }

    private void SocketOperations() {

        try {
            dataOutputStream = new PrintWriter(socket.getOutputStream());

            //dataOutputStream = new DataOutputStream( new BufferedOutputStream( socket.getOutputStream() ) );

            //dataOutputStream.write("GET /Mauchel1/Duo2/beelogger_log.php?"); //TODO hardcoded string
            //dataOutputStream.flush();
            dataOutputStream.write("http://community.beelogger.de/Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=2022/12/03_21:40:08,22.3,,,22.1,,,52.5,,-0.04,4.17,8.33,0.23,18.00,,,22.50"); //TODO hardcoded string
            //dataOutputStream.write("GET /Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=2022/12/03_21:40:08,22.3,,,22.1,,,52.5,,-0.04,4.17,8.33,0.23,18.00,,,22.50"); //TODO hardcoded string
            dataOutputStream.flush();
            //dataOutputStream.write(" HTTP/1.1"); //TODO hardcoded string
            //dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    */

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
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    binding.btBT.setTextColor(Color.LTGRAY);
                } else if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothAdapter.STATE_ON) {
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
                Log.d(TAG, "try to BT Connect... ");
                mmSocket.connect();
                binding.btConnect.setTextColor(Color.BLUE);
                Log.d(TAG, "BT Connected! ");
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "BT Connected!",
                        Snackbar.LENGTH_SHORT).show();

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                Log.d(TAG, "BT Connection Failed! ");
                Snackbar.make(requireActivity().findViewById(R.id.coordinatorLayout), "BT Connection Failed",
                        Snackbar.LENGTH_SHORT).show();

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

            try { //TODO was das?
                mmOutStream.flush();
            } catch (IOException e) {
                return;
            }

            readMessagesList.clear();
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int numBytes; // bytes returned from read()
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(mmInStream));
            while (true) {
                try {

                    String resp = br.readLine();
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
                    if (!progressBarHandler.hasCallbacks(runnableProgressBar)) {
                        progressBarHandler.post(runnableProgressBar);
                        lastArraySize = -1;
                        pb.setVisibility(View.VISIBLE);
                        inProgress = true;
                    }

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

        if (mConnectedThread != null) {
            mConnectedThread.write(out);
        }
    }


    private final Handler progressBarHandler = new Handler(Looper.getMainLooper()) {};

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
        if (readMessagesList.get(0).contains(".csv")) { //TODO insecure!
            filename = readMessagesList.get(0);
            etFilename.setText(filename);
            currentData = readMessagesList.get(1); //TODO insecure! size >= 2 und size < 3
            readMessagesList.clear();
        }
    }

    private final Handler messageHandler = new Handler(Looper.getMainLooper()) {
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

                    if (readMessage.contains("\n")) {

                        readMessagesList.add(readMessageBuffer.concat(readMessage));

                        readMessageBuffer = "";

                    } else {
                        readMessageBuffer = readMessageBuffer.concat(readMessage);
                    }
                    Log.d(TAG, "Input Stream: " + readMessage);
                    //mConversationArrayAdapter.add(readMessage);
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

    private class HttpThread extends Thread {

        public void run() {
            httpStringRequest();
            //httpGetRequest();
        }
    }


}




