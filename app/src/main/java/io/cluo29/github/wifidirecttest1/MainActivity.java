package io.cluo29.github.wifidirecttest1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "haha";

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter filter;
    private WifiManager wifi;

    WifiP2pDevice device;
    WifiP2pConfig config = new WifiP2pConfig();

    OutputStream ServerOut =null;
    InputStream ServerIn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // scan devices
        // scan implies being visible to other devices
        startScanPeers();

        // create an instance of WiFip2pManager
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        // return a channel object used to connect your app to wifi p2p framework
        mChannel = mManager.initialize(this, getMainLooper(), null);

        wifi =  (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        filter = new IntentFilter();
        //Broadcast when Wi-Fi P2P is enabled or disabled on the device.
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //Broadcast when you call discoverPeers(). You usually want to call requestPeers() to get an updated list of peers if you handle this intent in your application.
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        //Broadcast when the state of the device's Wi-Fi connection changes.
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //Broadcast when a device's details have changed, such as the device's name
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver,filter);
    }

    public void startScanPeers(){
        //start to scan whether there are any peer devices
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "discovery successful");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "discovery failed");
            }
        });
    }

    private final BroadcastReceiver  mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        // Wifi P2P is enabled
                        Log.d(TAG, "WiFi Direct is enabled");
                    } else {
                        Log.d(TAG, "WiFi Direct is disenabled");
                    }
                    break;
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                    // Call WifiP2pManager.requestPeers() to get a list of current peers

                    break;
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                    // Respond to new connection or disconnections
                    break;
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                    // Respond to this device's wifi state changing
                    break;
            }
        }
    };


// end of activity class def
}
