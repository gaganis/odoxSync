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
import com.giorgosgaganis.filesynchronizer.Region;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by gaganis on 14/01/17.
 */
public class SyncClient {
    private Client client = ClientBuilder.newClient();
    private int clientId = -1;

    public static void main(String[] args) {

        SyncClient syncClient = new SyncClient();
        syncClient.start();

//        System.out.println("files = " + files);
    }

    private void start() {

        clientId = getClientId();
        processFiles();
    }

    private void processFiles() {
        Collection<File> files = getFiles();

        files.stream().peek(file -> System.out.println(file.getName())).parallel().forEach(SyncClient::processFile);
    }

    private Collection<File> getFiles() {
        WebTarget webTarget = client.target("http://localhost:8081/myapp/files");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        return invocationBuilder.get(new GenericType<Collection<File>>() {
        });
    }

    private int getClientId() {
        WebTarget webTarget = client.target("http://localhost:8081/myapp/introduction");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        return invocationBuilder.get(Integer.class);
    }

    private static void processFile(File file) {

        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path filePath = root.resolve(file.getName());

        try {
            if (Files.notExists(filePath)) {
                Path parent = filePath.getParent();
                Files.createDirectories(parent);

                try (
                        FileOutputStream fos = new FileOutputStream(filePath.toFile());
                        BufferedOutputStream bos = new BufferedOutputStream(fos)

                ) {
                    long counter = 0;
                    for (Region region : file.getRegions()) {
                        long sum = 0;
                        Hasher hasher = Hashing.sha256().newHasher();
                        for (long i = 0; i < region.getSize(); i++) {
                            byte b = (byte) counter;
                            bos.write(b);

                            hasher.putByte(b);
                            sum += b;

                            counter++;
                        }
                        Region clientRegion = new Region(region.getOffset(), region.getSize());
                        clientRegion.setQuickDigest(sum);
                        clientRegion.setSlowDigest(hasher.hash().asBytes());
                        ClientRegionMessage clientRegionMessage = new ClientRegionMessage();
                        clientRegionMessage.
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
