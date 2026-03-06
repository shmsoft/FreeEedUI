/*
 * Copyright SHMsoft, Inc.
 * Licensed under the Apache License, Version 2.0.
 */
package org.freeeed.search.web.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads configuration values from ~/.freeeed/.env at runtime.
 * All methods return sensible defaults if the file or key is absent.
 */
public class EnvConfig {

    private static final String ENV_FILE_PATH = System.getProperty("user.home") + "/.freeeed/.env";

    private static Properties load() {
        Properties props = new Properties();
        File f = new File(ENV_FILE_PATH);
        if (!f.exists()) {
            return props;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            props.load(fis);
        } catch (IOException ignored) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored2) {
                }
            }
        }
        return props;
    }

    /**
     * Returns the backend port from PORT key in ~/.freeeed/.env.
     * Falls back to {@code defaultPort} if the file or key is absent.
     */
    public static int getPort(int defaultPort) {
        String val = load().getProperty("PORT");
        if (val != null && !val.trim().isEmpty()) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultPort;
    }

    /**
     * Returns the full AI backend base URL, e.g. "http://localhost:8000".
     * Reads PORT from ~/.freeeed/.env; falls back to {@code defaultPort}.
     */
    public static String getAiBackendUrl(int defaultPort) {
        return "http://localhost:" + getPort(defaultPort);
    }
}
