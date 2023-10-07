package test.beeloggerbluetooth;


import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpThread extends Thread {

    private final static String TAG = "Beelogger HttpThread";
    private final FragmentActivity activity;
    private final int startIndex;
    private final List<String> readMessagesList;
    SharedPreferences pref;

    public HttpThread(FragmentActivity activity, int startIndex, List<String> readMessagesList) {
        this.readMessagesList = readMessagesList;
        this.startIndex = startIndex;
        this.activity = activity;
        pref = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
    }

    public void run() {

        if(readMessagesList.size() > 0 && startIndex >= 0){
            SendLoop(CreateCoreUrl());
        }
    }

    private void showAlert(){
        Looper.prepare();
        new AlertDialog.Builder(activity)
                .setTitle("Reminder")
                .setMessage("Make sure to switch Off Service-Switch on Beelogger!")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, null) /*new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })*/

                // A null listener allows the button to dismiss the dialog and take no further action.
                //.setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        Looper.loop();
    }

    private void SendLoop(String coreURL) {
        for (int i = startIndex; i < readMessagesList.size(); i += 5) {
        //TODO log prozess during upload
            String datastring = CreateDataString(i, coreURL);
            Log.d(TAG, datastring);

            httpStringRequestSynchron(datastring, 0);
            if(i%25 == 0) {
                ((MainActivity) activity).mainFragment.messageHandler.obtainMessage(MainFragment.MessageConstants.PROGRESS, i+":"+readMessagesList.size()).sendToTarget();
            }
        }
        ((MainActivity) activity).mainFragment.messageHandler.obtainMessage(MainFragment.MessageConstants.MESSAGE_LOG, 1,0, "Data Send! ").sendToTarget();
        ((MainActivity) activity).mainFragment.messageHandler.obtainMessage(MainFragment.MessageConstants.PROGRESS, "done").sendToTarget();
        showAlert();
    }

    private String CreateCoreUrl() {

        String coreUrl = "http://";
        coreUrl = coreUrl.concat(pref.getString("Webserver", activity.getResources().getString(R.string.Webserver)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Pfad", activity.getResources().getString(R.string.Pfad)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Systemtyp", activity.getResources().getString(R.string.Systemtyp)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Serverdatei", activity.getResources().getString(R.string.Serverdatei)));
        coreUrl = coreUrl.concat("?PW=");
        coreUrl = coreUrl.concat(pref.getString("Password", activity.getResources().getString(R.string.Password)));
        coreUrl = coreUrl.concat("&Z=");
        coreUrl = coreUrl.concat(pref.getString("Zeitsynchronisation", activity.getResources().getString(R.string.Zeitsynchronisation)));
        coreUrl = coreUrl.concat("&A=");
        coreUrl = coreUrl.concat(pref.getString("Aux", activity.getResources().getString(R.string.Aux)));
        coreUrl = coreUrl.concat("&ID=");
        coreUrl = coreUrl.concat(pref.getString("SketchID", activity.getResources().getString(R.string.SketchID)));
        coreUrl = coreUrl.concat("&M");
        coreUrl = coreUrl.concat(pref.getString("Systemkennung", activity.getResources().getString(R.string.Systemkennung)));
        coreUrl = coreUrl.concat("_Data=");

        return coreUrl;
    }

    private String CreateDataString(int startIndex, String coreURL) {

        String datastring = coreURL;

        for (int i = startIndex; i < startIndex + 5; i++) {

            if (i < readMessagesList.size()) {
                String message = readMessagesList.get(i).replace(' ', '_').replace("\n", "");
                if (message.endsWith(",,")) {
                    message = message.substring(0, message.length() - 1);
                }
                if (message.endsWith(",,")) {
                    message = message.substring(0, message.length() - 1);
                }
                datastring = datastring.concat(message);
            }
        }

        if (datastring.endsWith(",")) {
            datastring = datastring.substring(0, datastring.length() - 1);
        }

        return datastring;

    }

    private void httpStringRequestSynchron(String datastring, int retryNumber) {

        RequestFuture<String> future = RequestFuture.newFuture();

        if (retryNumber < 3) {

            StringRequest request = new StringRequest(Request.Method.GET, datastring, future, future);

            RequestQueue requestQueue = Volley.newRequestQueue(activity);
            requestQueue.add(request);

            try {
                String response = future.get(10, TimeUnit.SECONDS);
                ((MainActivity) activity).mainFragment.messageHandler.obtainMessage(MainFragment.MessageConstants.MESSAGE_LOG, "Response from Server: " + response).sendToTarget();
                if (response.contains("ok *")) {
                    Log.d(TAG, response);
                } else {
                    httpStringRequestSynchron(datastring, retryNumber+1);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                ((MainActivity) activity).mainFragment.messageHandler.obtainMessage(MainFragment.MessageConstants.MESSAGE_LOG_ERROR, e.getMessage()).sendToTarget();
            }
        }
    }


    private void httpStringRequestAsynchron(String datastring) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, datastring,
                response -> Log.d(TAG, response),
                error -> Log.d(TAG, error.toString())) {
            /*@Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("data", s);
                return params;
            }*/
        };
        RequestQueue requestQueue = Volley.newRequestQueue(activity);
        requestQueue.add(stringRequest);
    }


    /*
    private void httpGetRequest() {

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
}



