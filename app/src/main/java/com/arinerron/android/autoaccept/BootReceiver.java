package com.arinerron.android.autoaccept;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by aaron on 9/28/16.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) { // on phone boot, start instance.
        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);

        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (isTTSDWifi(context))
                    connect(context);
            }
        }, 0, 2, TimeUnit.MINUTES);
    }

    public static void connect(Context context) { // send accept request
        HashMap<String, String> map = new HashMap<>();
        map.put("buttonClicked", "4");
        map.put("err_flag", "0");
        map.put("err_msg", "");
        map.put("info_flag", "0");
        map.put("info_msg", "");
        map.put("redirect_url", "");
        map.put("network_name", "Guest Network");

        Log.i("RequestStatus", performPostCall("192.168.43.0/login.html", map));
    }

    public static String performPostCall(String requestURL, HashMap<String, String> postDataParams) { // HTTP POST to connect
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:42.0) Gecko/20100101 Firefox/42.0");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Cookie", "NID=87=u6_B8b5yUXPcKgVovW3dPjJx3LWRzSZjWmncdhZ7yz_SRK30iLEzDXnKpNzSpimi-Aopo81te_HUtcDMgxDWoHxYtqE-5eou3eOSBpvWWE8MtU8Q9W0egAr6rjAr1t_wwzRX5s1-y6DnMw; OGPC=5061451-25:5061821-5:");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Length", "98");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException { // generate post data as string
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    /*
    public static void registerAlarm(Context context) {
        Intent i = new Intent(context, BootReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, 0, i, 0);

        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 3 * 1000; // start 3 seconds after first register.

        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                300000, sender); // 5min interval

    }
    */

    public static boolean isTTSDWifi(Context context) { // is the wifi the right one?
        return getWifiName(context).contains("TTSD");
    }

    public static String getWifiName(Context context) { // get wifi name to be sure we're connected to TTSD_Guest
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }
}
