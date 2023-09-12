package com.atakmap.android.aprstak.receivers;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.atakmap.android.aprstak.plugin.R;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;

import utils.DropDownManager;

public class ReadLogReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = AprsDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_LOG = "com.atakmap.android.aprstak.receivers.SHOW_LOG";
    private LayoutInflater inflater;
    private View mainView;
    private Intent intent;

    public static TextView aprslog;

    public ReadLogReceiver(MapView mapView, Context context) {
        super(mapView);

        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mainView = inflater.inflate(R.layout.read_me, null);

        ImageButton backButton = mainView.findViewById(R.id.backButtonReadMeView);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReadLogReceiver.this.onBackButtonPressed();
            }
        });

        aprslog = mainView.findViewById(R.id.aprslog);
    }

    public static TextView getAprslog() {
        return aprslog;
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
        if(intent == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent was null");
            return;
        }

        if (intent.getAction() == null) {
            android.util.Log.w(TAG, "Doing nothing, because intent action was null");
            return;
        }

        if (intent.getAction().equals(SHOW_LOG)) {
            this.intent = intent;
            showDropDown(mainView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    FULL_HEIGHT, false);

        }
    }

    protected boolean onBackButtonPressed() {
        DropDownManager.getInstance().clearBackStack();
        DropDownManager.getInstance().removeFromBackStack();
        intent.setAction(AprsDropDownReceiver.SHOW_PLUGIN);
        AtakBroadcast.getInstance().sendBroadcast(intent);
        return true;
    }

}
