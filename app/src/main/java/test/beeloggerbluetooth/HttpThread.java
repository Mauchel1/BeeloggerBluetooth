package test.beeloggerbluetooth;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.List;

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
        SendLoop(CreateCoreUrl());
        //httpGetRequest();
    }

    private void SendLoop(String coreURL) {
        for (int i = startIndex; i < readMessagesList.size(); i += 5) {

            String datastring = CreateDataString(i, coreURL);
            Log.d(TAG, datastring);

            httpStringRequest(datastring);
        }
    }

    private String CreateCoreUrl() {

        String coreUrl = "http://";
        coreUrl = coreUrl.concat(pref.getString("Webserver", String.valueOf(R.string.Webserver)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Pfad", String.valueOf(R.string.Pfad)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Systemtyp", String.valueOf(R.string.Systemtyp)));
        coreUrl = coreUrl.concat("/");
        coreUrl = coreUrl.concat(pref.getString("Serverdatei", String.valueOf(R.string.Serverdatei)));
        coreUrl = coreUrl.concat("?PW=");
        coreUrl = coreUrl.concat(pref.getString("Password", String.valueOf(R.string.Password)));
        coreUrl = coreUrl.concat("&Z=");
        coreUrl = coreUrl.concat(pref.getString("Zeitsynchronisation", String.valueOf(R.string.Zeitsynchronisation)));
        coreUrl = coreUrl.concat("&A=");
        coreUrl = coreUrl.concat(pref.getString("Aux", String.valueOf(R.string.Aux)));
        coreUrl = coreUrl.concat("&ID=");
        coreUrl = coreUrl.concat(pref.getString("SketchID", String.valueOf(R.string.SketchID)));
        coreUrl = coreUrl.concat("&M");
        coreUrl = coreUrl.concat(pref.getString("Systemkennung", String.valueOf(R.string.Systemkennung)));
        coreUrl = coreUrl.concat("_Data=");

        return coreUrl;
        //return "http://community.beelogger.de/Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=";
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

        return datastring;

    }

    private void httpStringRequest(String datastring) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, datastring, //TODO resend if error
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



