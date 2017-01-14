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

import com.giorgosgaganis.filesynchronizer.DirectorySynchronizer;

import javax.ws.rs.client.Client;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class ClientRegionMessageHandler {
    private static final Logger logger = Logger.getLogger(DirectorySynchronizer.class.getName());

    public static final int MAX_REGION_THREADS = 4;

    private ExecutorService executorService = Executors.newFixedThreadPool(MAX_REGION_THREADS);

    private final RestClient restClient;

    public ClientRegionMessageHandler(RestClient restClient) {
        this.restClient = restClient;
    }

    public void submit(ClientRegionMessage clientRegionMessage) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ClientRegionMessager submited for processing ["
                    + clientRegionMessage + "]");
        }

        executorService.submit(()->transferClientRegionMessage(clientRegionMessage));

    }

    public void transferClientRegionMessage(ClientRegionMessage clientRegionMessage) {
        restClient.postClientRegionMessage(clientRegionMessage);
    }
}
