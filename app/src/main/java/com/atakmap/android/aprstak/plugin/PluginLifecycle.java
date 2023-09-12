
package com.atakmap.android.aprstak.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import com.atakmap.android.aprstak.AprsMapComponent;


public class PluginLifecycle extends AbstractPlugin implements IPlugin {

    public PluginLifecycle(IServiceController serviceController) {
        super(serviceController, new PluginTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new AprsMapComponent());
    }
}