package com.giorgosgaganis.filesynchronizer.files.processing;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.files.BatchArea;
import com.giorgosgaganis.filesynchronizer.files.FastDigestHandler;
import com.giorgosgaganis.filesynchronizer.server.files.FileProcessorBatchTest;
import com.giorgosgaganis.filesynchronizer.server.files.FileRegionHashMapDigestHandler;
import com.giorgosgaganis.filesynchronizer.utils.Contants;
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

        Supplier<FileProcessor> fileProcessorSupplier = () -> getFileProcessor(fileSize,workingDirectory,file);
        //1st pass
        FileProcessorBatchTest.testTouchSkip(file, fileProcessorSupplier, Contants.REGION_SIZE);
    }

    private static FileProcessor getFileProcessor(long fileSize, String workingDirectory, File file) {
        RegionCalculator rc = new RegionCalculator(workingDirectory, file);

        rc.calculateForSize(fileSize);

        return new FastFileProcessor(
                new FileRegionHashMapDigestHandler(),
                file);
    }
}