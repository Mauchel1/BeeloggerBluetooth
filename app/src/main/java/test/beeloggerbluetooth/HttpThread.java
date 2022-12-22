package test.beeloggerbluetooth;


import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class HttpThread extends Thread {

    private final static String TAG = "Beelogger HttpThread";
    String url2 = "http://community.beelogger.de/Mauchel1/Duo2/beelogger_log.php?PW=LogPW&Z=2&A=1&ID=WLAN_M_220924&M2_Data=2022/12/11_21:27:53,22.3,,,22.1,,,52.4,,-0.03,4.18,8.31,0.63,0.00,,,26.75"; //beeloggerD1_2 beeloggerD2_1 beeloggerD2_2 //TODO konfigurierbar machen
    private final FragmentActivity activity;

    public HttpThread(FragmentActivity activity) {
        this.activity = activity;
    }

    public void run() {
        httpStringRequest();
        //httpGetRequest();
    }

    private void httpStringRequest() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url2, //TODO echte Daten verwenden
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



