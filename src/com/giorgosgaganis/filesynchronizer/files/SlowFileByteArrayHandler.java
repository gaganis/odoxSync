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
package com.giorgosgaganis.filesynchronizer.files;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 23/01/17.
 */
public class SlowFileByteArrayHandler implements FileByteArrayHandler {
    private static final Logger logger = Logger.getLogger(SlowFileProcessor.class.getName());

    private static Statistics statistics = Statistics.INSTANCE;

    @Override
    public void handleBytes(byte[] buffer, File file, Region currentRegion) {

        byte[] slowDigest = calculateSlowDigest(currentRegion.getOffset(), currentRegion.getSize(), file.getName(), buffer);
        Region region = file.getRegions().get(currentRegion.getOffset());
        region.setSlowDigest(slowDigest);
        statistics.bytesReadSlow.addAndGet(buffer.length);
    }


    private static byte[] calculateSlowDigest(long offset, long size, String fileName, byte[] buffer) {
        byte[] slowDigest;
        Hasher hasher = Hashing.sha256().newHasher();
        hasher.putBytes(buffer);
        slowDigest = hasher.hash().asBytes();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Calculated slow digest[" + slowDigest + "] for file ["
                    + fileName + "]" + offset
                    + ":" + (offset + size));
        }
        statistics.bytesReadSlow.addAndGet(buffer.length);
        return slowDigest;
    }
}
