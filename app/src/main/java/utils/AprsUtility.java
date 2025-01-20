package utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;

import com.atakmap.android.aprstak.APRSCotArray;
import com.atakmap.android.aprstak.AprsMapComponent;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;
import com.atakmap.android.aprstak.AutoBeacon;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.helpers.DefaultEXIFactory;
import com.siemens.ct.exi.main.api.sax.EXIResult;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class AprsUtility extends DropDownReceiver implements DropDown.OnStateListener, MapEventDispatcher.MapEventDispatchListener {
    public static final String TAG = AprsUtility.class
            .getSimpleName();

    private static AprsUtility instance = null;
    private MapView mapView;
    private Context context;
    private boolean isReceiving = false;

    public static AutoBeacon ab = null;
    private boolean isAutoBeaconing = false;

    // TNC
    public static boolean useTNC = false;
    public static boolean aprsdroid_running = false;

    // Encryption
    public static boolean usePSK = false;

    public interface CotEventListener{
            void onReceiveCotEvent(CotEvent cotEvent);
    }

    public static AprsUtility getInstance(MapView mapView, Context context){
        if(instance == null){
            instance = new AprsUtility(mapView, context);
        }
        return instance;
    }

    private AprsUtility(MapView mapView, Context context) {
        super(mapView);
        this.mapView = mapView;
        this.context = context;


        Collection<MapItem> mapItems = getMapItemsInGroup(getMapView().getRootGroup(), new HashSet<MapItem>());
        for(MapItem mapItem : mapItems){
            mapItem.setMetaString("menu", getMenu());
        }

        getMapView().getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED, this);
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {

    }

    @Override
    public void onDropDownVisible(boolean b) {

    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            final String action = intent.getAction();
            if (action != null && action
                    .equals("com.atakmap.android.aprstak.receivers.cotMenu")) {

                String targetUID = intent.getStringExtra("targetUID");
                CotEvent cotEvent = null;
                MapItem mapItem = mapView.getMapItem(targetUID);
                String itemUid = mapItem.getUID();

                HashMap<String, String> groupItemUid = new HashMap<>();
                HashMap<String, String> parentGroupItemUid = new HashMap<>();
                HashMap<String, String> childGroupItemUid = new HashMap<>();

                Log.d(TAG, String.format("Clicked targetUID:%s, MapItem UID:%s", targetUID, itemUid));
                Log.d(TAG, "Group name: " + mapItem.getGroup().getFriendlyName());
                Log.d(TAG, String.format("Group count: %d, Item Count: %d ", mapItem.getGroup().getGroupCount(), mapItem.getGroup().getItemCount()));

                for (MapItem i : mapItem.getGroup().getItems()) {
                    Log.d(TAG, "Group item: " + i.toString());
                    if (i.getUID().isEmpty())
                        continue;
                    if (AprsMapComponent.getAprsCotArray().hasCotEvent(i.getUID())) {
                        groupItemUid.put(i.getUID(), "GroupItem");
                        Log.d(TAG, "Found matching CotEvent UID: " + i.getUID());
                    }
                }

                if (mapItem.getGroup().getParentGroup() != null) {
                    Log.d(TAG, "Parent Group name: " + mapItem.getGroup().getParentGroup().getFriendlyName());
                    Log.d(TAG, String.format("PARENT Group count: %d, Item Count: %d ", mapItem.getGroup().getParentGroup().getGroupCount(), mapItem.getGroup().getParentGroup().getItemCount()));
                    for (MapItem i : mapItem.getGroup().getParentGroup().getItems()) {
                        Log.d(TAG, "Parent Group item: " + i.toString());
                        if (i.getUID().isEmpty())
                            continue;
                        if (AprsMapComponent.getAprsCotArray().hasCotEvent(i.getUID())) {
                            parentGroupItemUid.put(i.getUID(), "ParentGroupItem");
                            Log.d(TAG, "Found matching CotEvent UID: " + i.getUID());
                        }
                    }
                }
                Log.d(TAG, "After Parent Group");
                for (MapGroup g : mapItem.getGroup().getChildGroups()) {
                    for (MapItem i : g.getItems()) {
                        if (i.getUID().isEmpty())
                            continue;
                        Log.d(TAG, "Child Group " + g.getFriendlyName() + " item: " + i.toString());
                        if (AprsMapComponent.getAprsCotArray().hasCotEvent(i.getUID())) {
                            childGroupItemUid.put(i.getUID(), "ChildGroupItem");
                            Log.d(TAG, "Found matching CotEvent UID: " + i.getUID());
                        }
                    }
                }
                Log.d(TAG, "After Child Group");
                HashMap<String, CotEvent> sendList = new HashMap<>();

                if (AprsMapComponent.getAprsCotArray().hasCotEvent(mapItem.getUID())) {
                    cotEvent = AprsMapComponent.getAprsCotArray().getCotEvent(mapItem.getUID());
                    sendList.put(mapItem.getUID(), cotEvent);
                }
                if (AprsMapComponent.getAprsCotArray().hasCotEvent(mapItem.getGroup().getFriendlyName())) { // circle
                    cotEvent = AprsMapComponent.getAprsCotArray().getCotEvent(mapItem.getGroup().getFriendlyName());
                    sendList.put(mapItem.getGroup().getFriendlyName(), cotEvent);
                }
                for (String groupUid : groupItemUid.keySet()) {
                    if (AprsMapComponent.getAprsCotArray().hasCotEvent(groupUid)) { //???
                        cotEvent = AprsMapComponent.getAprsCotArray().getCotEvent(groupUid);
                        sendList.put(groupUid, cotEvent);
                    }
                }
                for (String parentGroupUid : parentGroupItemUid.keySet()) {
                    if (AprsMapComponent.getAprsCotArray().hasCotEvent(parentGroupUid)) { // rectangles
                        cotEvent = AprsMapComponent.getAprsCotArray().getCotEvent(parentGroupUid);
                        sendList.put(parentGroupUid, cotEvent);
                    }
                }
                for (String childGroupUid : childGroupItemUid.keySet()) {
                    if (AprsMapComponent.getAprsCotArray().hasCotEvent(childGroupUid)) { // freehand line
                        cotEvent = AprsMapComponent.getAprsCotArray().getCotEvent(childGroupUid);
                        sendList.put(childGroupUid, cotEvent);
                    }
                }
                Log.d(TAG, "After sendList");
                Set<Map.Entry<String, CotEvent>> set = sendList.entrySet();
                for (Map.Entry<String, CotEvent> ce : set) {
                    byte[] cotAsBytes;

                    try {
                        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
                        ByteArrayOutputStream osEXI = new ByteArrayOutputStream();
                        EXIResult exiResult = new EXIResult(exiFactory);
                        exiResult.setOutputStream(osEXI);
                        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
                        SAXParser newSAXParser = saxParserFactory.newSAXParser();
                        XMLReader xmlReader = newSAXParser.getXMLReader();
                        xmlReader.setContentHandler(exiResult.getHandler());
                        InputSource stream = new InputSource(new StringReader(cotEvent.toString()));
                        xmlReader.parse(stream); // parse XML input
                        cotAsBytes = osEXI.toByteArray();
                        osEXI.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    Log.d(TAG, "Size: " + cotAsBytes.length);
                    String encodedString;
                    if (cotAsBytes.length > 0) {
                        if (usePSK) {
                            Log.i(TAG, "PSK enabled");
                            ByteBuffer payload;
                            byte[] PSKhash, cipherText;
                            SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                            String psk = sharedPref.getString("PSKText", "atakatak");
                            try {
                                MessageDigest digest = MessageDigest.getInstance("MD5");
                                PSKhash = digest.digest(psk.getBytes("UTF-8"));
                            } catch (Exception e) {
                                Log.d(TAG, "Encrypt PSK Hashing problem: " + e);
                                return;
                            }
                            try {
                                byte[] iv = new byte[16];
                                new SecureRandom().nextBytes(iv);
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec key = new SecretKeySpec(PSKhash, "AES");
                                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                                cipherText = cipher.doFinal(cotAsBytes);
                                // set the iv+cipherText as the payload
                                payload = ByteBuffer.allocate(iv.length + cipherText.length);
                                payload.put(iv);
                                payload.put(cipherText);
                            } catch (Exception e) {
                                Log.d(TAG, "Encrypt PSK problem: " + e);
                                return;
                            }
                            encodedString = Base64.encodeToString(payload.array(), Base64.NO_WRAP);
                        } else {
                            encodedString = Base64.encodeToString(cotAsBytes, Base64.NO_WRAP);
                        }

                        Log.d(TAG, "calling SEND_PACKET: " + encodedString);
                        Intent i = new Intent("org.aprsdroid.app.SEND_PACKET").setPackage("org.aprsdroid.app");
                        i.putExtra("data", String.format(">M,%s", encodedString));
                        context.getApplicationContext().startForegroundService(i);
                    }
                }
                sendList.clear();
            }
        } catch(Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void startABListener() {
        Log.d(TAG, "startAutoBeacon"); 
        isAutoBeaconing = true;

        ab = new AutoBeacon(context);
        ab.start();
    }

    public void stopABListener() {
        Log.d(TAG, "stopAutoBeacon");
        isAutoBeaconing = false;

        if(ab == null) {
            return;
        }

        ab.stop();
    }

    public boolean isAutoBeaconing(){
        return isAutoBeaconing;
    }


    private String getMenu() {
        return PluginMenuParser.getMenu(context, "menu.xml");
    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        Log.d(TAG, "onMapEvent showing aprs menu");
        mapEvent.getItem().setMetaString("menu", getMenu());
    }

    public static Collection<MapItem> getMapItemsInGroup(MapGroup mapGroup, Collection<MapItem> mapItems){
        if(mapGroup == null){
            return null;
        }

        Collection<MapGroup> childGroups = mapGroup.getChildGroups();

        if(childGroups.size() == 0){
            return mapGroup.getItems();
        }else {
            for (MapGroup childGroup : childGroups) {
                mapItems.addAll(getMapItemsInGroup(childGroup, mapItems));
            }
        }

        return mapItems;
    }

    public static LinkedHashSet<MapItem> getCursorOnTargetMapItems(MapView mapView){
        MapGroup cotMapGroup = mapView.getRootGroup().findMapGroup("Cursor on Target");

        LinkedHashSet<MapItem> cotMapItems = new LinkedHashSet<>(AprsUtility.getMapItemsInGroup(cotMapGroup, new ArrayList<MapItem>()));

        /**
         Get all CoT markers stored in memory (from User Objects).. I don't know why this happens, but
         these objects don't get saved to "Cursor on Target" group until after closing ATAK. Sigh...
         */
        if(mapView != null && mapView.getRootGroup() != null) {
            MapGroup userObjects = mapView.getRootGroup().findMapGroup("User Objects");

            Collection<MapItem> subItems;
            subItems = AprsUtility.getMapItemsInGroup(userObjects.findMapGroup("Hostile"), new ArrayList<MapItem>());
            if(subItems != null) {
                cotMapItems.addAll(subItems);
            }
            subItems = AprsUtility.getMapItemsInGroup(userObjects.findMapGroup("Friendly"), new ArrayList<MapItem>());
            if(subItems != null)
                cotMapItems.addAll(subItems);
            subItems = AprsUtility.getMapItemsInGroup(userObjects.findMapGroup("Neutral"), new ArrayList<MapItem>());
            if(subItems != null)
                cotMapItems.addAll(subItems);
            subItems = AprsUtility.getMapItemsInGroup(userObjects.findMapGroup("Unknown"), new ArrayList<MapItem>());
            if(subItems != null)
                cotMapItems.addAll(subItems);
        }

        return cotMapItems;
    }
}
