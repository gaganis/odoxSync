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
package com.giorgosgaganis.odoxsync.client;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.client.net.RestClient;
import com.giorgosgaganis.odoxsync.utils.Statistics;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.giorgosgaganis.odoxsync.files.processing.FastFileProcessor.SAMPLE_SIZE;

/**
 * Created by gaganis on 15/01/17.
 */
public class RegionDataHandler extends Thread {
    private static final Logger logger = Logger.getLogger(RegionDataHandler.class.getName());
    private final Statistics statistics = Statistics.INSTANCE;

    private RestClient restClient;
    private ClientMessageHandler clientMessageHandler;
    private ConcurrentHashMap<Integer, File> files;
    private int clientId;

    public RegionDataHandler(RestClient restClient, ClientMessageHandler clientMessageHandler, ConcurrentHashMap<Integer, File> files) {
        this.restClient = restClient;
        this.clientMessageHandler = clientMessageHandler;
        this.files = files;
    }

    @Override
    public void run() {
        logger.info("Starting Data Transfer Thread");

        startStatisticsThread();

        for (int threadCounter = 0; threadCounter < 2; threadCounter++) {

            new Thread(() -> {
                do {
                    try {
                        logger.fine("Requesting region data");
                        RegionDataParams regionData = restClient.getRegionData();
                        if (regionData == null) {
                            logger.fine("Nothing to transfer");
                            continue;
                        }

                        logger.fine("Starting to copy region [" + regionData + "]");
                        File file = files.get(regionData.fileId);
                        Path absolutePath = file.getAbsolutePath();

                        try (
                                RandomAccessFile randomAccessFile = new RandomAccessFile(absolutePath.toFile(), "rw");
                        ) {
                            Hasher hasher = Hashing.sha256().newHasher();
                            randomAccessFile.seek(regionData.offset);
                            randomAccessFile.write(regionData.bytes);
                            statistics.bytesTransferred.addAndGet(regionData.bytes.length);
                            hasher.putBytes(regionData.bytes);

                            long sampleSize = regionData.size <= SAMPLE_SIZE ? regionData.size : SAMPLE_SIZE;
                            int offset = (int) (regionData.size - sampleSize);

                            int sum = 0;
                            for (int i = offset; i < regionData.bytes.length; i++) {
                                byte b = regionData.bytes[i];
                                sum += b;
                            }

                            clientMessageHandler.submitClientRegionMessage(clientId, file, regionData.offset, regionData.size, sum, hasher.hash().asBytes());
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

    private void startStatisticsThread() {
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesTransferred;
                Statistics.printStatistic("transfered", counter);
            } while (true);
        }).start();
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
