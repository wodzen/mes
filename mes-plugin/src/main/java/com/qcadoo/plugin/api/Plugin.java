package com.qcadoo.plugin.api;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

public interface Plugin extends PersistentPlugin {

    PluginInformation getPluginInformation();

    Set<PluginDependencyInformation> getRequiredPlugins();

    boolean isSystemPlugin();

    void changeStateTo(PluginState state);

    String getFilename();

    int compareVersion(PersistentPlugin plugin);

    ClassLoader getClassLoader();

    URL getResource(String name);

    InputStream getResourceAsStream(String name);

    void init();

}
