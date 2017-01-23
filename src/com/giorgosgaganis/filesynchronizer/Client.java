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

import com.giorgosgaganis.filesynchronizer.server.candidates.TransferCandidate;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gaganis on 14/01/17.
 */
public class Client {
    public static final double OFFER_EXPIRATION_SECONDS = 5;
    private final int id;
    public LinkedBlockingQueue<TransferCandidate> transferCandidateQueue =
            new LinkedBlockingQueue<>();

    public CopyOnWriteArrayList<TransferCandidate> offeredTransferCandidates = new CopyOnWriteArrayList<>();

    public ConcurrentHashMap<Integer, File> files = new ConcurrentHashMap<>();


    public Client(int id) {
        this.id = id;
        removeExpiredOffers();
    }

    public ConcurrentHashMap<Integer, File> getFiles() {
        return files;
    }

    public void removeExpiredOffers() {
        new Thread(() -> {
            do {
                try {
                    Thread.sleep(1000);

                    long time = System.currentTimeMillis();
                    Iterator<TransferCandidate> iterator = offeredTransferCandidates.iterator();
                    while (iterator.hasNext()) {
                        TransferCandidate offer = iterator.next();
                        if (time < (offer.getOfferedTimeMillis() + OFFER_EXPIRATION_SECONDS * 1000)) {
                            iterator.remove();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (true);
        }).start();
    }

    public int getId() {
        return id;
    }
}