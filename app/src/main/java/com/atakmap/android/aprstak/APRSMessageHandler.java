package com.atakmap.android.aprstak;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.helpers.DefaultEXIFactory;
import com.siemens.ct.exi.main.api.sax.EXIResult;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import utils.AprsUtility;

public class APRSMessageHandler implements CommsMapComponent.PreSendProcessor {

    private static final String TAG = "APRSMessageHandler";

    private Context context;

    public APRSMessageHandler(Context context) {
        CommsMapComponent.getInstance().registerPreSendProcessor(this);
        this.context = context;
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        if (cotEvent.toString().contains("All Chat Rooms")) {
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

            Log.d(TAG, "Size: " + cotAsBytes.length);            String encodedString;

            if (AprsUtility.usePSK) {
                com.atakmap.coremap.log.Log.i(TAG, "PSK enabled");
                ByteBuffer payload;
                byte[] PSKhash, cipherText;
                SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                String psk = sharedPref.getString("PSKText", "atakatak");
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    PSKhash = digest.digest(psk.getBytes("UTF-8"));
                } catch (Exception e) {
                    com.atakmap.coremap.log.Log.d(TAG, "Encrypt PSK Hashing problem: " + e);
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
                    com.atakmap.coremap.log.Log.d(TAG, "Encrypt PSK problem: " + e);
                    return;
                }
                encodedString = Base64.encodeToString(payload.array(), Base64.NO_WRAP);
            } else {
                encodedString = Base64.encodeToString(cotAsBytes, Base64.NO_WRAP);
            }

            Log.d(TAG, "Base64 string len: " + encodedString.length());
            Intent i = new Intent("org.aprsdroid.app.SEND_PACKET").setPackage("org.aprsdroid.app");
            i.putExtra("data", ">M," + encodedString);
            context.getApplicationContext().startForegroundService(i);
        }
    }
}
