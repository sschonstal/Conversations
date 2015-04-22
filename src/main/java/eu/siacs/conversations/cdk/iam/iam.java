package eu.siacs.conversations.cdk.iam;

import android.os.AsyncTask;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import java.util.ArrayList;
import java.util.List;

import eu.siacs.conversations.Config;


public class Iam {

    private static Iam instance = null;
    private static HttpClient httpclient;
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
        HttpResponse response = postLogin(username, password);
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

    private HttpResponse postLogin(String username, String password)  {

        HttpResponse retval = null;
        HttpPost httppost = new HttpPost("https://login-stage.adpedge.com/siteminderagent/forms/login.fcc");

        try {
            httppost.setHeader("Connection", "keep-alive");
            httppost.setHeader("Origin", "https://login-stage.adpedge.com");
            httppost.setHeader("Cookie", "loginId=" + username );
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httppost.setHeader("Referer", "https://login-stage.adpedge.com/sso/common-login");
            httppost.setHeader("Cache-Control", "max-age=0");
            httppost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            httppost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");
            httppost.setHeader("Accept-Encoding", "gzip, deflate");
            httppost.setHeader("Accept-Language", "en-US,en;q=0.8");


            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            nameValuePairs.add(new BasicNameValuePair("SMENC", "ISO-8859-1"));
            nameValuePairs.add(new BasicNameValuePair("SMLOCALE", "US-EN"));
            nameValuePairs.add(new BasicNameValuePair("USER", username));
            nameValuePairs.add(new BasicNameValuePair("target", "-SM-https://login--stage.adpedge.com/sso/route/redirector.php"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            retval =  httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            Log.d(Config.LOGTAG, "Failed to get IAM token" + e.getMessage());
        } catch (IOException e) {
            Log.d(Config.LOGTAG, "Failed to get IAM token" + e.getMessage());
        }
        return retval;
    }

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
