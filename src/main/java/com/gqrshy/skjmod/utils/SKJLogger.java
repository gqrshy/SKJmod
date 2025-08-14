package com.gqrshy.skjmod.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SKJLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("SKJmod");
    
    public static void info(String message) {
        LOGGER.info(message);
    }
    
    public static void warn(String message) {
        LOGGER.warn(message);
    }
    
    public static void error(String message) {
        LOGGER.error(message);
    }
    
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
    
    public static void debug(String message) {
        LOGGER.debug(message);
    }
}