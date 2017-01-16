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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by gaganis on 15/01/17.
 */
public class TransferCandidateFinder {
    private static final Logger logger = Logger.getLogger(TransferCandidateFinder.class.getName());

    private final ConcurrentHashMap<Integer, File> files;
    private final ConcurrentHashMap<Integer, Client> clients;
    private final LinkedBlockingQueue<TransferCandidate> transferCandidateQueue;
    private CopyOnWriteArrayList<TransferCandidate> offeredTransferCandidates;

    public TransferCandidateFinder(ConcurrentHashMap<Integer, File> files,
                                   ConcurrentHashMap<Integer, Client> clients,
                                   LinkedBlockingQueue<TransferCandidate> transferCandidateQueue,
                                   CopyOnWriteArrayList<TransferCandidate> offeredTransferCandidates) {
        this.files = files;
        this.clients = clients;
        this.transferCandidateQueue = transferCandidateQueue;
        this.offeredTransferCandidates = offeredTransferCandidates;
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
            for (Long offset : clientFile.getRegions().keySet()
                    .stream()
                    .sorted()
                    .collect(Collectors.toList())) {
                logger.finer("Looking candidates for client ["
                        + clientId + "] and file ["
                        + fileId + "] and region " + offset + "]");
                lookAtRegion(clientFile, clientId, fileId, offset);
            }
        }
    }

    private void lookAtRegion(File clientFile, Integer clientId, Integer fileId, Long offset) {
        Region clintRegion = clientFile.getRegions().get(offset);
        if (clintRegion != null) {
            Region serverRegion = files.get(fileId).getRegions().get(offset);

            if (serverRegion == null) {
                return;
            }

            boolean doTransfer = false;
            if (clintRegion.getQuickDigest() != serverRegion.getQuickDigest()) {
                doTransfer = true;
            } else {
                for (int i = 0; i < clintRegion.getSlowDigest().length; i++) {
                    if (clintRegion.getSlowDigest()[i] != serverRegion.getSlowDigest()[i]) {
                        doTransfer = true;
                        logger.info("Colistion detected");
                    }
                }
            }

            if (doTransfer) {
                TransferCandidate transferCandidate = new TransferCandidate(fileId, offset, serverRegion.getSize());
                try {
                    //TODO Transfer candidates should be added to per client queues
                    if (!transferCandidateQueue.contains(transferCandidate)
                            && !offeredTransferCandidates.contains(transferCandidate)) {
                        transferCandidateQueue.put(transferCandidate);
                        offeredTransferCandidates.add(transferCandidate);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
