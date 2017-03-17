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
package com.giorgosgaganis.filesynchronizer.client;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.client.net.RestClient;
import com.giorgosgaganis.filesynchronizer.files.FileScanner;
import com.giorgosgaganis.filesynchronizer.files.processing.FastFileProcessorFactory;
import com.giorgosgaganis.filesynchronizer.files.processing.SlowFileProcessorFactory;
import com.giorgosgaganis.filesynchronizer.files.processing.handlers.FastDigestHandler;
import com.giorgosgaganis.filesynchronizer.files.processing.handlers.SlowDigestHandler;
import com.giorgosgaganis.filesynchronizer.messages.BlankFileMessage;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by gaganis on 02/02/17.
 */
public class FileOperations {
    private final static Logger logger = Logger.getLogger(FileOperations.class.getName());

    private final ConcurrentHashMap<Integer, File> fastProcessedFiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, File> slowProcessedFiles = new ConcurrentHashMap<>();
    private final ClientMessageHandler clientMessageHandler;
    private final ConcurrentHashMap<Integer, File> allFiles;
    private final RestClient restClient;
    private final String workingDirectory;

    private int clientId;

    public FileOperations(ClientMessageHandler clientMessageHandler, ConcurrentHashMap<Integer, File> allFiles, RestClient restClient, String workingDirectory) {

        this.clientMessageHandler = clientMessageHandler;
        this.allFiles = allFiles;
        this.restClient = restClient;
        this.workingDirectory = workingDirectory;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    void processFiles() {
        Collection<File> files = restClient.getFiles();

        initAbsolutePaths(files);

        files.stream().forEach((file) -> allFiles.putIfAbsent(file.getId(), file));

        List<File> blankFiles = files
                .stream()
                .filter((file) -> Files.notExists(file.getAbsolutePath()))
                .collect(Collectors.toList());

        blankFiles.stream().forEach(this::blankFile);

        List<File> existingFiles = files
                .stream()
                .filter((file) -> Files.exists(file.getAbsolutePath()))
                .collect(Collectors.toList());

        existingFiles.stream().forEach(this::resizeFile);

        FutureTask<?> fastTask = new FutureTask(
                () -> existingFiles.stream().forEach(this::fastScanFile),
                null);
        fastTask.run();

        FutureTask<?> slowTask = new FutureTask(
                () -> existingFiles.stream().forEach(this::slowScanFile),

                null);
        slowTask.run();

        try {
            fastTask.get();
            slowTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void resizeFile(File file) {
        Path absolutePath = file.getAbsolutePath();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(absolutePath.toFile(), "rw")) {
            randomAccessFile.setLength(file.getSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initAbsolutePaths(Collection<File> fileCollection) {
        Path root = Paths.get(workingDirectory).toAbsolutePath().normalize();
        fileCollection.stream().forEach((file -> file.setAbsolutePath(root.resolve(file.getName()))));
    }

    private void blankFile(File file) {

        try {
            if ((!fastProcessedFiles.containsKey(file.getId()))) {
                Path parent = file.getAbsolutePath().getParent();
                Files.createDirectories(parent);

                restClient.postBlankFileMessage(
                        new BlankFileMessage(clientId, file.getId()));
            }
            fastProcessedFiles.put(file.getId(), file);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failure while initializing blank file [" + file.getName() + "]", e);
        }

    }

    private void slowScanFile(File file) {

        try {
            if (!slowProcessedFiles.containsKey(file.getId())) {
                logger.fine("Beginning slow scan for file [" + file.getName() + "}");

                SlowDigestHandler slowDigestHandler =
                        new ClientRegionMessageSlowDigestHandler(clientId, clientMessageHandler);

                FileScanner fileScanner = new FileScanner(workingDirectory,
                        new SlowFileProcessorFactory(slowDigestHandler), () -> {
                });
                fileScanner.scanFile(file);
                slowProcessedFiles.put(file.getId(), file);
                logger.fine("Done slow scan for file [" + file.getName() + "}");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failure while slow scanning file [" + file.getName() + "]", e);
        }
    }

    private void fastScanFile(File file) {
        try {
            if (!fastProcessedFiles.containsKey(file.getId())) {
                logger.fine("Beginning fast scan for file [" + file.getName() + "}");

                FastDigestHandler fastDigestHandler =
                        new ClientRegionMessageFastDigestHandler(clientId, clientMessageHandler);

                FileScanner fileScanner = new FileScanner(workingDirectory,
                        new FastFileProcessorFactory(fastDigestHandler), () -> {
                });
                fileScanner.scanFile(file);
                fastProcessedFiles.put(file.getId(), file);
                logger.fine("Done fast scan for file [" + file.getName() + "}");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failure while fast scanning file [" + file.getName() + "]", e);
        }
    }


    private void processFile(File file) {

        logger.fine("Processing file [" + file.getName() + "]");
        Path root = Paths.get(workingDirectory).toAbsolutePath().normalize();
        Path filePath = root.resolve(file.getName());

        file.setAbsolutePath(filePath.toAbsolutePath());

        boolean wasInTheMap = allFiles.putIfAbsent(file.getId(), file) != null;

        try {
            if (Files.notExists(filePath)) {
                Path parent = filePath.getParent();
                Files.createDirectories(parent);

                RegionProcessor regionProcessor = (region, hasher, mappedByteBuffer) -> processByteBufferWrite(region, hasher, mappedByteBuffer);
                processRegions(file, regionProcessor, "rw", FileChannel.MapMode.READ_WRITE);
            } else if (!wasInTheMap) {
                FastDigestHandler fastDigestHandler = new ClientRegionMessageFastDigestHandler(clientId, clientMessageHandler);

                FileScanner fileScanner = new FileScanner(workingDirectory,
                        new FastFileProcessorFactory(fastDigestHandler), () -> {
                });
                fileScanner.scanFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRegions(File file, RegionProcessor regionProcessor, String randomAccessFileMode, FileChannel.MapMode mapMode) throws IOException {
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(file.getAbsolutePath().toFile(), randomAccessFileMode);
                FileChannel channel = randomAccessFile.getChannel()

        ) {
            RegionCalculator.calculateForSize(file, file.getSize());


            for (Region region : file.getRegions().values()) {
                Hasher hasher = Hashing.sha256().newHasher();
                MappedByteBuffer mappedByteBuffer = channel.map(mapMode, region.getOffset(), region.getSize());

                int sum = regionProcessor.processRegion(region, hasher, mappedByteBuffer);

                region.setQuickDigest(sum);

                byte[] slowDigest = hasher.hash().asBytes();
                region.setSlowDigest(slowDigest);

                clientMessageHandler.submitClientRegionMessage(clientId, file, region.getOffset(), region.getSize(), sum, slowDigest);
            }
        }
    }

    private int processByteBufferWrite(Region region, Hasher hasher, MappedByteBuffer mappedByteBuffer) {
        for (int i = 0; i < region.getSize(); i++) {
            byte b = 0;

            mappedByteBuffer.put(i, b);
            hasher.putByte(b);
        }
        return 0;
    }
}
