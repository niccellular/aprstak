package com.atakmap.android.aprstak;

import android.util.Log;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.HashMap;

public class APRSCotArray {

    private static final String TAG = "APRSCotArray";
    private static HashMap<String, CotEvent> cotEventsList;
    private static HashMap<String, MapEvent> mapEventsList;

    public APRSCotArray() {
        cotEventsList = new HashMap<>();
        mapEventsList = new HashMap<>();
    }

    public CotEvent getCotEvent(String uid) {
        if (hasCotEvent(uid))
            return cotEventsList.get(uid);
        return null;
    }

    public boolean hasCotEvent(String uid) {
        if (cotEventsList.containsKey(uid))
            return true;
        return false;
    }

    public void addCotEventToAPRSCotArray(CotEvent e) {
        String uid = e.getUID();
        Log.d(TAG, "Adding CotEvent uid: " + uid);
        cotEventsList.put(uid, e);
    }

    public void removeCotEventToAPRSCotArray(String uid) {
        Log.d(TAG, "Removing CotEvent uid: " + uid);
        cotEventsList.remove(uid);
    }

    public MapEvent getMapEvent(String uid) {
        if (hasMapEvent(uid))
            return mapEventsList.get(uid);
        return null;
    }

    public boolean hasMapEvent(String uid) {
        if (mapEventsList.containsKey(uid))
            return true;
        return false;
    }

    public void addMapEventToAPRSCotArray(MapEvent e) {
        String uid = e.getItem().getUID();
        Log.d(TAG, "Adding MapEvent uid: " + uid);
        mapEventsList.put(uid, e);
    }

    public void removeMapEventToAPRSCotArray(String uid) {
        Log.d(TAG, "Removing CotEvent uid: " + uid);
        mapEventsList.remove(uid);
    }
}
