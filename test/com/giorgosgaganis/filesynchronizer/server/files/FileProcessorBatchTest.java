package com.giorgosgaganis.filesynchronizer.server.files;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.files.BatchArea;
import com.giorgosgaganis.filesynchronizer.files.processing.FileProcessor;
import com.giorgosgaganis.filesynchronizer.files.processing.SlowFileProcessor;
import com.giorgosgaganis.filesynchronizer.utils.Contants;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

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

    @Test
    public void batch_should_not_skip_on_first_scan() throws IOException {
        long fileSize = ((Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE) * 2) + 1;
        FileProcessor fp = getFileProcessorForFileSize(fileSize);


        BatchArea batchArea = fp.nextBatchArea();
        assertThat(batchArea.isSkip).isFalse();
    }

    @Test
    public void batch_should_skip_on_second_scan() throws IOException {
        long fileSize = ((Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE) * 2) + 1;
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        String fileName = "ubuntu-16.04.1-desktop-amd64.iso";
        File file = new File(fileName);
        updateAbsolutePath(file, workingDirectory, fileName);

        FileProcessor fileProcessor = getFileProcessor(fileSize, workingDirectory, file);
        BatchArea batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isFalse();

        fileProcessor.doBeforeBatchByteRead();
        fileProcessor.process(
                new byte[(int) (Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE)],
                batchArea);
        fileProcessor = getFileProcessor(fileSize, workingDirectory, file);
        batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isTrue();
        FileUtils.touch(file.getAbsolutePath().toFile());

    }

    @Test
    public void should_not_skip_after_touch() throws IOException, InterruptedException {
        long fileSize = ((Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE) * 2) + 1;
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        String fileName = "ubuntu-16.04.1-desktop-amd64.iso";
        File file = new File(fileName);
        updateAbsolutePath(file, workingDirectory, fileName);

        Supplier<FileProcessor> fileProcessorSupplier = () -> getFileProcessor(fileSize,workingDirectory,file);
        //1st pass
        testTouchSkip(file, fileProcessorSupplier, Contants.REGION_SIZE * SlowFileProcessor.BATCH_SIZE);
    }

    public static void testTouchSkip(File file, Supplier<FileProcessor> fileProcessorSupplier, long byteArraySize) throws IOException, InterruptedException {
        FileProcessor fileProcessor = fileProcessorSupplier.get();

        fileProcessor.doBeforeFileRead();
        BatchArea batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isFalse();

        fileProcessor.doBeforeBatchByteRead();
        fileProcessor.process(
                new byte[(int) byteArraySize],
                batchArea);

        //2nd pass
        fileProcessor = fileProcessorSupplier.get();
        fileProcessor.doBeforeFileRead();
        batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isTrue();

        Thread.sleep(1000);//The 1 second precision of touch makes the test flaky, hence the sleep to make the touch roll to the next second
        FileUtils.touch(file.getAbsolutePath().toFile());

        //3rd pass
        fileProcessor = fileProcessorSupplier.get();
        fileProcessor.doBeforeFileRead();
        batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isFalse();
    }

    public static void updateAbsolutePath(File file, String workingDirectory, String fileName) {
        file.setAbsolutePath(
                Paths.get(
                        workingDirectory,
                        fileName
                ).toAbsolutePath());
    }

    private static FileProcessor getFileProcessorForFileSize(long fileSize) {
        String workingDirectory = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
        String fileName = "ubuntu-16.04.1-desktop-amd64.iso";
        File file = new File(fileName);
        updateAbsolutePath(file, workingDirectory, fileName);

        return getFileProcessor(fileSize, workingDirectory, file);
    }

    private static FileProcessor getFileProcessor(long fileSize, String workingDirectory, File file) {

        RegionCalculator.calculateForSize(file, fileSize);

        return new SlowFileProcessor(
                (file1, currentRegion, batchLastModifiedTime, slowDigest) -> currentRegion.setSlowModifiedTime(batchLastModifiedTime), file);
    }
}