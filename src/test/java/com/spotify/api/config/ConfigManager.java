package com.spotify.api.config;

import org.aeonbits.owner.ConfigFactory;

public class ConfigManager {

    private static FrameworkConfig frameworkConfig;

    private ConfigManager() {}

    public static FrameworkConfig getConfig() {
        if (frameworkConfig == null) {
            frameworkConfig = ConfigFactory.create(FrameworkConfig.class, System.getProperties(), System.getenv());
        }
        return frameworkConfig;
    }
}
