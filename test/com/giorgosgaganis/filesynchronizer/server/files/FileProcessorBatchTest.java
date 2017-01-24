package com.giorgosgaganis.filesynchronizer.server.files;

import com.giorgosgaganis.filesynchronizer.files.processing.FileProcessor;
import com.giorgosgaganis.filesynchronizer.files.processing.SlowFileProcessor;
import com.giorgosgaganis.filesynchronizer.utils.Contants;
import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by gaganis on 21/01/17.
 */
public class FileProcessorBatchTest {

    @Test
    public void small_file_has_one_batcharea() throws IOException {
        long fileSize = Contants.REGION_SIZE;
        FileProcessor fp = getFileProcessorForFileSize(fileSize);

        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isFalse();
    }

    @Test
    public void file_with_exact_batchsize() throws IOException {
        long fileSize = Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE;
        FileProcessor fp = getFileProcessorForFileSize(fileSize);


        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isFalse();
    }

    @Test
    public void file_larger_than_batchsize() throws IOException {
        long fileSize = Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE + 1;
        FileProcessor fp = getFileProcessorForFileSize(fileSize);


        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isTrue();
        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isFalse();
    }

    @Test
    public void file_larger_than_2times_batchsize() throws IOException {
        long fileSize = ((Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE) * 2) + 1;
        FileProcessor fp = getFileProcessorForFileSize(fileSize);


        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isTrue();
        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isTrue();
        fp.nextBatchArea();
        assertThat(fp.hasNextBatchArea()).isFalse();
    }

    private static FileProcessor getFileProcessorForFileSize(long fileSize) {
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        File file = new File("ubuntu-16.04.1-desktop-amd64.iso");

        RegionCalculator rc = new RegionCalculator(workingDirectory, file);

        rc.calculateForSize(fileSize);

        return new SlowFileProcessor(
                file);
    }
}