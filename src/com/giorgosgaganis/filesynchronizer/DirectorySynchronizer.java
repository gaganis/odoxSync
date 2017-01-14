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

import com.giorgosgaganis.filesynchronizer.digest.LongDigester;
import com.giorgosgaganis.filesynchronizer.digest.ShaDigester;
import com.giorgosgaganis.filesynchronizer.net.client.ClientRegionMessage;
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;
import com.google.common.hash.HashCode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 13/01/17.
 */
public class DirectorySynchronizer {
    private static final Logger logger = Logger.getLogger(DirectorySynchronizer.class.getName());

    public static final DirectorySynchronizer INSTANCE = new DirectorySynchronizer();

    private final AtomicInteger fileIdCounter = new AtomicInteger(1);
    public final ConcurrentHashMap<Integer, File> files = new ConcurrentHashMap<>();

    private final AtomicInteger clientIdCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<>();


    public void start() {
//        do {
        scanDirectoryAndFiles();
//        } while (true);
    }

    private void scanDirectoryAndFiles() {
        DirectoryScanner ds = new DirectoryScanner(files, fileIdCounter);
        ds.scan();

        files.values().parallelStream().forEach(this::processFile);
    }

    private void processFile(File file) {
        try {
            Path path = Paths.get(file.getName());
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            if (lastModifiedTime.compareTo(file.getLastModified()) < 0) {
                logger.fine(" File has not been modified [" + file.getName() + "]");
                return;
            }

            long size = Files.size(path);
            if (size != file.getSize()) {
                logger.fine("Recalculation regions for [" + file.getName() + "]");
                reCalculateRegions(file);
            }

            logger.fine("Starting scan for [" + file.getName() + "]");
            scanFile(file);
            logger.fine("Finished scan for [" + file.getName() + "]");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void scanFile(File file) throws IOException {
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(file.getName(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {
            for (Region region : file.getRegions().values()) {

                scanRegion(region, channel);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Calculated fast digest[" + region.getQuickDigest()
                            + "] for file [" + file.getName() + "]" + region.getOffset()
                            + ":" + (region.getOffset() + region.getSize()));

                    String slowDigest = HashCode.fromBytes(region.getSlowDigest()).toString();
                    logger.finer("Calculated slow digest[" + slowDigest + "] for file ["
                            + file.getName() + "]" + region.getOffset()
                            + ":" + (region.getOffset() + region.getSize()));

                }
            }
        }
    }

    private void scanRegion(Region region, FileChannel channel) throws IOException {
        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, region.getOffset(), region.getSize());

        LongDigester longDigester = new LongDigester();
        region.setQuickDigest(
                longDigester.digest(mappedByteBuffer));


        mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, region.getOffset(), region.getSize());
        ShaDigester shaDigester = new ShaDigester();
        region.setSlowDigest(
                shaDigester.digest(mappedByteBuffer)
        );

    }

    private void reCalculateRegions(File file) throws IOException {
        RegionCalculator regionCalculator = new RegionCalculator(file);
        regionCalculator.calculate();
    }

    public static void main(String[] args) throws IOException {
        LoggingUtils.configureLogging();


        DirectorySynchronizer fs = new DirectorySynchronizer();

        fs.start();

    }

    public int setupClient() {
        int clientId = clientIdCounter.getAndIncrement();
        Client client = new Client(clientId);
        clients.put(clientId, client);
        return clientId;
    }

    public void addClientRegion(ClientRegionMessage clientRegionMessage) {

        int clientId = clientRegionMessage.getClientId();
        int fileId = clientRegionMessage.getFileId();

        String fileName = files.get(fileId).getName();

        Client client = clients.get(clientId);

        client.files.putIfAbsent(fileId, new File(fileName));
        File file = client.files.get(fileId);
        ConcurrentHashMap<Long, Region> clientRegions = file.getRegions();
        Region region = clientRegionMessage.getRegion();

        clientRegions.put(region.getOffset(), region);
        logger.fine("Added client region " + region);
    }
}
