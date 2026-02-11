package io.metersphere.platform.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Date/time utilities for Phabricator timestamps
 */
public class PhabricatorDateUtils {
    
    public static LocalDateTime fromEpoch(long epochSeconds) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(epochSeconds), 
            ZoneId.systemDefault()
        );
    }
    
    public static long toEpoch(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}