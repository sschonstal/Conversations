package eu.siacs.conversations.tests;

import android.test.InstrumentationTestCase;
import eu.siacs.conversations.cdk.iam.Iam;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.siacs.conversations.Config;


public class iamSpec extends InstrumentationTestCase {

    public void testLogin() {
        Iam iam = new Iam();
        String token = iam.login("schonstals", "yetiPASS$");
        String jid = iam.getComminicationEdgeId();
        assertEquals((token.length() > 0), true);
        assertEquals((jid.length() > 0), true);
    }
    public void test() throws Exception {

            String token = "";
//            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://login-dit.adpedge.com/siteminderagent/forms/login.fcc");

            try {

                httppost.setHeader("Connection", "keep-alive");
                httppost.setHeader("Origin", "https://login-dit.adpedge.com");
                httppost.setHeader("Cookie", "loginId=schonstals" );
                httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httppost.setHeader("Referer", "https://login-dit.adpedge.com/sso/common-login");
                httppost.setHeader("Cache-Control", "max-age=0");
                httppost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                httppost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");
                httppost.setHeader("Accept-Encoding", "gzip, deflate");
                httppost.setHeader("Accept-Language", "en-US,en;q=0.8");


                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("username", "schonstals"));
                nameValuePairs.add(new BasicNameValuePair("password", "yetiPASS$"));
                nameValuePairs.add(new BasicNameValuePair("SMENC", "ISO-8859-1"));
                nameValuePairs.add(new BasicNameValuePair("SMLOCALE", "US-EN"));
                nameValuePairs.add(new BasicNameValuePair("USER", "schonstals"));
                nameValuePairs.add(new BasicNameValuePair("target", "-SM-https://login--dit.adpedge.com/sso/route/redirector.php"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                HttpGet httpGet = new HttpGet("https://api-dit.adpedge.com/identityservice/1.1.1/rest/user");
                response = httpclient.execute(httpGet);

                InputStreamReader inStream =  new InputStreamReader(response.getEntity().getContent());

                BufferedReader readBuffer = new BufferedReader(inStream);

                StringBuilder responseString = new StringBuilder();

                String line = null;
                try {
                    while ((line = readBuffer.readLine()) != null) {
                        responseString.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                JSONObject user = new JSONObject(responseString.toString());

                String jid = user.getString("communicationEdgeId");

                Header cookies[] = response.getHeaders("set-cookie");
                for (Header header : cookies) {
                    Log.d(Config.LOGTAG, "Key : " + header.getName()
                            + " ,Value : " + header.getValue());

                    String[] rawCookieParams = header.getValue().split(";");
                    if (rawCookieParams.length > 0) {
                        String[] rawCookieNameAndValue = rawCookieParams[0].split("=");

                        if (rawCookieNameAndValue.length > 1 && rawCookieNameAndValue[0].equals("SMOFC")) {
                            token = rawCookieNameAndValue[1];
                        }
                    }
                }

                Log.d(Config.LOGTAG, "SMOFC token = " + token);



            } catch (ClientProtocolException e) {
                Log.d(Config.LOGTAG, ": iam fail " + e.getMessage());
            } catch (IOException e) {
                Log.d(Config.LOGTAG, ": iam fail " + e.getMessage());
            }

            return;
        }
    }