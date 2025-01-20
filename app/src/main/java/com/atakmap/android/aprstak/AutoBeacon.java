package com.atakmap.android.aprstak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;


public class AutoBeacon implements Runnable {
    private Thread thread;
    private boolean running;

    private Context context;

    final static String TAG = "AutoBeacon";

    public AutoBeacon(Context context){
        this.running = true;
        this.context = context;
        this.thread = new Thread(this);
    }
    @Override
    public void run() {
        Looper.prepare();

        SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
        int delay = sharedPref.getInt("autoBeaconInterval", 1);
        try {
            while(running) {
                Log.i(TAG, "APRS-TAK sending one POSITION packet");
                Intent i = new Intent("org.aprsdroid.app.ONCE").setPackage("org.aprsdroid.app");
                context.getApplicationContext().startForegroundService(i);
                Thread.sleep(delay*60000); // convert minutes
            }
        } catch(Exception e) {
            Log.i(TAG, "AutoBeacon thread error: " + e);
        }
    }
    public void start()  {
        thread.start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

}
