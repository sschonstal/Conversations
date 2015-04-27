package eu.siacs.conversations.cdk.iam;

import android.os.AsyncTask;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import eu.siacs.conversations.Config;


public class Iam {

    private static Iam instance = null;
    private static DefaultHttpClient httpclient;
    private String iamLoginServer = "login-stage.adpedge.com";
    private String iamApiServer = "api-stage.adpedge.com";

    private String token = null;
    private String jid;

    protected Iam(){
        httpclient = new DefaultHttpClient();
    }

    public static Iam getInstance() {
        if (instance == null) {
            instance = new Iam();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public String getJid() {
        return jid;
    }

    public void login(String username, String password) {
        HttpResponse response = postLoginOauth(username, password);
        getSmofcCookie(response);
        getJid(response);
        return;
    }

    private String getSmofcCookie(HttpResponse response) {
        Header cookies[] = response.getHeaders("set-cookie");
        for (Header header : cookies) {
            String[] rawCookieParams = header.getValue().split(";");
            if (rawCookieParams.length > 0) {
                String[] rawCookieNameAndValue = rawCookieParams[0].split("=");

                if (rawCookieNameAndValue.length > 1 && rawCookieNameAndValue[0].equals("SMOFC")) {
                    token = rawCookieNameAndValue[1];
                }
            }
        }
        return token;
    }

    private String getJid(HttpResponse response) {
        Header cookies[] = response.getHeaders("set-cookie");
        for (Header header : cookies) {
            String[] rawCookieParams = header.getValue().split(";");
            if (rawCookieParams.length > 0) {
                String[] rawCookieNameAndValue = rawCookieParams[0].split("=");

                if (rawCookieNameAndValue.length > 1 && rawCookieNameAndValue[0].equals("JID")) {
                    jid = rawCookieNameAndValue[1];
                }
            }
        }
        return jid;
    }

    public HttpResponse postLoginOauth(String username, String password) {
        HttpResponse retval = null;

        HttpPost httppost = new HttpPost("https://" + iamLoginServer + "/oauth/getorcreatesessiontoken");

        try {
            httppost.setHeader("Accept", "*/*");
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("Accept-Encoding", "gzip,deflate,sdch");
            httppost.setHeader("Accept-Language", "en-US,en;q=0.8");

            httpclient.getCredentialsProvider().setCredentials(
                    new AuthScope(iamLoginServer, 443),
                    new UsernamePasswordCredentials(username, password));
            retval = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            Log.d(Config.LOGTAG, "Failed to get IAM token" + e.getMessage());
        } catch (IOException e) {
            Log.d(Config.LOGTAG, "Failed to get IAM token" + e.getMessage());
        }
        return retval;
    }

   // curl -vv 'https://login-stage.adpedge.com/oauth/getorcreatesessiontoken'
   // -H 'Accept-Encoding: gzip,deflate,sdch'
   // -H 'Accept-Language: en-US,en;q=0.8'
   // -H 'Content-Type: application/json'
   // -H 'Accept: */*'
   // --compressed
   // --user 'schonsts:yetiPASS$' --data-binary '{}'

    public String getComminicationEdgeId() {

        HttpResponse response;
        String ceId = "";

        try {

            HttpGet httpGet = new HttpGet("https://" + iamApiServer + "/identityservice/1.1.1/rest/user");
            response = httpclient.execute(httpGet);

            InputStreamReader inStream = new InputStreamReader(response.getEntity().getContent());
            BufferedReader readBuffer = new BufferedReader(inStream);
            StringBuilder responseString = new StringBuilder();

            String line = null;
            try {
                while ((line = readBuffer.readLine()) != null) {
                    responseString.append(line + "\n");
                }
            } catch (IOException e) {
                Log.d(Config.LOGTAG, "Failed to user communication edge id" + e.getMessage());
                throw (e);
            } finally {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.d(Config.LOGTAG, "Failed to user communication edge id" + e.getMessage());
                }
            }

            JSONObject user = new JSONObject(responseString.toString());
            ceId = user.getString("communicationEdgeId");

        } catch(JSONException e) {
            Log.d(Config.LOGTAG, "Failed to parse user JSON for communication edge id" + e.getMessage());
        } catch (ClientProtocolException e) {
            Log.d(Config.LOGTAG, "Failed to user communication edge id" + e.getMessage());
        } catch (IOException e) {
            Log.d(Config.LOGTAG, "Failed to user communication edge id" + e.getMessage());
        }
        return ceId;
    }
}
