package com.giorgosgaganis.directorysynchronizer.integration;

import com.giorgosgaganis.odoxsync.client.SyncClient;
import com.giorgosgaganis.odoxsync.server.net.MainServer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by gaganis on 14/01/17.
 */
public class SimpleTest {

    public static final String SOURCE_DIR = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/source";
    public static final String TARGET_DIR = "/home/gaganis/IdeaProjects/DirectorySynchronizer/testdata/target";

    public static void main(String[] args) throws IOException, InterruptedException {



        new Thread(() -> {
            try {
                MainServer.main(new String[]{SOURCE_DIR});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        Thread.sleep(2000);

        deleteTargetDir();

        new Thread(() -> {
            try {
                SyncClient.main(new String[]{TARGET_DIR});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(2000);

        System.out.println("result = " + compareSourceTarget());
    }

    public static void deleteTargetDir() throws IOException {
        Files.walkFileTree(
                Paths.get(TARGET_DIR), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if(Files.isSameFile(Paths.get(TARGET_DIR), dir)){
                            return FileVisitResult.CONTINUE;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    private static boolean compareSourceTarget() throws IOException {
        Path sourceRoot = Paths.get(SOURCE_DIR);
        Path targetRoot = Paths.get(TARGET_DIR);

        Optional<Boolean> failed = Files.walk(sourceRoot).filter(Files::isRegularFile).map(path -> {
            Path relative = sourceRoot.relativize(path);
            Path target = targetRoot.resolve(relative);
            try {
                byte[] file1 = Files.readAllBytes(path);
                byte[] file2 = Files.readAllBytes(target);
                if (file1.length != file2.length) {
                    System.out.println("Different size");
                    return false;
                }
                for (int i = 0; i < file1.length; i++) {
                    if (file1[i] != file2[i]) {
                        System.out.println("Different Content");
                        return false;
                    }
                }
            } catch (IOException e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                return false;
            }
            return true;
        }).filter(Predicate.isEqual(false)).findFirst();
        return !failed.isPresent();
    }
}
