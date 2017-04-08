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
package com.giorgosgaganis.odoxsync.files.processing;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.files.BatchArea;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by gaganis on 23/01/17.
 */
public interface FileProcessor {
    File getFile();

    void process(byte[] buffer, BatchArea batchArea);

    boolean hasNextBatchArea();

    BatchArea nextBatchArea() throws IOException;

    void doBeforeBatchByteRead() throws IOException;

    void doBeforeFileRead(RandomAccessFile randomAccessFile) throws IOException;
}