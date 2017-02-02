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
package com.giorgosgaganis.filesynchronizer.client;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.client.net.RestClient;
import com.giorgosgaganis.filesynchronizer.utils.LoggingUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class SyncClient {
    private static final Logger logger = Logger.getLogger(SyncClient.class.getName());

    private final String workingDirectory;
    private int clientId = -1;


    private final ConcurrentHashMap<Integer, File> allFiles = new ConcurrentHashMap<>();


    private final RestClient restClient;
    private final ClientMessageHandler clientMessageHandler;
    private final RegionDataHandler regionDataHandler;
    private final FileOperations fileOperations;

    public SyncClient(String hostPort, String workingDirectory) {
        this.workingDirectory = workingDirectory;
        restClient = new RestClient(hostPort);
        clientMessageHandler = new ClientMessageHandler(restClient);
        regionDataHandler = new RegionDataHandler(restClient, clientMessageHandler, allFiles);
        fileOperations = new FileOperations(clientMessageHandler, allFiles, restClient, workingDirectory);
    }


    public static void main(String[] args) throws IOException {

        String hostPort = args.length > 0 ? args[0] : "192.168.1.7:8081";
        String workingDirectory = args.length > 1 ? args[0] : ".";
        LoggingUtils.configureLogging();

        SyncClient syncClient = new SyncClient(hostPort, workingDirectory);
        syncClient.start();
    }

    private void start() {
        Path root = Paths.get(workingDirectory).toAbsolutePath().normalize();
        logger.info("Starting sync client at [" + root + "]");
        clientId = restClient.getClientId();

        restClient.setClientId(clientId);
        regionDataHandler.setClientId(clientId);
        regionDataHandler.start();

        fileOperations.setClientId(clientId);

        new Thread(() -> {
            do {
                fileOperations.processFiles();
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }).start();

        clientMessageHandler.start();
    }


}
