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

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class SyncClient {
    private static final Logger logger = Logger.getLogger(SyncClient.class.getName());

    private final String workingDirectory;
    private int clientId = -1;

    private ConcurrentHashMap<Integer, File> files = new ConcurrentHashMap<>();

    private RestClient restClient = new RestClient();
    private RegionDataHandler regionDataHandler = new RegionDataHandler(restClient, files);
    private ClientRegionMessageHandler clientRegionMessageHandler = new ClientRegionMessageHandler(restClient);


    public SyncClient(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }


    public static void main(String[] args) throws IOException {

        String workingDirectory = args.length > 0 ? args[0] : ".";
        LoggingUtils.configureLogging();

        SyncClient syncClient = new SyncClient(workingDirectory);
        syncClient.start();

//        System.out.println("files = " + files);
    }

    private void start() {

        logger.info("Starting sync client");
        clientId = restClient.getClientId();
        new Thread(() -> {
            do {
                processFiles();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }).start();
        regionDataHandler.start();
    }

    private void processFiles() {
        Collection<File> files = restClient.getFiles();

        files.stream().parallel().forEach(this::processFile);
    }


    private void processFile(File file) {

        logger.fine("Processing file [" + file.getName() + "]");
        Path root = Paths.get(workingDirectory).toAbsolutePath().normalize();
        Path filePath = root.resolve(file.getName());

        file.setAbsolutePath(filePath.toAbsolutePath());

        files.putIfAbsent(file.getId(), file);

        try {
            if (Files.notExists(filePath)) {
                Path parent = filePath.getParent();
                Files.createDirectories(parent);

                try (
                        RandomAccessFile randomAccessFile = new RandomAccessFile(file.getAbsolutePath().toFile(), "rw");
                        FileChannel channel = randomAccessFile.getChannel()

                ) {
                    long counter = 0;
                    RegionCalculator regionCalculator = new RegionCalculator(workingDirectory, file);
                    regionCalculator.calculateForSize(file.getSize());


                    for (Region region : file.getRegions().values()) {
                        long sum = 0;
//                        Hasher hasher = Hashing.sha256().newHasher();
                        Hasher hasher = null;
                        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, region.getOffset(), region.getSize());
                        for (long i = 0; i < region.getSize(); i++) {
                            byte b = (byte) (counter % 127);
                            mappedByteBuffer.put(b);

//                            hasher.putByte(b);
                            sum += b;

                            counter++;
                        }

                        clientRegionMessageHandler.submit(
                                createClientRegionMessage(file, region, sum, hasher));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ClientRegionMessage createClientRegionMessage(File file, Region region, long sum, Hasher hasher) {
        Region clientRegion = new Region(region.getOffset(), region.getSize());
        clientRegion.setQuickDigest(sum);
//        clientRegion.setSlowDigest(hasher.hash().asBytes());
        ClientRegionMessage clientRegionMessage = new ClientRegionMessage();
        clientRegionMessage.setClientId(clientId);
        clientRegionMessage.setFileId(file.getId());
        clientRegionMessage.setRegion(clientRegion);
        return clientRegionMessage;
    }
}
