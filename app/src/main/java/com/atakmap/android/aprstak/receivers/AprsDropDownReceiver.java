
package com.atakmap.android.aprstak.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import com.atakmap.android.aprstak.plugin.PluginLifecycle;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;

import com.atakmap.coremap.log.Log;

import utils.AprsUtility;

import com.atakmap.android.aprstak.plugin.R;

public class AprsDropDownReceiver extends DropDownReceiver implements
        OnStateListener{

    public static final String TAG = AprsDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.aprstak.SHOW_PLUGIN";

    private final View mainView;
    private final Context pluginContext;
    private MapView mapView;
    private AprsUtility aprsUtility;

    private Switch autoBroadcastSwitch;
    private Switch enableTNCSwitch;
    private Switch enablePSKSwitch;
    private NumberPicker autoBroadcastNPInterval;

    private Button viewAPRSLog;
    private TextView frequencyText;
    private EditText PSKTV;
    /**************************** CONSTRUCTOR *****************************/

    public AprsDropDownReceiver(final MapView mapView,
                                      final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mainView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;


        autoBroadcastSwitch = mainView.findViewById(R.id.autoBroadcast);
        autoBroadcastNPInterval = mainView.findViewById(R.id.autoBroadcastInterval);

        // TNC
        enableTNCSwitch = mainView.findViewById(R.id.TNC);
        frequencyText = mainView.findViewById(R.id.frequency);

        // PSK
        enablePSKSwitch = mainView.findViewById(R.id.PSKSwitch);
        PSKTV = mainView.findViewById(R.id.PSKET);
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {
            showDropDown(mainView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            viewAPRSLog = mainView.findViewById(R.id.viewAPRSLog);
            viewAPRSLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    android.util.Log.d(TAG, "onClick: ");
                    Intent intent = new Intent();
                    intent.setAction(ReadLogReceiver.SHOW_LOG);
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            });

            aprsUtility = AprsUtility.getInstance(mapView, pluginContext);

            enableTNCSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    AprsUtility.useTNC = b;
                    SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("useTNC", b);
                    editor.apply();
                }
            });

            if (aprsUtility.useTNC) {
                enableTNCSwitch.setChecked(true);

                if (!aprsUtility.aprsdroid_running) {
                    // make sure APRSDroid is running
                    Intent i = new Intent("org.aprsdroid.app.SERVICE").setPackage("org.aprsdroid.app");
                    context.getApplicationContext().startForegroundService(i);
                }
            } else {
                enableTNCSwitch.setChecked(false);
                if (aprsUtility.aprsdroid_running) {
                    // make sure APRSDroid is stopped
                    Intent i = new Intent("org.aprsdroid.app.SERVICE_STOP").setPackage("org.aprsdroid.app");
                    context.getApplicationContext().startForegroundService(i);
                }

            }

            enablePSKSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    AprsUtility.usePSK = b;
                    SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("usePSK", b);
                    editor.apply();
                }
            });

            if (aprsUtility.useTNC) {
                enableTNCSwitch.setChecked(true);
            } else {
                enableTNCSwitch.setChecked(false);
            }

            PSKTV.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d(TAG, String.format("PSK Text: %s", s.toString()));
                    SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("PSKText", s.toString());
                    editor.apply();
                }
            });

            frequencyText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.d(TAG, String.format("APRS Frequency: %s", s.toString()));
                    Intent i = new Intent("org.aprsdroid.app.FREQUENCY").setPackage("org.aprsdroid.app");
                    i.putExtra("frequency", s.toString());
                    context.getApplicationContext().startForegroundService(i);
                }
            });

            autoBroadcastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b && !aprsUtility.isAutoBeaconing()) {
                        aprsUtility.startABListener();
                        autoBroadcastNPInterval.setEnabled(false);
                    } else if (aprsUtility.isAutoBeaconing()) {
                        aprsUtility.stopABListener();
                        autoBroadcastNPInterval.setEnabled(true);
                    }
                }
            });

            if (aprsUtility.isAutoBeaconing()) {
                autoBroadcastSwitch.setChecked(true);
                autoBroadcastNPInterval.setEnabled(false);
            } else {
                autoBroadcastSwitch.setChecked(false);
                autoBroadcastNPInterval.setEnabled(true);
            }

            final String[] delays = new String[]{"5", "10", "15", "30", "60"};
            autoBroadcastNPInterval.setDisplayedValues(null);
            autoBroadcastNPInterval.setMinValue(0);
            autoBroadcastNPInterval.setMaxValue(delays.length - 1);
            autoBroadcastNPInterval.setWrapSelectorWheel(false);
            autoBroadcastNPInterval.setDisplayedValues(delays);

            SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
            int delay = sharedPref.getInt("autoBroadcastInterval", 0);
            if (delay == 0)
                autoBroadcastNPInterval.setValue(2);


            autoBroadcastNPInterval.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, final int oldVal, final int newVal) {
                    Log.d(TAG, String.format("AutoBroadcast Interval: %s", delays[newVal]));
                    SharedPreferences sharedPref = context.getSharedPreferences("aprs-prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("autoBroadcastInterval", Integer.parseInt(delays[newVal]));
                    editor.apply();
                }
            });


        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }
}
