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
package com.giorgosgaganis.filesynchronizer.files;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.utils.Contants;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;

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
        FastFileProcessor fastFileProcessor = new FastFileProcessor(new FastFileByteArrayHandler((buffer, file1, currentRegion, fastDigest) -> {

        }), file);

        Region region = file.getRegions().get(key);
        LinkedList<Long> currentBatchRegions = new LinkedList<>();
        currentBatchRegions.add(region.getOffset());
        return fastFileProcessor.getSample(currentBatchRegions, region.getOffset(), region);
    }
}