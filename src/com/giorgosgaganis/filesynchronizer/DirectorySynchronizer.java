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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
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

    private final FileScanner fileScanner = new FileScanner(files);

    public String workingDirectory;


    public void start(String workingDirectory) {
        this.workingDirectory = workingDirectory;

        fileScanner.setWorkingDirectory(workingDirectory);

        new Thread(() -> {
            do {
                fileScanner.scanDirectoryAndFiles();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            } while (true);
        }).start();

        transferCandidateFinder.lookForRegionsToTransfer();
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
}
