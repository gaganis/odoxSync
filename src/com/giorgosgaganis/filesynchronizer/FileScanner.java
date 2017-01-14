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

import com.giorgosgaganis.filesynchronizer.digest.Digester;
import com.giorgosgaganis.filesynchronizer.digest.LongDigester;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.giorgosgaganis.filesynchronizer.Contants.REGION_SIZE;

/**
 * Created by gaganis on 13/01/17.
 */
public class FileScanner {
    private static final Logger logger = Logger.getLogger(FileScanner.class.getName());


    private final Digester digester;
    
    private Path file;
    

    public FileScanner(Digester digester, Path file) {
        this.digester = digester;
        this.file = file;
    }

    public void scanFile() throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "r");

        long position = 0;
        FileChannel channel = randomAccessFile.getChannel();
        do {

            long regionSize =
                    position + REGION_SIZE > channel.size()
                            ? channel.size() - position
                            : REGION_SIZE;
            MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, position, regionSize);
            digester.digest(mappedByteBuffer);
            position += REGION_SIZE;
        } while (position < channel.size());
    }

   

    public static void main(String[] args) throws IOException {
//        FileScanner scanner = new FileScanner(Paths.get("/home/gaganis/Downloads/testfile"));
        FileScanner scanner = new FileScanner(new LongDigester(), Paths.get(args[0]));

        long start = System.currentTimeMillis();
        scanner.scanFile();
        System.out.println("System.currentTimeMillis() - start = " + (System.currentTimeMillis() - start));

//        scanner =  new FileScanner(new ShaDigester(), Paths.get(args[0]));
        start = System.currentTimeMillis();
        scanner.scanFile();
        System.out.println("System.currentTimeMillis() - start = " + (System.currentTimeMillis() - start));

        System.out.println("REGION_SIZE = " + REGION_SIZE);
    }
}
