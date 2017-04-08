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
import com.giorgosgaganis.odoxsync.RegionCalculator;
import com.giorgosgaganis.odoxsync.files.BatchArea;
import com.giorgosgaganis.odoxsync.files.processing.handlers.FastDigestHandler;
import com.giorgosgaganis.odoxsync.server.files.FileProcessorBatchTest;
import com.giorgosgaganis.odoxsync.server.files.FileRegionHashMapDigestHandler;
import com.giorgosgaganis.odoxsync.utils.Contants;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by gaganis on 23/01/17.
 */
public class FastFileProcessorTest {

    @Test
    public void sample_calculation_first() throws IOException {
        long key = 0L;
        BatchArea sample = getBatchArea(key);

        assertThat(sample.size).isEqualTo(FastFileProcessor.SAMPLE_SIZE);
        assertThat(sample.offset).isEqualTo(Contants.REGION_SIZE - FastFileProcessor.SAMPLE_SIZE);
    }

    @Test
    public void sample_calculation_second() throws IOException {
        long key = Contants.REGION_SIZE;
        BatchArea sample = getBatchArea(key);

        assertThat(sample.size).isEqualTo(FastFileProcessor.SAMPLE_SIZE);
        assertThat(sample.offset).isEqualTo((Contants.REGION_SIZE * 2) - FastFileProcessor.SAMPLE_SIZE);
    }

    private BatchArea getBatchArea(long key) throws IOException {
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        File file = new File("ubuntu-16.04.1-desktop-amd64.iso");

        RegionCalculator rc = new RegionCalculator(workingDirectory, file);

        rc.calculate();
        FastDigestHandler fastDigestHandler = (buffer, file1, currentRegion, fastDigest, fileLastModifiedTime) -> {
        };
        FastFileProcessor fastFileProcessor = new FastFileProcessor(
                fastDigestHandler
                , file);

        Region region = file.getRegions().get(key);
        LinkedList<Long> currentBatchRegions = new LinkedList<>();
        currentBatchRegions.add(region.getOffset());
        return fastFileProcessor.getSample(currentBatchRegions, region.getOffset(), region);
    }

    @Test
    public void should_not_skip_after_touch() throws IOException, InterruptedException {
        long fileSize = ((Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE) * 2) + 1;
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        String fileName = "ubuntu-16.04.1-desktop-amd64.iso";
        File file = new File(fileName);
        FileProcessorBatchTest.updateAbsolutePath(file, workingDirectory, fileName);

        Supplier<FileProcessor> fileProcessorSupplier = () -> getFileProcessor(fileSize, workingDirectory, file);
        //1st pass
        FileProcessorBatchTest.testTouchSkip(file, fileProcessorSupplier, Contants.REGION_SIZE);
    }

    private static FileProcessor getFileProcessor(long fileSize, String workingDirectory, File file) {
        RegionCalculator.calculateForSize(file, fileSize);

        return new FastFileProcessor(
                new FileRegionHashMapDigestHandler(),
                file);
    }
}