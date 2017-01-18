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
package com.giorgosgaganis.filesynchronizer.net.client;

import com.giorgosgaganis.filesynchronizer.Contants;
import com.giorgosgaganis.filesynchronizer.File;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Created by gaganis on 15/01/17.
 */
public class RegionDataHandler extends Thread {
    private static final Logger logger = Logger.getLogger(RegionDataHandler.class.getName());

    private AtomicLong bytesTransferred = new AtomicLong(0);

    private RestClient restClient;
    private ClientRegionMessageHandler clientRegionMessageHandler;
    private ConcurrentHashMap<Integer, File> files;
    private int clientId;

    public RegionDataHandler(RestClient restClient, ClientRegionMessageHandler clientRegionMessageHandler, ConcurrentHashMap<Integer, File> files) {
        this.restClient = restClient;
        this.clientRegionMessageHandler = clientRegionMessageHandler;
        this.files = files;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public void run() {
        logger.info("Starting Data Transfer Thread");
        new Thread(() -> {

            do {
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
                logger.info("bytes [" + humanReadableByteCount(endBytes, false)
                        + "], bytes/s [" + humanReadableByteCount(bytesPerSecond, false) + "]");
            } while (true);
        }).start();

        for (int threadCounter = 0; threadCounter < 4; threadCounter++) {

            new Thread(() -> {
                do {
                    try {
                        logger.fine("Requesting region data");
                        RegionDataParams regionData = restClient.getRegionData();
                        if (regionData == null) {
                            logger.fine("Nothing to transfer");
                            return;
                        }

                        logger.fine("Starting to copy region [" + regionData + "]");
                        File file = files.get(regionData.fileId);
                        Path absolutePath = file.getAbsolutePath();

                        try (
                                RandomAccessFile randomAccessFile = new RandomAccessFile(absolutePath.toFile(), "rw");
                                FileChannel channel = randomAccessFile.getChannel();
                        ) {
                            MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, regionData.offset, regionData.size);
                            Hasher hasher = Hashing.sha256().newHasher();
                            mappedByteBuffer.put(regionData.bytes);
                            bytesTransferred.addAndGet(regionData.size);
                            hasher.putBytes(regionData.bytes);

                            int sum = 0;
                            for (int i = 0; i < regionData.bytes.length; i += Contants.BYTE_SKIP_LENGHT) {
                                byte b = regionData.bytes[i];
                                sum += b;
                            }

                            clientRegionMessageHandler.submitClientRegionMessage(clientId, file, regionData.offset, regionData.size, sum, hasher.hash().asBytes());
                            regionData.response.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (true);
            }).start();
        }
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
