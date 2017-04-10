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
package com.giorgosgaganis.odoxsync.files;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.Region;
import com.giorgosgaganis.odoxsync.RegionCalculator;
import com.giorgosgaganis.odoxsync.files.processing.SlowFileProcessorFactory;
import com.giorgosgaganis.odoxsync.files.processing.handlers.SlowDigestHandler;
import com.giorgosgaganis.odoxsync.server.files.HashMapSlowDigestHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Created by gaganis on 03/02/17.
 */
public class FileScannerSlowModificationTest {

    private class CountingWrapper implements SlowDigestHandler {
        private final SlowDigestHandler wrapped;

        private int count = 0;

        private CountingWrapper(SlowDigestHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void handleSlowDigest(File file, Region currentRegion, FileTime batchLastModifiedTime, byte[] slowDigest) {
            wrapped.handleSlowDigest(file, currentRegion, batchLastModifiedTime, slowDigest);
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    @Test
    public void modification_is_skipping() throws IOException {
        String workingDirectory = ".";
        String name = "spotless.license.java";
        File file = new File(name);
        file.setAbsolutePath(Paths.get(name).toAbsolutePath());

        RegionCalculator rc = new RegionCalculator(workingDirectory, file);

        rc.calculate();
        CountingWrapper countingWrapper = new CountingWrapper(new HashMapSlowDigestHandler());
        FileScanner scanner = new FileScanner(workingDirectory,
                new SlowFileProcessorFactory(countingWrapper), () -> {}, false);
        scanner.scanFile(file);
        int firstPassCount = countingWrapper.getCount();

        scanner = new FileScanner(workingDirectory,
                new SlowFileProcessorFactory(countingWrapper), () -> {}, false);
        scanner.scanFile(file);

        assertThat(countingWrapper.getCount()).isEqualTo(firstPassCount);


    }
}
