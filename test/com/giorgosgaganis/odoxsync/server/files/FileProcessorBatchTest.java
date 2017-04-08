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
package com.giorgosgaganis.odoxsync.server.files;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.RegionCalculator;
import com.giorgosgaganis.odoxsync.files.BatchArea;
import com.giorgosgaganis.odoxsync.files.processing.FileProcessor;
import com.giorgosgaganis.odoxsync.files.processing.SlowFileProcessor;
import com.giorgosgaganis.odoxsync.utils.Contants;
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

        fileProcessor.doBeforeFileRead(null);
        BatchArea batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isFalse();

        fileProcessor.doBeforeBatchByteRead();
        fileProcessor.process(
                new byte[(int) byteArraySize],
                batchArea);

        //2nd pass
        fileProcessor = fileProcessorSupplier.get();
        fileProcessor.doBeforeFileRead(null);
        batchArea = fileProcessor.nextBatchArea();
        assertThat(batchArea.isSkip).isTrue();

        Thread.sleep(1000);//The 1 second precision of touch makes the test flaky, hence the sleep to make the touch roll to the next second
        FileUtils.touch(file.getAbsolutePath().toFile());

        //3rd pass
        fileProcessor = fileProcessorSupplier.get();
        fileProcessor.doBeforeFileRead(null);
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