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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 18/01/17.
 */
public class FileScanner {
    private static final Logger logger = Logger.getLogger(FileScanner.class.getName());

    private final ConcurrentHashMap<Integer, File> files;
    private final DirectoryScanner ds;
    private boolean isFast;

    private String workingDirectory;

    private volatile int scanCount = 0;

    public FileScanner(ConcurrentHashMap<Integer, File> files, boolean isFast) {
        this.files = files;
        ds = new DirectoryScanner(files);
        this.isFast = isFast;
    }

    void scanDirectoryAndFiles() {
        new Thread(() -> {
            do {
                ds.scan(workingDirectory);

                ForkJoinPool forkJoinPool = new ForkJoinPool(2);
                try {
                    forkJoinPool.submit(() -> {
                                if (isFast) {
                                    files.values().parallelStream().forEach(this::processFileFast);
                                } else {
                                    files.values().parallelStream().forEach(this::processFileSlow);
                                }
                            }
                    ).get();
                    scanCount++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            } while (true);
        }).start();
    }

    private void processFileFast(File file) {
        try {

            boolean doScan = false;

            Path path = Paths.get(workingDirectory, file.getName());
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);

            if (lastModifiedTime.compareTo(file.getLastModified()) == 0) {
                logger.fine(" File has not been modified [" + file.getName() + "]");
            } else {
                doScan = true;
            }
            file.setLastModified(lastModifiedTime);

            long size = Files.size(path);
            if (size != file.getSize()) {
                logger.fine("Recalculation regions for [" + file.getName() + "]");
                reCalculateRegions(file);
                doScan = true;
            }

            if (doScan) {
                logger.info("Starting scan for [" + file.getName() + "]");
                scanFile(file, true);
                logger.info("Finished scan for [" + file.getName() + "]");
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void processFileSlow(File file) {
        try {
            logger.info("Starting scan for [" + file.getName() + "]");
            scanFile(file, false);
            logger.info("Finished scan for [" + file.getName() + "]");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void scanFile(File file, boolean isFast) throws IOException {
        Path filePath = Paths.get(workingDirectory, file.getName());
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {
            for (Region region : file.getRegions().values()) {

                if(isFast || !region.getSlowDigestsMap().containsKey(region.getQuickDigest())) {
                    scanRegion(file, region, channel, isFast);
                }
            }
        }
    }

    private void scanRegion(File file, Region region, FileChannel channel, boolean isFast) throws IOException {
        MappedByteBuffer mappedByteBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                region.getOffset(),
                region.getSize());

        if (isFast) {
            LongDigester longDigester = new LongDigester();
            region.setQuickDigest(
                    longDigester.digest(mappedByteBuffer));
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Calculated fast digest[" + region.getQuickDigest()
                        + "] for file [" + file.getName() + "]" + region.getOffset()
                        + ":" + (region.getOffset() + region.getSize()));
            }

        } else {
            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY,
                    region.getOffset(),
                    region.getSize());

            ShaDigester shaDigester = new ShaDigester();
            shaDigester.digest(mappedByteBuffer);

            region.getSlowDigestsMap()
                    .put(shaDigester.getFastDigest(),
                            shaDigester.getSlowDigest());

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Calculated slow digest[" + mappedByteBuffer + "] for file ["
                        + file.getName() + "]" + region.getOffset()
                        + ":" + (region.getOffset() + region.getSize()));
            }
        }

    }

    private void reCalculateRegions(File file) throws IOException {
        RegionCalculator regionCalculator = new RegionCalculator(workingDirectory, file);
        regionCalculator.calculate();
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
