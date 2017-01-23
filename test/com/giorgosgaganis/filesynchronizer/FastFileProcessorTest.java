package com.giorgosgaganis.filesynchronizer;

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

        assertThat(sample.size).isEqualTo(FastFileProcessor.SAMPLE_LENGTH);
        assertThat(sample.offset).isEqualTo(Contants.REGION_SIZE - FastFileProcessor.SAMPLE_LENGTH);
    }

    @Test
    public void sample_calculation_second() throws IOException {
        long key = Contants.REGION_SIZE ;
        BatchArea sample = getBatchArea(key);

        assertThat(sample.size).isEqualTo(FastFileProcessor.SAMPLE_LENGTH);
        assertThat(sample.offset).isEqualTo((Contants.REGION_SIZE * 2) - FastFileProcessor.SAMPLE_LENGTH);
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