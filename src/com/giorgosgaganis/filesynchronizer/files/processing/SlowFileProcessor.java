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
package com.giorgosgaganis.filesynchronizer.files.processing;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.files.BatchArea;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by gaganis on 20/01/17.
 */
public class SlowFileProcessor implements FileProcessor {
    private static final Logger logger = Logger.getLogger(SlowFileProcessor.class.getName());

    public static final int BATCH_SIZE = 32;

    private static Statistics statistics = Statistics.INSTANCE;
    private final File file;
    private final ConcurrentHashMap<Long, Region> regions;

    private final LinkedList<Long> regionsToProcess;

    private final LinkedList<Long> currentBatchRegions = new LinkedList<>();

    private final SlowFileByteArrayHandler fileByteArrayHandler = new SlowFileByteArrayHandler();
    private FileTime batchLastModifiedTime;

    public SlowFileProcessor(File file) {
        this.file = file;
        regions = file.getRegions();
        regionsToProcess = regions.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public void process(byte[] buffer, BatchArea batchArea) {

        for (Long currentBatchRegionOffset : currentBatchRegions) {

            Region currentRegion = regions.get(currentBatchRegionOffset);

            fileByteArrayHandler.handleBytes(buffer, file, batchArea.offset, currentRegion, batchLastModifiedTime);
        }
    }

    @Override
    public boolean hasNextBatchArea() {
        return !regionsToProcess.isEmpty();
    }

    @Override
    public BatchArea nextBatchArea() throws IOException {
        currentBatchRegions.clear();
        batchLastModifiedTime = Files.getLastModifiedTime(file.getAbsolutePath());

        Long firstRegionOffset = regionsToProcess.remove();
        long size = regions.get(firstRegionOffset).getSize();
        currentBatchRegions.add(firstRegionOffset);

        boolean isSkip = true;
        for (int i = 1; i < BATCH_SIZE && !regionsToProcess.isEmpty(); i++) {
            Long regionOffset = regionsToProcess.remove();
            Region region = regions.get(regionOffset);
            size += region.getSize();


            FileTime regionSlowModifiedTime = region
                    .getSlowModifiedTime();
            if(regionSlowModifiedTime == null
                    || regionSlowModifiedTime.compareTo(batchLastModifiedTime) == -1) {
                isSkip = false;
            }
            currentBatchRegions.add(regionOffset);
        }
        return new BatchArea(firstRegionOffset, size, currentBatchRegions, isSkip);
    }

    @Override
    public void doBeforeBatchByteRead() throws IOException {

    }

    @Override
    public void doBeforeFileRead() throws IOException {
    }
}
