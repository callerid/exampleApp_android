package com.callerid.sampleapp;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class UDPListen extends Service {

    // Setup required variables
    private final IBinder mBinder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;
    String recString = "";
    DatagramSocket socket = null;
    PowerManager pm = null;
    PowerManager.WakeLock pLock = null;
    WifiManager wm = null;
    WifiManager.WifiLock wLock = null;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        UDPListen getService() {
            // Return this instance of LocalService so clients can call public methods
            return UDPListen.this;
        }
    }

    @Override
    public void onCreate(){

        // ------------------- NOT WORKING AT THIS TIME --------------------------
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CIDLock");
        pLock.acquire();


        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL,"CIDWLock");
        wLock.acquire();

        //------------------------------------------------------------------------

        // Listening thread start
        if(!idle.isAlive()) {
            idle.start();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Set callbacks for interface
    public void setCallbacks(ServiceCallbacks callbacks){
        serviceCallbacks = callbacks;
    }


    // ** Main thread that listens for UDP traffic from CallerID.com unit.
    Thread idle = new Thread(new Runnable() {
        public void run()
        {

            // ** This is where CallerID.com Hardware sends
            //    the CallerID records via Ethernet.
            try{

                socket = new DatagramSocket(null);
                SocketAddress address = new InetSocketAddress("255.255.255.255",3520);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(address);

                byte[] buffer = new byte[65507];
                Boolean looping = true;
                while (looping) {
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    try {

                        socket.receive(dp);
                        recString = new String(dp.getData(), 0, dp.getLength());

                        // Send UDP packet information to MainActivity through interface
                        serviceCallbacks.display(recString);

                    } catch (Exception ex) {
                        System.out.println("Exception: " + ex.toString());
                        looping = false;
                    }
                }

                System.out.println("Thread ended.");

            }catch(Exception exMain){
                System.out.print("Exception: " + exMain.toString());
            }

        }
    });

}
