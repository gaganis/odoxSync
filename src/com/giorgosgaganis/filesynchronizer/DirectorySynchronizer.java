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

import com.giorgosgaganis.filesynchronizer.net.client.ClientRegionMessage;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Created by gaganis on 13/01/17.
 */
public class DirectorySynchronizer {
    private static final Logger logger = Logger.getLogger(DirectorySynchronizer.class.getName());

    public static final DirectorySynchronizer INSTANCE = new DirectorySynchronizer();

    public final ConcurrentHashMap<Integer, File> files = new ConcurrentHashMap<>();

    public final ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<>();


    private TransferCandidateFinder transferCandidateFinder = new TransferCandidateFinder(
            files,
            clients);

    private final FileScanner fastFileScanner = new FileScanner(files, true);
    private final FileScanner slowFileScanner = new FileScanner(files, false);

    public String workingDirectory;


    public void start(String workingDirectory) {
        this.workingDirectory = workingDirectory;

        startStatisticsPrintThread();

        fastFileScanner.setWorkingDirectory(workingDirectory);
        fastFileScanner.scanDirectoryAndFiles();

        slowFileScanner.setWorkingDirectory(workingDirectory);
        slowFileScanner.scanDirectoryAndFiles();

        transferCandidateFinder.lookForRegionsToTransfer();
    }

    private void startStatisticsPrintThread() {
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesTransferred;
                printStatistic("transfered", counter);
            } while (true);
        }).start();
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesReadFast;
                printStatistic("read fast", counter);
            } while (true);
        }).start();
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesReadSlow;
                printStatistic("read slow", counter);
            } while (true);
        }).start();
    }

    private void printStatistic(String statName, AtomicLong bytesTransferred) {
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


    public static void main(String[] args) throws IOException {
        LoggingUtils.configureLogging();


        DirectorySynchronizer fs = new DirectorySynchronizer();

        fs.start(".");

    }

    public int setupClient() {
        Client collision;
        int clientId;
        do {
            clientId = ThreadLocalRandom.current().nextInt();
            Client client = new Client(clientId);
            collision = clients.putIfAbsent(clientId, client);
        } while (collision != null);
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

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
