package com.github.suzhenhui.jvc;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

public class Logging {
    private static Log log;

    public static Log getLog() {
        if (log == null) {
            log = new SystemStreamLog();
        }

        return log;
    }

    public static void setLog(Log log) {
        Logging.log = log;
    }
}
