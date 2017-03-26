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
                new SlowFileProcessorFactory(countingWrapper), () -> {});
        scanner.scanFile(file);
        int firstPassCount = countingWrapper.getCount();

        scanner = new FileScanner(workingDirectory,
                new SlowFileProcessorFactory(countingWrapper), () -> {});
        scanner.scanFile(file);

        assertThat(countingWrapper.getCount()).isEqualTo(firstPassCount);


    }
}
