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
package com.giorgosgaganis.odoxsync.server;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.RegionCalculator;
import com.giorgosgaganis.odoxsync.files.FileScanner;
import com.giorgosgaganis.odoxsync.files.processing.FastFileProcessorFactory;
import com.giorgosgaganis.odoxsync.files.processing.SlowFileProcessorFactory;
import com.giorgosgaganis.odoxsync.files.processing.handlers.SlowDigestHandler;
import com.giorgosgaganis.odoxsync.server.files.FileRegionHashMapDigestHandler;
import com.giorgosgaganis.odoxsync.server.files.HashMapSlowDigestHandler;
import com.giorgosgaganis.odoxsync.utils.Statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 18/01/17.
 */
public class DirectoryScanner {
    private static final Logger logger = Logger.getLogger(DirectoryScanner.class.getName());

    private final ConcurrentHashMap<Integer, File> files;
    private boolean isFast;

    private final AtomicInteger fileIdCounter = new AtomicInteger(1);

    private String workingDirectory;

    private volatile int scanCount = 0;
    private final ActivityStaler activityStaler;

    public DirectoryScanner(ConcurrentHashMap<Integer, File> files, boolean isFast, ActivityStaler activityStaler) {
        this.files = files;
        this.isFast = isFast;
        this.activityStaler = activityStaler;
    }

    private void scan(String workingDirectory) {

        try {
            Path root = Paths.get(workingDirectory)
                    .toAbsolutePath()
                    .normalize();

            logger.fine("Starting directory scan in [" + root + "]");

            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .map(Path::normalize)
                    .map(path -> root.relativize(path))
                    .forEach((path) -> {
                        String name = path.toString();
                        File file = new File(name);
                        if (!files.containsValue(file)) {
                            int id = fileIdCounter.getAndIncrement();
                            file.setId(id);

                            file.setAbsolutePath(
                                    Paths.get(
                                            workingDirectory,
                                            name
                                    ).toAbsolutePath());
                            RegionCalculator rc = new RegionCalculator(workingDirectory, file);
                            try {
                                rc.calculate();
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "Error while getting size of file", e);
                            }

                            files.put(id, file);
                            logger.fine("Added new tracked file " + id + ":[" + name + "]");
                        } else {
                            logger.finer("File is already tracked [" + name + "]");
                        }
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while scanning directory", e);
        }
        logger.fine("Finished directory scan");
    }

    void scanDirectoryAndFiles() {
        new Thread(() -> {
            do {
                scan(workingDirectory);
                long fastBytesAtStart = Statistics.INSTANCE.bytesReadFast.get();
                long startTime = System.currentTimeMillis();

                try {
                    if (isFast) {
                        files.values()
                                .stream()
                                .sorted(new FileModifiedComparator().reversed())
                                .forEach(this::processFileFast);
                    } else {
                        files.values()
                                .parallelStream()
                                .sorted(new FileModifiedComparator().reversed())
                                .forEach(this::processFileSlow);
                    }
                    scanCount++;

                    long duration = System.currentTimeMillis() - startTime;
                    long speed =
                            duration == 0 ?
                                    -1 :
                                    (Statistics.INSTANCE.bytesReadFast.get() - fastBytesAtStart) * 1000 / duration;

                    String scanType = isFast ? "fast" : "slow";
                    logger.info("Finished [" + scanCount + "] " + scanType + " scan in ["
                            + duration / 1000 + "] at [" + Statistics.humanReadableByteCount(speed, false) + "]");
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }).start();
    }

    private void processFileFast(File file) {
        try {

            boolean doScan = false;

            Path path = Paths.get(workingDirectory, file.getName());

            if(!Files.exists(path)) {
                return;
            }

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
                long startTime = System.currentTimeMillis();
                logger.fine("Starting scan for [" + file.getName() + "]");
                FileScanner fileScanner = new FileScanner(workingDirectory,
                        new FastFileProcessorFactory(new FileRegionHashMapDigestHandler()), activityStaler);
                fileScanner.scanFile(file);
                long duration = System.currentTimeMillis() - startTime;
                logger.fine("Finished scan for [" + file.getName() + "] in [" + duration + "ms]");
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void processFileSlow(File file) {
        try {
            logger.fine("Starting scan for [" + file.getName() + "]");

            SlowDigestHandler slowDigestHandler = new HashMapSlowDigestHandler();
            FileScanner fileScanner = new FileScanner(workingDirectory, new SlowFileProcessorFactory(slowDigestHandler), activityStaler);
            fileScanner.scanFile(file);
            logger.fine("Finished scan for [" + file.getName() + "]");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
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
