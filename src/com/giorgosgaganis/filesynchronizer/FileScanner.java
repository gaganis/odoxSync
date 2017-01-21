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
package com.giorgosgaganis.filesynchronizer;

import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.giorgosgaganis.filesynchronizer.Contants.REGION_SIZE;

/**
 * Created by gaganis on 21/01/17.
 */
public class FileScanner {
    void scanFile(File file, boolean isFast) throws IOException {
        Path filePath = Paths.get(workingDirectory, file.getName());

        FileProcessor fileProcessor = new FileProcessor(file);
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {
            while(fileProcessor.hasNextArea()) {

                FileArea fileArea = fileProcessor.nextArea();

                MappedByteBuffer mappedByteBuffer = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileArea.offset,
                        fileArea.size);
                byte[] buffer = new byte[mappedByteBuffer.remaining()];
                mappedByteBuffer.get(buffer);
                fileProcessor.process(buffer);
            }
        }
    }

    private void scanRegion(File file, Region region, FileChannel channel, boolean isFast) throws IOException {
        long offset = region.getOffset();
        long size = region.getSize();
        String fileName = file.getName();

        DigestResult digestResult = calculateDigestForFileArea(channel, isFast, offset, size, fileName);

        region.setQuickDigest(
                digestResult.quickDigest);
        if (digestResult.slowDigest != null) {
            region.getSlowDigestsMap()
                    .put(digestResult.quickDigest,
                            digestResult.slowDigest);
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: filename offset isFast");
            System.exit(1);
        }
        String fileName = args[0];
        boolean isFast = "true".equals(args[1]);

        long start = System.currentTimeMillis();
        Path path = Paths.get(fileName);
        DigestResult digestResult;
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r");
                FileChannel channel = randomAccessFile.getChannel()
        ) {

            long fileSize = Files.size(path);
            long position = 0;
            do {

                long regionSize =
                        position + REGION_SIZE > fileSize
                                ? fileSize - position
                                : REGION_SIZE;

                digestResult = calculateDigestForFileArea(channel, isFast, position, regionSize, fileName);
                System.out.println("fast = " + digestResult.quickDigest);
                byte[] slowDigest = digestResult.slowDigest;
                if (slowDigest != null && slowDigest.length == 0) {
                    System.out.println("slow = " + Hashing.sha256().hashBytes(slowDigest).toString());
                }

                position += REGION_SIZE;
            } while (position < fileSize);
        }
    }
}
