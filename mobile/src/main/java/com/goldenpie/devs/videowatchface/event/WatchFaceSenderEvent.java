package com.goldenpie.devs.videowatchface.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author anton
 * @version 3.4
 * @since 18.10.16
 */
@Data
@AllArgsConstructor
public class WatchFaceSenderEvent {
    private String path;
}
