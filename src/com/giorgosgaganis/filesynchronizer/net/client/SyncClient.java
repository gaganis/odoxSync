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
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class SyncClient {
    private static final Logger logger = Logger.getLogger(SyncClient.class.getName());

    private Client restClient = ClientBuilder.newClient();
    private int clientId = -1;

    private ClientRegionMessageHandler clientRegionMessageHandler = new ClientRegionMessageHandler(restClient);


    public static void main(String[] args) throws IOException {

        LoggingUtils.configureLogging();

        SyncClient syncClient = new SyncClient();
        syncClient.start();

//        System.out.println("files = " + files);
    }

    private void start() {

        logger.info("Starting sync client");
        clientId = getClientId();
        processFiles();
    }

    private void processFiles() {
        Collection<File> files = getFiles();

        files.stream().peek(file -> System.out.println(file.getName())).parallel().forEach(this::processFile);
    }

    private Collection<File> getFiles() {
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/files");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Collection<File> files = invocationBuilder.get(new GenericType<Collection<File>>() {
        });

        logger.info("Retrieved files from server");
        if (logger.isLoggable(Level.FINER)) {
            logger.finest("Files collection content " + files);
        }
        return files;
    }

    private int getClientId() {
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/introduction");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Integer id = invocationBuilder.get(Integer.class);
        logger.info("Retrieved clientId [" + id + "]");
        return id;
    }

    private void processFile(File file) {

        logger.fine("Processing file [" + file.getName() + "]");
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

                        clientRegionMessageHandler.submit(
                                createClientRegionMessage(file, region, sum, hasher));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ClientRegionMessage createClientRegionMessage(File file, Region region, long sum, Hasher hasher) {
        Region clientRegion = new Region(region.getOffset(), region.getSize());
        clientRegion.setQuickDigest(sum);
        clientRegion.setSlowDigest(hasher.hash().asBytes());
        ClientRegionMessage clientRegionMessage = new ClientRegionMessage();
        clientRegionMessage.setClientId(clientId);
        clientRegionMessage.setFileId(file.getId());
        clientRegionMessage.setRegion(clientRegion);
        return clientRegionMessage;
    }
}
