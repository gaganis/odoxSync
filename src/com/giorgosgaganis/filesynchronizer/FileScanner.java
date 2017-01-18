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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 18/01/17.
 */
public class FileScanner {
    private static final Logger logger = Logger.getLogger(FileScanner.class.getName());

    private final ConcurrentHashMap<Integer, File> files;
    private final DirectoryScanner ds;

    private String workingDirectory;

    private boolean firstScan = true;

    public FileScanner(ConcurrentHashMap<Integer, File> files) {
        this.files = files;
        ds = new DirectoryScanner(files);
    }

    void scanDirectoryAndFiles() {

        ds.scan(workingDirectory);

        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        try {
            forkJoinPool.submit(() ->
                    //parallel task here, for example
                    files.values().parallelStream().forEach(this::processFile)
            ).get();
            firstScan = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void processFile(File file) {
        try {

            boolean doScan = file.getRegions()
                    .values()
                    .stream()
                    .filter(Region::isDoSlowScan)
                    .findFirst()
                    .isPresent();

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
                scanFile(file);
                logger.info("Finished scan for [" + file.getName() + "]");
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void scanFile(File file) throws IOException {
        Path filePath = Paths.get(workingDirectory, file.getName());
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {
            for (Region region : file.getRegions().values()) {

                scanRegion(region, channel);
                if (region.isDoSlowScan()) {
                    String slowDigest = HashCode.fromBytes(region.getSlowDigest()).toString();
                    logger.info("Calculated slow digest[" + slowDigest + "] for file ["
                            + file.getName() + "]" + region.getOffset()
                            + ":" + (region.getOffset() + region.getSize()));
                }
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Calculated fast digest[" + region.getQuickDigest()
                            + "] for file [" + file.getName() + "]" + region.getOffset()
                            + ":" + (region.getOffset() + region.getSize()));


                }
            }
        }
    }

    private void scanRegion(Region region, FileChannel channel) throws IOException {
        MappedByteBuffer mappedByteBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                region.getOffset(),
                region.getSize());

        LongDigester longDigester = new LongDigester();
        region.setQuickDigest(
                longDigester.digest(mappedByteBuffer));


        if (region.isDoSlowScan()) {
            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY,
                    region.getOffset(),
                    region.getSize());

            ShaDigester shaDigester = new ShaDigester();
            region.setSlowDigest(
                    shaDigester.digest(mappedByteBuffer)
            );
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
