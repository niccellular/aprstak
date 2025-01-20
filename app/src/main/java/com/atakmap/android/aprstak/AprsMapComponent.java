package com.atakmap.android.aprstak;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import utils.AprsUtility;

import com.atakmap.android.aprstak.receivers.APRSdroidEventReceiver;

import com.atakmap.android.aprstak.receivers.ReadLogReceiver;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.aprstak.receivers.AprsDropDownReceiver;
import com.atakmap.android.aprstak.plugin.R;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;

public class AprsMapComponent extends DropDownMapComponent implements AprsUtility.CotEventListener, CotServiceRemote.CotEventListener, MapEventDispatcher.MapEventDispatchListener, MapEventDispatcher.OnMapEventListener {

    public static final String TAG = "APRSTAKMain";

    public Context pluginContext;

    private AprsDropDownReceiver ddr;
    private MapView mapView;
    private static APRSCotArray aprsCotArray;
    private AprsUtility aprsUtility;
    private APRSMessageHandler aprsMessageHandler;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        this.mapView = view;
        view.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED,this); //added, persist

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new AprsDropDownReceiver(
                view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(AprsDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        CommsMapComponent.getInstance().addOnCotEventListener(this);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.android.aprstak.receivers.cotMenu",
                "this intent launches the cot send utility",
                new DocumentedExtra[] {
                        new DocumentedExtra("targetUID",
                                "the map item identifier used to populate the drop down")
                });

        aprsUtility = AprsUtility.getInstance(view, context);
        registerDropDownReceiver(aprsUtility, filter);

        Log.d(TAG, "Registering aprs receiver with intent filter");
        APRSdroidEventReceiver aprsDroidReceiver = new APRSdroidEventReceiver(view, context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("org.aprsdroid.app.SERVICE_STARTED");
        intentFilter.addAction("org.aprsdroid.app.SERVICE_STOPPED");
        //intentFilter.addAction("org.aprsdroid.app.MESSAGE");
        //intentFilter.addAction("org.aprsdroid.app.MESSAGETX");
        intentFilter.addAction("org.aprsdroid.app.POSITION");
        intentFilter.addAction("org.aprsdroid.app.UPDATE");
        pluginContext.registerReceiver(aprsDroidReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);

        ReadLogReceiver readMeReceiver = new ReadLogReceiver(view, context);
        registerReceiverUsingPluginContext(pluginContext, "aprs log receiver", readMeReceiver, ReadLogReceiver.SHOW_LOG);


        this.aprsCotArray = new APRSCotArray();

        this.aprsMessageHandler = new APRSMessageHandler(context);
    }

    public static APRSCotArray getAprsCotArray() {
        return aprsCotArray;
    }

    private void registerReceiverUsingPluginContext(Context pluginContext, String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        this.registerReceiver(pluginContext, rec, mainIntentFilter);
    }

    private void registerReceiverUsingAtakContext(String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        AtakBroadcast.getInstance().registerReceiver(rec, mainIntentFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }

    @Override
    public void onReceiveCotEvent(CotEvent cotEvent) {
        android.util.Log.d(TAG, "onReceiveCotEvent");
    }

    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        android.util.Log.d(TAG, "onCotEvent: " + cotEvent.toString());
    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        android.util.Log.d(TAG, "onReceiveMapEvent: " + mapEvent.getType() + "," + mapEvent.getItem() + "," + mapEvent.toString());

        if (!AprsMapComponent.getAprsCotArray().hasMapEvent(mapEvent.getItem().getUID())) {
            AprsMapComponent.getAprsCotArray().addMapEventToAPRSCotArray(mapEvent);

            this.mapView.getMapEventDispatcher().addMapItemEventListener(mapEvent.getItem(), this);
        }
    }

    @Override
    public void onMapItemMapEvent(MapItem mapItem, MapEvent mapEvent) {
        android.util.Log.d(TAG, "onMapItemMapEvent: " + mapEvent.getType() + "," + mapEvent.toString() + "," + mapItem.toString());

        CotEvent cotEvent = CotEventFactory.createCotEvent(mapItem);
        if(cotEvent != null) {
            if (!AprsMapComponent.getAprsCotArray().hasCotEvent(cotEvent.getUID())) {
                AprsMapComponent.getAprsCotArray().addCotEventToAPRSCotArray(cotEvent);
                Log.d(TAG, "CotEvent from MapEvent: " + cotEvent.toString());
            }
        } else {
            Log.e(TAG, "CotEvent was null");
        }
    }
}
