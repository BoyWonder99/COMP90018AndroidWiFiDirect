package io.cluo29.github.wifidirecttest1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "haha";

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter filter;
    private WifiManager wifi;

    OutputStream ServerOut =null;
    OutputStream ClientOut = null;

    volatile boolean isThreadRuning = false;

    private List peers = new ArrayList();

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
                    // Check to see if Wi-Fi is enabled and notify appropriate activity
                    Log.d(TAG, "Line 86 WIFI_P2P_STATE_CHANGED_ACTION");

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
                    Log.d(TAG, "Line 98 WIFI_P2P_PEERS_CHANGED_ACTION");

                    if (mManager != null) {
                        mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                            @Override
                            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                                peers.clear();
                                peers.addAll(wifiP2pDeviceList.getDeviceList());

                                for (int i =0 ; i<peers.size();i++){
                                    WifiP2pDevice selectDevice = (WifiP2pDevice) peers.get(i);

                                    Log.d(TAG, "peer detected: "+selectDevice.deviceAddress);

                                    //connect a peer
                                    //connect(selectDevice);
                                }
                                if (peers.size() == 0) {
                                    Log.d(TAG, "no peers detected");
                                }


                            }
                        });
                    }

                    break;
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                    // Respond to new connection or disconnections
                    Log.d(TAG, "Line 127 WIFI_P2P_CONNECTION_CHANGED_ACTION");

                    if (mManager == null) {
                        return;
                    }

                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                                    @Override
                                    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                                        Log.d(TAG, "onConnectionInfoAvailable");
                                        final String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
                                        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                                            Log.d(TAG, "groupFormed, I am group owner");

                                            // do work using group owner logic
                                            // create a server thread if the current device is a group owner
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        isThreadRuning = true;
                                                        ServerSocket serverSocket = new ServerSocket(8888);
                                                        String line = "";
                                                        while (isThreadRuning){
                                                            Log.d(TAG, " wait for connection");
                                                            Socket client = serverSocket.accept();
                                                            Log.d(TAG, "@ server connection successfully");
                                                            InputStream inputStream = client.getInputStream();
                                                            OutputStream outputStream = client.getOutputStream();
                                                            ServerOut = outputStream;
                                                            serverSendData("Server says hello");
                                                            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                                                            while ((line = br.readLine()) != null) {
                                                                Log.d(TAG, "@ " +line);
                                                                // for thread safety in demo, print first line and quit
                                                                isThreadRuning = false;
                                                            }
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                        Log.d(TAG, "@ connection fail "+e.getMessage());
                                                    }
                                                }
                                            }).start();
                                        }
                                        else if (wifiP2pInfo.groupFormed){
                                            Log.d(TAG, "groupFormed, I am client");

                                            // do work using client logic
                                            // create a client thread if the current device is not a group owner
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    isThreadRuning = true;
                                                    String line = "";
                                                    Socket socket = new Socket();
                                                    try {
                                                        socket.bind(null);
                                                        socket.connect(new InetSocketAddress(groupOwnerAddress,8888),5000);
                                                        Log.d(TAG, "@ start connecting to sever "+groupOwnerAddress);
                                                        Log.d(TAG, "@ client connection successfully");
                                                        InputStream inputStream = socket.getInputStream();
                                                        OutputStream outputStream = socket.getOutputStream();
                                                        ClientOut = outputStream;
                                                        clientSendData("Client says hello");
                                                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                                                        while ((line = br.readLine()) != null){
                                                            Log.d(TAG, "@ " +line);
                                                        }

                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                        Log.d(TAG, "@ connection fail "+e.getMessage());
                                                    }
                                                    isThreadRuning = false;
                                                }
                                            }).start();
                                        }

                                    }
                                }
                        );
                    }


                    break;
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                    // Respond to this device's wifi state changing
                    Log.d(TAG, "Line 135 WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

                    break;
            }
        }
    };

    public void connect(WifiP2pDevice selectDevice){

        //obtain a peer from the WifiP2pDeviceList

        if (selectDevice.status == WifiP2pDevice.AVAILABLE) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = selectDevice.deviceAddress;
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                    Log.d(TAG, "connect success");
                }

                @Override
                public void onFailure(int reason) {

                    Log.d(TAG, "connect failure");
                }
            });
        }


    }

    public void serverSendData(String data) {
        StringBuffer sb = new StringBuffer();
        sb.append(data);
        sb.append("\n");
        if (ServerOut != null) {
            try {
                ServerOut.write(sb.toString().getBytes());
                ServerOut.flush();
                Log.d(TAG,"@ server sent data ");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG,"@ Server sending fail");
            }
        }
    }

    public void clientSendData(String data) {
        StringBuffer sb = new StringBuffer();
        sb.append(data);
        sb.append("\n");
        if (ClientOut != null) {
            try {
                ClientOut.write(sb.toString().getBytes());
                ClientOut.flush();
                Log.d(TAG,"@ client sent data ");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG,"@ client sending fail");
            }
        }
    }


    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, filter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }


// end of activity class def
}
