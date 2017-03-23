/*
 * Copyright (C) 2017 Giorgos Gaganis
 *
 * This file is part of odoxSync.
 *
 * odoxSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * odoxSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with odoxSync.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.giorgosgaganis.odoxsync.utils;

import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    public static Statistics INSTANCE = new Statistics();


    public AtomicLong bytesTransferred = new AtomicLong(0);
    public AtomicLong bytesReadFast = new AtomicLong(0);
    public AtomicLong bytesReadSlow = new AtomicLong(0);

    public AtomicLong collisions = new AtomicLong(0);

    private Statistics() {
    }

    public static void printStatistic(String statName, AtomicLong bytesTransferred) {
        long start = System.currentTimeMillis();
        long startBytes = bytesTransferred.get();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long duration = System.currentTimeMillis() - start;
        long endBytes = bytesTransferred.get();
        long bytes = endBytes - startBytes;
        long bytesPerSecond = bytes * 1000 / duration;
        System.out.println(statName + " bytes [" + humanReadableByteCount(endBytes, false)
                + "], bytes/s [" + humanReadableByteCount(bytesPerSecond, false) + "]");
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}