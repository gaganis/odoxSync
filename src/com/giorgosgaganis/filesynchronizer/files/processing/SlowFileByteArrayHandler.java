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
import com.giorgosgaganis.filesynchronizer.utils.Statistics;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 23/01/17.
 */
public class SlowFileByteArrayHandler {
    private static final Logger logger = Logger.getLogger(SlowFileProcessor.class.getName());

    private static Statistics statistics = Statistics.INSTANCE;

    public void handleBytes(byte[] buffer, File file, long batchAreaOffset, Region currentRegion, FileTime batchLastModifiedTime) {

        byte[] slowDigest = calculateSlowDigest(batchAreaOffset, currentRegion.getOffset(), currentRegion.getSize(), file.getName(), buffer);
        Region region = file.getRegions().get(currentRegion.getOffset());
        region.setSlowDigest(slowDigest);
        region.setSlowModifiedTime(batchLastModifiedTime);
        statistics.bytesReadSlow.addAndGet(currentRegion.getSize());
    }


    private static byte[] calculateSlowDigest(long batchAreaOffset, long offset, long size, String fileName, byte[] buffer) {
        byte[] slowDigest;
        Hasher hasher = Hashing.sha256().newHasher();
        hasher.putBytes(buffer,
                Math.toIntExact(offset - batchAreaOffset),
                Math.toIntExact(size));
        slowDigest = hasher.hash().asBytes();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Calculated slow digest[" + slowDigest + "] for file ["
                    + fileName + "]" + offset
                    + ":" + (offset + size));
        }
        return slowDigest;
    }
}
