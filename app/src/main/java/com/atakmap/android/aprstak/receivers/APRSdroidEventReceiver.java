package com.atakmap.android.aprstak.receivers;

import static com.atakmap.android.maps.MapView.getMapView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Base64;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinkerFactory;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import utils.AprsUtility;

public class APRSdroidEventReceiver extends BroadcastReceiver {

	public static final String TAG = APRSdroidEventReceiver.class.getSimpleName();

	private MapView mapView;
	private final Context pluginContext;
	private CotShrinker cotShrinker;

	public APRSdroidEventReceiver(MapView mapView, Context context) {

		this.mapView = mapView;
		this.pluginContext = context;

		CotShrinkerFactory cotShrinkerFactory = new CotShrinkerFactory();
		CotShrinker cotShrinker = cotShrinkerFactory.createCotShrinker();
		this.cotShrinker = cotShrinker;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent == null) {
			Log.w(TAG, "Doing nothing, because intent was null");
			return;
		}

		if (intent.getAction() == null) {
			Log.w(TAG, "Doing nothing, because intent action was null");
			return;
		}

		String a = intent.getAction().replace("org.aprsdroid.app.", "");

		long time;
		CotEvent event;
		CotDetail detail;
		CotPoint point;
		switch (a) {
			case "SERVICE_STARTED":
				AprsUtility.aprsdroid_running = true;
				Log.i(TAG, "APRSdroid is running");
				break;
			case "SERVICE_STOPPED":
				AprsUtility.aprsdroid_running = false;
				Log.i(TAG, "APRSdroid is not running");
				break;
			case "POSITION":
				String callsign = intent.getStringExtra("callsign");
				String packet = intent.getStringExtra("packet");
				Location location = intent.getParcelableExtra("location");
				Log.i("APRS Position Event", "callsign: " + callsign + " position: " + location + "\nraw packet: " + packet);

				event = new CotEvent();
				event.setUID(callsign);
				event.setType("a-f-G-U-C");
				event.setHow("m-g");

				point = new CotPoint(location.getLatitude(), location.getLongitude(), 0,0,0);
				event.setPoint(point);

				time =  new CoordinatedTime().getMilliseconds();
				event.setTime(new CoordinatedTime(time));
				event.setStart(new CoordinatedTime(time));
				event.setStale(new CoordinatedTime(time+300000000));

				detail = new CotDetail("detail");
				event.setDetail(detail);

				CotDetail remark = new CotDetail("remarks");
				remark.setAttribute("source", "APRS Position");
				remark.setInnerText(packet);

				detail.addChild(remark);

				if(event.isValid()) {
					CotMapComponent.getInternalDispatcher().dispatch(event);
					MapItem mi = getMapView().getRootGroup().deepFindUID(callsign);
					mi.setMetaBoolean("archive", true);
					mi.persist(getMapView().getMapEventDispatcher(), null, this.getClass());
				}

				break;
			case "UPDATE":
				int type = intent.getIntExtra("type",2); // 2: error (something went wrong)
				String status = intent.getStringExtra("status");
				Log.d("APRS Update Event", type + " status: " + status);

				switch (type) {
					case 0: // Outgoing packet sent by APRSDroid
						ReadLogReceiver.getAprslog().append(String.format("%d: packet sent\n-----\n",type));
						break;
					case 1:
					case 2:
						ReadLogReceiver.getAprslog().append(String.format("%d: %s\n-----\n",type, status));
						break;
					case 3: // Incoming packet received from APRSDroid
						CotEvent cotEvent;
						if (status.contains(":>M,")) { // this is the Marker flag we put in the raw APRS packet
							int start = status.indexOf(":");
							String encodedString = status.substring(start+4); // skip past Marker
							Log.d(TAG, "Received Base64 string length: " + encodedString.length());
							byte[] payload = Base64.decode(encodedString, Base64.NO_WRAP);
							if (AprsUtility.usePSK) {
								Log.i(TAG, "PSK enabled");
								byte[] PSKhash;
								SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
								String psk = sharedPref.getString("PSKText", "atakatak");
								try {
									MessageDigest digest = MessageDigest.getInstance("MD5");
									PSKhash = digest.digest(psk.getBytes());
								} catch (Exception e) {
									Log.d(TAG, "Decrypt PSK Hashing problem: " + e);
									return;
								}
								try {
									Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
									SecretKeySpec key = new SecretKeySpec(PSKhash, "AES");
									// first 16 bytes are IV
									byte[] iv = Arrays.copyOf(payload,16);
									cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
									// don't decrypt the IV
									byte[] cotAsBytes = cipher.doFinal(Arrays.copyOfRange(payload, 16, payload.length));
									cotEvent = this.cotShrinker.toCotEvent(cotAsBytes);
								} catch (Exception e) {
									Log.d(TAG, "Decrypt PSK problem: " + e);
									return;
								}
							} else {
									cotEvent = this.cotShrinker.toCotEvent(payload);
							}
							if (cotEvent.isValid()) {
								Log.d(TAG, "Dispatching: " + cotEvent.toString());
								CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
								MapItem mi = getMapView().getRootGroup().deepFindUID(cotEvent.getUID());
								mi.setMetaBoolean("archive", true);
								mi.persist(getMapView().getMapEventDispatcher(), null, this.getClass());
							}
						}
						ReadLogReceiver.getAprslog().append(String.format("%d: packet received\n-----\n", type));
						break;
				}
			break;
		}
	}
}
