package it.polito.profilechanger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import static android.content.Context.WIFI_SERVICE;


public class WifiService extends BroadcastReceiver {

    WifiManager wifiManager;
    SharedPreferences sharedPref;

    @Override
    public void onReceive(Context c, Intent intent) {
        sharedPref = c.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        wifiManager = (WifiManager) c.getApplicationContext().getSystemService(WIFI_SERVICE);

        // get information about connection (if connected or connecting, proceed)
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
            if (state == NetworkInfo.DetailedState.CONNECTED ||
                    state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                // getSSID return the name with ""
                String ssid = wifiInfo.getSSID().replace("\"", "");
                if (ssid != null) {
                    // when connected, if the name is present in the shared preferences list, change profile accordingly
                    AudioManager audioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                    int profile = sharedPref.getInt(ssid, -1);
                    switch (profile) {
                        case 0:
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            break;
                        case 1:
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                            break;
                        case 2:
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
