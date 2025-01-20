
package com.atakmap.android.aprstak.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.util.Disposable;

public class PluginTool extends AbstractPluginTool implements Disposable {



    public PluginTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                "com.atakmap.android.aprstak.SHOW_PLUGIN");
        PluginNativeLoader.init(context);
    }

    @Override
    public void dispose() {
    }

}
