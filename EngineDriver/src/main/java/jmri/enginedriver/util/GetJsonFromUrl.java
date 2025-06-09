/* modified from code by Junaid
   https://stackoverflow.com/questions/13196234/simple-parse-json-from-url-on-android-and-display-in-listview

 */

package jmri.enginedriver.util;

import android.os.AsyncTask;
import java.io.InputStream;

import jmri.enginedriver.threaded_application;

public class GetJsonFromUrl extends AsyncTask<String, String, Void> {

    InputStream inputStream = null;
    String result = "";
    String parseResult;
    public threaded_application mainapp;  // hold pointer to mainapp

    protected void onPreExecute() {
    }

    public GetJsonFromUrl(threaded_application myApp) {
        mainapp = myApp;
    }

    @Override
    protected Void doInBackground(String... params) {

//        String url_select = params[0];
//
//        ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
//
//        try {
//            // Set up HTTP post
//
//            // HttpClient is more or less deprecated. Need to change to URLConnection
//            HttpClient httpClient = new DefaultHttpClient();
//
//            HttpPost httpPost = new HttpPost(url_select);
//            httpPost.setEntity(new UrlEncodedFormEntity(param));
//            HttpResponse httpResponse = httpClient.execute(httpPost);
//            HttpEntity httpEntity = httpResponse.getEntity();
//
//            // Read content & Log
//            inputStream = httpEntity.getContent();
//        } catch (UnsupportedEncodingException e1) {
//            Log.e(threaded_application.applicationName, activityName + ": UnsupportedEncodingException: " + e1.toString());
//            e1.printStackTrace();
//        } catch (ClientProtocolException e2) {
//            Log.e(threaded_application.applicationName, activityName + ": ClientProtocolException", e2.toString());
//            e2.printStackTrace();
//        } catch (IllegalStateException e3) {
//            Log.e(threaded_application.applicationName, activityName + ": IllegalStateException", e3.toString());
//            e3.printStackTrace();
//        } catch (IOException e4) {
//            Log.e(threaded_application.applicationName, activityName + ": IOException", e4.toString());
//            e4.printStackTrace();
//        }
//        // Convert response to string using String Builder
//        try {
//            BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
//            StringBuilder sBuilder = new StringBuilder();
//
//            String line = null;
//            while ((line = bReader.readLine()) != null) {
//                sBuilder.append(line + "\n");
//            }
//
//            inputStream.close();
//            result = sBuilder.toString();
//            String serverName = parseDataOneValue("name");
//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.HTTP_SERVER_NAME_RECEIVED, serverName);  // 2=toggle
//
//        } catch (Exception e) {
//            Log.e(threaded_application.applicationName, activityName + ": StringBuilding & BufferedReader: " + "Error converting result " + e.toString());
//        }
        return null;
    } // protected Void doInBackground(String... params)

//    private String parseDataOneValue(String which) {
//        parseResult  = "";
//        //parse JSON data
//        try {
//            JSONObject object = new JSONObject(result);
//            JSONObject oData = object.getJSONObject("data");
////            JSONArray jArray = new JSONArray(oData.getString("name"));
//            parseResult = oData.getString(which);
//        } catch (JSONException e) {
//            Log.e(threaded_application.applicationName, activityName + ": JSONException: Error: " + e.toString());
//        } // catch (JSONException e)
//
//        return parseResult;
//    }

    protected void onPostExecute(Void v) {
    } // protected void onPostExecute(Void v)
} //class MyAsyncTask extends AsyncTask<String, String, Void>