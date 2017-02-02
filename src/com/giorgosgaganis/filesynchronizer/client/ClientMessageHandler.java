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
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.client.net.RestClient;
import com.giorgosgaganis.filesynchronizer.messages.ClientFastDigestMessage;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class ClientMessageHandler {
    private static final Logger logger = Logger.getLogger(ClientMessageHandler.class.getName());

    public static final int BATCH_SIZE = 200;

    private final RestClient restClient;

    LinkedBlockingQueue<ClientFastDigestMessage> fastMessagesQueue = new LinkedBlockingQueue<>(BATCH_SIZE);

    public ClientMessageHandler(RestClient restClient) {
        this.restClient = restClient;
    }

    public void start() {
        new Thread(null, () -> {
            do {
                try {
                    ArrayList<ClientFastDigestMessage> batch = new ArrayList<>(BATCH_SIZE);
                    int drainedNo = fastMessagesQueue.drainTo(batch, BATCH_SIZE);
                    if(drainedNo == 0) {
                        Thread.sleep(1000);
                    } else {
                        restClient.postFastDigestMessageBatch(batch);
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }
            } while (true);
        }, "fast digest message handler").start();
    }

    private void submit(ClientRegionMessage clientRegionMessage) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ClientRegionMessager submited for processing ["
                    + clientRegionMessage + "]");
        }

        transferClientRegionMessage(clientRegionMessage);

    }

    private void transferClientRegionMessage(ClientRegionMessage clientRegionMessage) {
        restClient.postClientRegionMessage(clientRegionMessage);
    }

    public void submitFastDigest(int clientId, int fileId, long offset, Integer fastDigest) throws InterruptedException {
        ClientFastDigestMessage message = new ClientFastDigestMessage(clientId, fileId, offset, fastDigest);
        fastMessagesQueue.put(message);
    }

    public void submitClientRegionMessage(int clientId, File file, long offset, long size, Integer sum, byte[] slowDigest) {
        Region clientRegion = new Region(offset, size);
        clientRegion.setQuickDigest(sum);
        clientRegion.setSlowDigest(slowDigest);

        ClientRegionMessage clientRegionMessage = new ClientRegionMessage();
        clientRegionMessage.setClientId(clientId);
        clientRegionMessage.setFileId(file.getId());
        clientRegionMessage.setRegion(clientRegion);
        submit(clientRegionMessage);
    }
}
