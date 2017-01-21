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
package com.giorgosgaganis.filesynchronizer;

import com.giorgosgaganis.filesynchronizer.utils.Statistics;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.nio.MappedByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.giorgosgaganis.filesynchronizer.Contants.BYTE_SKIP_LENGHT;

/**
 * Created by gaganis on 20/01/17.
 */
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());

    private static Statistics statistics = Statistics.INSTANCE;
    private static DigestResult digestResult;
    private final File file;

    public FileProcessor(File file) {
        this.file = file;
    }

    private static void processBytes(boolean isFast, long offset, long size, String fileName, MappedByteBuffer mappedByteBuffer, byte[] buffer) {


        Integer quickDigest = calculateFastDigest(offset, size, fileName, buffer);

        byte[] slowDigest = null;
        if(isFast){
            statistics.bytesReadFast.addAndGet(buffer.length);
        } else {
            slowDigest = calculateSlowDigest(offset, size, fileName, buffer);
            statistics.bytesReadSlow.addAndGet(buffer.length);
        }
        digestResult = new DigestResult(quickDigest, slowDigest);
    }

    private static byte[] calculateSlowDigest(long offset, long size, String fileName, byte[] buffer) {
        byte[] slowDigest;Hasher hasher = Hashing.sha256().newHasher();
        hasher.putBytes(buffer);
        slowDigest = hasher.hash().asBytes();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Calculated slow digest[" + slowDigest + "] for file ["
                    + fileName + "]" + offset
                    + ":" + (offset + size));
        }
        return slowDigest;
    }

    private static Integer calculateFastDigest(long offset, long size, String fileName, byte[] buffer) {
        Integer quickDigest = null;
        int sum = 0;
        for (int i = 0; i < buffer.length; i += BYTE_SKIP_LENGHT) {
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

    public void process(byte[] buffer) {
    }
}
