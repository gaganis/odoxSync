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
package com.giorgosgaganis.filesynchronizer.net.client;

import com.giorgosgaganis.filesynchronizer.File;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gaganis on 15/01/17.
 */
public class RegionDataHandler extends Thread {

    private RestClient restClient;
    private ConcurrentHashMap<Integer, File> files;

    private ExecutorService service = Executors.newFixedThreadPool(4);

    public RegionDataHandler(RestClient restClient, ConcurrentHashMap<Integer, File> files) {
        this.restClient = restClient;
        this.files = files;
    }

    @Override
    public void run() {
        do {
            service.submit(() -> {
                try {
                    RegionDataParams regionData = restClient.getRegionData();

                    File file = files.get(regionData.fileId);
                    Path absolutePath = file.getAbsolutePath();

                    try (
                            RandomAccessFile randomAccessFile = new RandomAccessFile(absolutePath.toFile(), "rw");
                            FileChannel channel = randomAccessFile.getChannel();
                            InputStream inputStream = regionData.inputStream;
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(regionData.inputStream)
                    ) {
                        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, regionData.offset, regionData.size);
                        while(mappedByteBuffer.hasRemaining() && bufferedInputStream.available() >0) {
                            byte b = (byte) bufferedInputStream.read();
                            mappedByteBuffer.put(b);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } while (true);
    }
}
