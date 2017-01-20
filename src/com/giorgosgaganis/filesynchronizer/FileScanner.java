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

import com.giorgosgaganis.filesynchronizer.utils.Statistics;
import com.google.common.base.Objects;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

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

import static com.giorgosgaganis.filesynchronizer.Contants.BYTE_SKIP_LENGHT;
import static com.giorgosgaganis.filesynchronizer.Contants.REGION_SIZE;

/**
 * Created by gaganis on 18/01/17.
 */
public class FileScanner {
    private static final Logger logger = Logger.getLogger(FileScanner.class.getName());

    private static Statistics statistics = Statistics.INSTANCE;
    private final ConcurrentHashMap<Integer, File> files;
    private final DirectoryScanner ds;
    private boolean isFast;

    private class RegionIdentifier {
        String filename;
        int offset;

        public RegionIdentifier(String filename, int offset) {
            this.filename = filename;
            this.offset = offset;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegionIdentifier that = (RegionIdentifier) o;
            return offset == that.offset &&
                    Objects.equal(filename, that.filename);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(filename, offset);
        }
    }


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
                long fastBytesAtStart = Statistics.INSTANCE.bytesReadFast.get();
                long startTime = System.currentTimeMillis();


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

                    long duration = System.currentTimeMillis() - startTime;
                    long speed = (Statistics.INSTANCE.bytesReadFast.get() - fastBytesAtStart ) * 1000 / duration;

                    logger.info("Finished ["+scanCount + "] scan in ["
                            + duration/1000 + "] at ["  +Statistics.humanReadableByteCount(speed, false) +"]");
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
                logger.fine("Starting scan for [" + file.getName() + "]");
                scanFile(file, true);
                logger.fine("Finished scan for [" + file.getName() + "]");
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan file [" + file.getName() + "]", e);
        }
    }

    private void processFileSlow(File file) {
        try {
            logger.fine("Starting scan for [" + file.getName() + "]");
            scanFile(file, false);
            logger.fine("Finished scan for [" + file.getName() + "]");

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

                if (isFast || !region.getSlowDigestsMap().containsKey(region.getQuickDigest())) {
                    scanRegion(file, region, channel, isFast);
                }
            }
        }
    }

    private void scanRegion(File file, Region region, FileChannel channel, boolean isFast) throws IOException {
        long offset = region.getOffset();
        long size = region.getSize();
        String fileName = file.getName();

        DigestResult digestResult = calculateDigestForFileArea(channel, isFast, offset, size, fileName);

        region.setQuickDigest(
                digestResult.quickDigest);
        if (digestResult.slowDigest != null) {
            region.getSlowDigestsMap()
                    .put(digestResult.quickDigest,
                            digestResult.slowDigest);
        }
    }

    public static DigestResult calculateDigestForFileArea(
            FileChannel channel, boolean isFast, long offset,
            long size, String fileName) throws IOException {
        MappedByteBuffer mappedByteBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                offset,
                size);

        Integer quickDigest = null;
        byte[] slowDigest = null;

        byte[] buffer = new byte[mappedByteBuffer.remaining()];
        mappedByteBuffer.get(buffer);
        int sum = 0;
        for (int i = 0; i < buffer.length; i += BYTE_SKIP_LENGHT) {
            byte b = buffer[i];
            sum += b;
        }
        quickDigest = sum;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Calculated fast digest[" + quickDigest
                    + "] for file [" + fileName + "]" + offset
                    + ":" + (offset + size));
        }

        if(isFast){
            statistics.bytesReadFast.addAndGet(buffer.length);
        } else {
            Hasher hasher = Hashing.sha256().newHasher();
            hasher.putBytes(buffer);
            slowDigest = hasher.hash().asBytes();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Calculated slow digest[" + mappedByteBuffer + "] for file ["
                        + fileName + "]" + offset
                        + ":" + (offset + size));
            }
            statistics.bytesReadSlow.addAndGet(buffer.length);
        }
        return new DigestResult(quickDigest, slowDigest);
    }


    private void reCalculateRegions(File file) throws IOException {
        RegionCalculator regionCalculator = new RegionCalculator(workingDirectory, file);
        regionCalculator.calculate();
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: filename offset isFast");
            System.exit(1);
        }
        String fileName = args[0];
        boolean isFast = "true".equals(args[1]);

        long start = System.currentTimeMillis();
        Path path = Paths.get(fileName);
        DigestResult digestResult;
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {

            long fileSize = Files.size(path);
            long position = 0;
            do {

                long regionSize =
                        position + REGION_SIZE > fileSize
                                ? fileSize - position
                                : REGION_SIZE;

                digestResult = calculateDigestForFileArea(channel, isFast, position, regionSize, fileName);
                System.out.println("fast = " + digestResult.quickDigest);
                byte[] slowDigest = digestResult.slowDigest;
                if (slowDigest != null && slowDigest.length == 0) {
                    System.out.println("slow = " + Hashing.sha256().hashBytes(slowDigest).toString());
                }

                position += REGION_SIZE;
            } while (position < fileSize);
        }
    }
}
