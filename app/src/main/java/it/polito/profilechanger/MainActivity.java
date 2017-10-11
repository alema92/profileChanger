package it.polito.profilechanger;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPref;
    WifiManager wifiManager;
    ListView list;
    TextView noNet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialization
        setContentView(R.layout.activity_main);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        list = (ListView) findViewById(R.id.listView1);
        noNet = (TextView) findViewById(R.id.noNet);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        // scanning available wifis
        wifiManager.startScan();

        // listening to single list item on click
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get selected item name
                String ssid = ((TextView) view).getText().toString();
                showRadioButtonDialog(ssid);
            }
        });
    }

    private void showRadioButtonDialog(final String ssid) {
        // custom dialog initialization
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(String.format(getString(R.string.dialog_title), ssid));
        dialog.setContentView(R.layout.dialog);
        dialog.setCancelable(true);
        dialog.show();

        //initialization
        RadioButton rd1 = (RadioButton) dialog.findViewById(R.id.r1);
        RadioButton rd2 = (RadioButton) dialog.findViewById(R.id.r2);
        RadioButton rd3 = (RadioButton) dialog.findViewById(R.id.r3);

        // if the profile has already been set, check corresponding radio button
        int profile = sharedPref.getInt(ssid, -1);
        switch (profile) {
            case 0:
                rd1.setChecked(true);
                break;
            case 1:
                rd2.setChecked(true);
                break;
            case 2:
                rd3.setChecked(true);
                break;
            default:
                break;
        }

        rd1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRadioB(ssid, 0, v.getContext(), dialog);
            }
        });
        rd2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRadioB(ssid, 1, v.getContext(), dialog);
            }
        });
        rd3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRadioB(ssid, 2, v.getContext(), dialog);
            }
        });
    }

    public void onClickRadioB(String ssid, int profile, Context c, Dialog dialog) {
        SharedPreferences.Editor editor = sharedPref.edit();
        // add corresponding profile value to shared preferences when radio button is clicked
        editor.putInt(ssid, profile);
        editor.apply();
        // set profile and close dialog
        changeProfile(c);
        dialog.dismiss();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        // register broadcast receiver when connecting to a network and when refreshing available wifis
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(receiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        super.onResume();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        List<ScanResult> wifiScanList;
        ArrayList<String> wifis = new ArrayList<>();

        @Override
        public void onReceive(Context c, Intent intent) {
            if (wifiManager.isWifiEnabled()) {
                wifiScanList = wifiManager.getScanResults();
                for (int i = 0; i < wifiScanList.size(); i++) {
                    // create a list of string with all wifi names
                    String ssid = (wifiScanList.get(i)).SSID;
                    if (!wifis.contains(ssid)) {
                        wifis.add(ssid);
                    }
                }
                // set the adapter with the list
                list.setAdapter(new ArrayAdapter<>(getApplicationContext(), R.layout.list_item, wifis));

                // if there are no wifis, show a message
                if (wifis.isEmpty()) {
                    noNet.setVisibility(View.VISIBLE);
                } else {
                    noNet.setVisibility(View.GONE);
                }
                // if is connected change profile
                changeProfile(c);
            }
        }
    };

    // change when select profile in dialog and when connecting
    public void changeProfile(Context c) {
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
                    AudioManager audioManager = (AudioManager) c.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
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
