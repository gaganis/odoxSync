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
package com.giorgosgaganis.filesynchronizer.files.processing;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.files.processing.handlers.FastDigestHandler;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;

import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 23/01/17.
 */
public class FastFileByteArrayHandler {
    private static final Logger logger = Logger.getLogger(SlowFileProcessor.class.getName());

    private static Statistics statistics = Statistics.INSTANCE;

    private final FastDigestHandler fastDigestHandler;

    public FastFileByteArrayHandler(FastDigestHandler fastDigestHandler) {
        this.fastDigestHandler = fastDigestHandler;
    }

    public void handleBytes(byte[] buffer, File file, Region currentRegion, FileTime fileLastModifiedTime) {

        Integer fastDigest = calculateFastDigest(currentRegion.getOffset(), currentRegion.getSize(), file.getName(), buffer);
        fastDigestHandler.handleFastDigest(buffer, file, currentRegion, fastDigest, fileLastModifiedTime);
        statistics.bytesReadFast.addAndGet(currentRegion.getSize());
    }


    private static Integer calculateFastDigest(long offset, long size, String fileName, byte[] buffer) {
        Integer quickDigest = null;
        int sum = 0;
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            sum += b;
        }
        quickDigest = sum;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Calculated fast digest[" + quickDigest
                    + "] for file [" + fileName + "]" + offset
                    + ":" + (offset + size));
        }
        return quickDigest;
    }
}
