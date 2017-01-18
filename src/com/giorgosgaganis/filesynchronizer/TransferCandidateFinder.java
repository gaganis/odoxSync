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

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by gaganis on 15/01/17.
 */
public class TransferCandidateFinder {
    private static final Logger logger = Logger.getLogger(TransferCandidateFinder.class.getName());
    public static final int OFFER_EXPIRY_SECONDS = 30;

    private final ConcurrentHashMap<Integer, File> files;
    private final ConcurrentHashMap<Integer, Client> clients;

    public TransferCandidateFinder(ConcurrentHashMap<Integer, File> files,
                                   ConcurrentHashMap<Integer, Client> clients) {
        this.files = files;
        this.clients = clients;
    }

    public void lookForRegionsToTransfer() {
        new Thread(() -> {
            do {
                for (Integer clientId : clients.keySet()) {
                    logger.finer("Looking candidates for client [" + clientId + "]");
                    lookAtClient(clientId);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }).start();
    }

    private void lookAtClient(Integer clientId) {
        Client client = clients.get(clientId);
        if (client != null) {
            for (Integer fileId : client.files.keySet()) {
                logger.finer("Looking candidates for client ["
                        + clientId + "] and file [" + fileId + "]");
                lookAtFile(client, clientId, fileId);
            }
        }
    }

    private void lookAtFile(Client client, Integer clientId, Integer fileId) {
        File clientFile = client.files.get(fileId);
        if (clientFile != null) {
            int allCount = 0;
            int toTransferCount = 0;

            for (Long offset : clientFile.getRegions().keySet()
                    .stream()
                    .sorted()
                    .collect(Collectors.toList())) {
                logger.finer("Looking candidates for client ["
                        + clientId + "] and file ["
                        + fileId + "] and region " + offset + "]");
                boolean doTranser = lookAtRegion(clientFile, client, fileId, offset);

                if (doTranser) {
                    toTransferCount++;
                }
                allCount++;
            }

            if (allCount > 0) {
                int syncedPercentage = (allCount - toTransferCount) * 100 / allCount;
                clientFile.setSyncedPercentage(syncedPercentage);
            }

        }
    }

    private boolean lookAtRegion(File clientFile, Client client, Integer fileId, Long offset) {
        boolean doTransfer = false;

        Region clientRegion = clientFile.getRegions().get(offset);
        if (clientRegion != null) {
            Region serverRegion = files.get(fileId).getRegions().get(offset);

            if (serverRegion == null) {
                return doTransfer;
            }

            long clientQuickDigest = clientRegion.getQuickDigest();
            long serverQuickDigest = serverRegion.getQuickDigest();

            if (clientQuickDigest != serverQuickDigest) {
                doTransfer = true;
            } else {
                byte[] bytes = serverRegion.getSlowDigestsMap().get(serverQuickDigest);
                if (bytes != null) {
                    for (int i = 0; i < bytes.length; i++) {
                        if (clientRegion.getSlowDigest()[i] != bytes[i]) {
                            doTransfer = true;
                            logger.info("Collision detected");
                        }
                    }
                }
            }

            if (doTransfer) {
                TransferCandidate transferCandidate = new TransferCandidate(fileId, offset, serverRegion.getSize());
                try {

                    //TODO this is contains a race since is is a unsynchronized compare and set idiom
                    removeFromOfferedIfExpired(client, transferCandidate);

                    if (!client.transferCandidateQueue.contains(transferCandidate)
                            && !client.offeredTransferCandidates.contains(transferCandidate)) {
                        client.transferCandidateQueue.put(transferCandidate);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return doTransfer;
    }

    private void removeFromOfferedIfExpired(Client client, TransferCandidate transferCandidate) {
        int index = client.offeredTransferCandidates.indexOf(transferCandidate);

        int expiryDelay = OFFER_EXPIRY_SECONDS * 1000;
        if (index >= 0 && (client.offeredTransferCandidates.get(index).getOfferedTimeMillis() + expiryDelay) < System.currentTimeMillis()) {
            client.offeredTransferCandidates.remove(index);
        }
    }
}
