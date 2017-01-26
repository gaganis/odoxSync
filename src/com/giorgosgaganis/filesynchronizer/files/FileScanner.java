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
import com.giorgosgaganis.filesynchronizer.RegionCalculator;
import com.giorgosgaganis.filesynchronizer.files.processing.FastFileProcessorFactory;
import com.giorgosgaganis.filesynchronizer.files.processing.FileProcessor;
import com.giorgosgaganis.filesynchronizer.files.processing.FileProcessorFactory;
import com.giorgosgaganis.filesynchronizer.utils.Statistics;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Created by gaganis on 21/01/17.
 */
public class FileScanner {
    private static final Logger logger = Logger.getLogger(FileScanner.class.getName());

    private final String workingDirectory;
    private final FileProcessorFactory fileProcessorFactory;


    public FileScanner(String workingDirectory, FileProcessorFactory fileProcessorFactory) {
        this.workingDirectory = workingDirectory;
        this.fileProcessorFactory = fileProcessorFactory;
    }

    public void scanFile(File file) throws IOException {

        FileProcessor fileProcessor = fileProcessorFactory.create(file);

        Path filePath = Paths.get(workingDirectory, file.getName());
        fileProcessor.doBeforeFileRead();
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "rw");
                FileChannel channel = randomAccessFile.getChannel()
        ) {
            while (fileProcessor.hasNextBatchArea()) {
                BatchArea batchArea = fileProcessor.nextBatchArea();

                fileProcessor.doBeforeBatchByteRead();

                MappedByteBuffer mappedByteBuffer = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        batchArea.offset,
                        batchArea.size);
                byte[] buffer = new byte[mappedByteBuffer.remaining()];
                mappedByteBuffer.get(buffer);
                fileProcessor.process(buffer, batchArea);
            }
        }
    }

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();
        String workingDirectory = ".";
        File file = new File("testdata/source/ubuntu-16.04.1-desktop-amd64.iso");

        RegionCalculator rc = new RegionCalculator(workingDirectory, file);

        rc.calculate();
        FileScanner scanner = new FileScanner(workingDirectory,
                new FastFileProcessorFactory(new ConsolePrintingFastDigestHandler()));
        scanner.scanFile(file);
        long elapsedTime = System.currentTimeMillis() - start;
        System.out.println("System.currentTimeMillis() - start = " + elapsedTime);

        long bytesReadFast = Statistics.INSTANCE.bytesReadFast.get();
        System.out.println("bytesReadFast = " + bytesReadFast);
        String speed = Statistics.humanReadableByteCount(bytesReadFast / elapsedTime * 1000, false);

        System.out.println("speed = " + speed);
        System.out.println("Files.size(Paths.get(workingDirectory,file.getName())) = " + Files.size(Paths.get(workingDirectory, file.getName())));
    }
}
