package it.polito.profilechanger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    AlertDialog mGPSDialog;
    SharedPreferences sharedPref;
    WifiManager wifiManager;
    ListView list;
    TextView noNet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialization
        setContentView(R.layout.activity_main);
        sharedPref = this.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
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
        AlertDialog dialog;
        CharSequence[] values = {getString(R.string.normal), getString(R.string.vibration), getString(R.string.mute)};
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(String.format(getString(R.string.dialog_title), ssid));
        builder.setCancelable(true);

        // if the profile has already been set, check corresponding radio button
        int profile = sharedPref.getInt(ssid, -1);
        builder.setSingleChoiceItems(values, profile, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // add corresponding profile value to shared preferences when radio button (item) is clicked
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(ssid, item);
                editor.apply();
                // set right profile if already connected to wifi just modified then close dialog
                changeProfile();
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    private void changeProfile() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replace("\"", "");
        if (ssid != null) {
            // if it is already connected to the network just updated,
            // and if the name is present in the shared preferences list,
            // change profile accordingly
            AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
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

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        // for API >= 23, to scan network you need to turn on gps and grant permission for location
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledDialog();
            }
        }

        // register broadcast receiver when refreshing available wifis
        // I need 2 receiver, this is declared here because it is dynamic
        // and when application close it will stop listening for scanning
        // the other one instead is declared statically in the manifest,
        // because I need it even after closing application
        // (if connecting to a wifi for which I have set a profile, it must change profile accordingly)
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        super.onResume();
    }

    public void showGPSDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_gps))
                .setMessage(getString(R.string.text_gps))
                .setPositiveButton(getString(R.string.ok_gps), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
                    }
                })
                .setNegativeButton(getString(R.string.no_gps), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                });
        mGPSDialog = builder.create();
        mGPSDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mGPSDialog.dismiss();
            }
        }
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
                // if there are no wifis, show a message
                if (wifis.isEmpty()) {
                    noNet.setVisibility(View.VISIBLE);
                } else {
                    noNet.setVisibility(View.GONE);
                    // set the adapter with the list
                    list.setAdapter(new ArrayAdapter<>(getApplicationContext(), R.layout.list_item, wifis));
                }
            }
        }
    };

}
