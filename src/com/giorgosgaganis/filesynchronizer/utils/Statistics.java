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
package com.giorgosgaganis.filesynchronizer.utils;

import com.giorgosgaganis.filesynchronizer.DirectorySynchronizer;

import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    public static Statistics INSTANCE = new Statistics();


    public AtomicLong bytesTransferred = new AtomicLong(0);
    public AtomicLong bytesReadFast = new AtomicLong(0);
    public AtomicLong bytesReadSlow = new AtomicLong(0);

    private Statistics(){
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
        System.out.println(statName + " bytes [" + DirectorySynchronizer.humanReadableByteCount(endBytes, false)
                + "], bytes/s [" + DirectorySynchronizer.humanReadableByteCount(bytesPerSecond, false) + "]");
    }
}