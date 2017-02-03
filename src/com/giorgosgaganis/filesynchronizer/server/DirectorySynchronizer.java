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
package com.giorgosgaganis.filesynchronizer.server;

import com.fasterxml.jackson.databind.deser.Deserializers;
import com.giorgosgaganis.filesynchronizer.Client;
import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.client.ClientRegionMessage;
import com.giorgosgaganis.filesynchronizer.messages.BlankFileMessage;
import com.giorgosgaganis.filesynchronizer.messages.ClientFastDigestMessage;
import com.giorgosgaganis.filesynchronizer.messages.ClientSlowDigestMessage;
import com.giorgosgaganis.filesynchronizer.server.candidates.TransferCandidateFinder;
import com.giorgosgaganis.filesynchronizer.server.status.RegionWalker;
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.giorgosgaganis.filesynchronizer.utils.Contants.REGION_SIZE;

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

    private final DirectoryScanner fastDirectoryScanner = new DirectoryScanner(
            files,
            true,
            new CandidateQueueActivityStaler(clients));

    private final DirectoryScanner slowDirectoryScanner = new DirectoryScanner(
            files,
            false,
            new CandidateQueueActivityStaler(clients));

    public String workingDirectory;


    public void start(String workingDirectory) {
        this.workingDirectory = workingDirectory;

        startStatisticsPrintThread();
        startProgressTrackingThread();

        fastDirectoryScanner.setWorkingDirectory(workingDirectory);
        fastDirectoryScanner.scanDirectoryAndFiles();

        slowDirectoryScanner.setWorkingDirectory(workingDirectory);
        slowDirectoryScanner.scanDirectoryAndFiles();

        transferCandidateFinder.lookForRegionsToTransfer();
    }

    private void startProgressTrackingThread() {
        new Thread(null, () -> {
            do {
                try {
                    new RegionWalker(files, clients).walk();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (true);
        }, "odox-progress-tracker").start();
    }

    private void startStatisticsPrintThread() {
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesTransferred;
                Statistics.printStatistic("transfered", counter);
            } while (true);
        }).start();
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesReadFast;
                Statistics.printStatistic("read fast", counter);
            } while (true);
        }).start();
        new Thread(() -> {

            do {
                AtomicLong counter = Statistics.INSTANCE.bytesReadSlow;
                Statistics.printStatistic("read slow", counter);
            } while (true);
        }).start();
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

        try {
            int clientId = clientRegionMessage.getClientId();
            int fileId = clientRegionMessage.getFileId();

            String fileName = files.get(fileId).getName();

            Client client = clients.get(clientId);

            File clientFile = new File(fileName);
            clientFile.setId(fileId);

            File existingFile = client.files.putIfAbsent(fileId, clientFile);

            if (existingFile != null) {
                clientFile = existingFile;
            }
            ConcurrentHashMap<Long, Region> clientRegions = clientFile.getRegions();
            Region region = clientRegionMessage.getRegion();

            clientRegions.put(region.getOffset(), region);
            logger.fine("Added client region " + region);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void addClientFastDigests(List<ClientFastDigestMessage> clientFastDigestMessages) {
        for (ClientFastDigestMessage clientFastDigestMessage : clientFastDigestMessages) {

            int clientId = clientFastDigestMessage.getClientId();
            int fileId = clientFastDigestMessage.getFileId();
            long offset = clientFastDigestMessage.getOffset();
            int fastDigest = clientFastDigestMessage.getFastDigest();

            Region clientRegion = getClientRegion(clientId, fileId, offset);
            clientRegion.setQuickDigest(fastDigest);
        }
    }

    private Region getClientRegion(int clientId, int fileId, long offset) {
        Client client = clients.get(clientId);

        File clientFile = getOrCreateClientFile(fileId, client);

        Region serverRegion = files.get(fileId).getRegions().get(offset);

        ConcurrentHashMap<Long, Region> clientRegions = clientFile.getRegions();
        return clientRegions.computeIfAbsent(offset, (aLong) -> new Region(serverRegion.getOffset(), serverRegion.getSize()));
    }

    private File getOrCreateClientFile(int fileId, Client client) {
        String fileName = files.get(fileId).getName();

        return client.files.computeIfAbsent(fileId, integer -> {
            File newFile = new File(fileName);
            newFile.setId(fileId);
            return newFile;
        });
    }

    public void addBlankFile(BlankFileMessage blankFileMessage) {
        int clientId = blankFileMessage.getClientId();
        int fileId = blankFileMessage.getFileId();

        Client client = clients.get(clientId);
        File clientFile = getOrCreateClientFile(fileId, client);

        ConcurrentHashMap<Long, Region> clientRegions = clientFile.getRegions();
        ConcurrentHashMap<Long, Region> serverRegions = files.get(fileId).getRegions();

        byte[] emptyByteArrayRegionSize = new byte[(int) REGION_SIZE];
        byte[] slowDigestForWholeEmptyRegion = Base64.getEncoder().encode(emptyByteArrayRegionSize);

        for (Region serverRegion : serverRegions.values()) {
            long offset = serverRegion.getOffset();
            long size = serverRegion.getSize();

            Region region = clientRegions.computeIfAbsent(offset, (aLong) ->
                    new Region(offset, size));
            region.setQuickDigest(0);
            if(region.getSize() == REGION_SIZE) {
                region.setSlowDigest(slowDigestForWholeEmptyRegion);
            } else {
                byte[] emptyArraySmallerSize = new byte[(int) region.getSize()];
                region.setSlowDigest(
                        Base64.getEncoder()
                                .encode(emptyArraySmallerSize));
            }
        }
    }

    public void addClientSlowDigests(List<ClientSlowDigestMessage> clientSlowDigestMessage) {
        for (ClientSlowDigestMessage slowDigestMessage : clientSlowDigestMessage) {
            int clientId = slowDigestMessage.getClientId();
            int fileId = slowDigestMessage.getFileId();
            long offset = slowDigestMessage.getOffset();
            byte[] slowDigest = slowDigestMessage.getSlowDigest();

            Region clientRegion = getClientRegion(clientId, fileId, offset);
            clientRegion.setSlowDigest(slowDigest);
        }
    }
}
