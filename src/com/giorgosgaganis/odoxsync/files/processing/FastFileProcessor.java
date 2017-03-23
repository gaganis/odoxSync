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
package com.giorgosgaganis.odoxsync.files.processing;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.Region;
import com.giorgosgaganis.odoxsync.files.BatchArea;
import com.giorgosgaganis.odoxsync.files.processing.handlers.FastDigestHandler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by gaganis on 20/01/17.
 */
public class FastFileProcessor implements FileProcessor {
    private static final Logger logger = Logger.getLogger(FastFileProcessor.class.getName());

    public static final int SAMPLE_SIZE = 0x1000;

    private final File file;
    private final ConcurrentHashMap<Long, Region> regions;

    private final LinkedList<Long> regionsToProcess;

    private final FastFileByteArrayHandler fileByteArrayHandler;
    private FileTime fileLastModifiedTime;

    public FastFileProcessor(FastDigestHandler fastDigestHandler, File file) {
        this.file = file;
        regions = file.getRegions();
        this.fileByteArrayHandler = new FastFileByteArrayHandler(fastDigestHandler);
        regionsToProcess = regions.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void process(byte[] buffer, BatchArea batchArea) {

        Region currentRegion = regions.get(batchArea.currentBatchRegions.remove());

        fileByteArrayHandler.handleBytes(buffer, file, currentRegion, fileLastModifiedTime);
    }

    @Override
    public boolean hasNextBatchArea() {
        return !regionsToProcess.isEmpty();
    }

    @Override
    public BatchArea nextBatchArea() {
        LinkedList<Long> currentBatchRegions = new LinkedList<>();

        Long regionOffset = regionsToProcess.remove();
        Region region = regions.get(regionOffset);

        return getSample(currentBatchRegions, regionOffset, region);
    }

    @Override
    public void doBeforeBatchByteRead() throws IOException {
    }

    @Override
    public void doBeforeFileRead(RandomAccessFile randomAccessFile) throws IOException {
        fileLastModifiedTime = Files.getLastModifiedTime(file.getAbsolutePath());
    }

    public BatchArea getSample(LinkedList<Long> currentBatchRegions, Long regionOffset, Region region) {
        long sampleSize = region.getSize() <= SAMPLE_SIZE ? region.getSize() : SAMPLE_SIZE;
        long offset = regionOffset + region.getSize() - sampleSize;

        currentBatchRegions.add(regionOffset);

        FileTime regionFastModifiedTime = region
                .getFastModifiedTime();

        boolean isSkip = regionFastModifiedTime != null
                && regionFastModifiedTime.compareTo(fileLastModifiedTime) >= 0;

        return new BatchArea(offset, sampleSize, currentBatchRegions, isSkip);
    }
}
