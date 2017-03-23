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
package com.giorgosgaganis.odoxsync.utils.ochillatingqueue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gaganis on 30/01/17.
 */
public class OscillatingQueueWrapper<T> {

    private final int minShrinkLimit;
    private final int maxGrowLimit;

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedBlockingQueue<T> backingQueue = new LinkedBlockingQueue<>();

    private volatile boolean growing = true;

    public OscillatingQueueWrapper(int minShrinkLimit, int maxGrowLimit) {
        this.minShrinkLimit = minShrinkLimit;
        this.maxGrowLimit = maxGrowLimit;
    }

    public LinkedBlockingQueue<T> getBackingQueue() {
        return backingQueue;
    }

    public void put(T e) throws InterruptedException {
        boolean wait;

        lock.lock();
        try {
            wait = updateGrowingStatus();
        } finally {
            lock.unlock();
        }

        if (wait) {
            waitForGrowingAndPut(e);
        } else {
            backingQueue.put(e);
        }
    }

    private boolean updateGrowingStatus() {
        boolean wait = false;
        if (growing) {
            if (backingQueue.size() >= maxGrowLimit) {
                growing = false;
                wait = true;
            }
        } else {
            if (backingQueue.size() <= minShrinkLimit) {
                growing = true;
            } else {
                wait = true;
            }
        }
        return wait;
    }

    private void waitForGrowingAndPut(T e) throws InterruptedException {
        waitWhileShrinking();
        backingQueue.put(e);
    }

    public void waitWhileShrinking() {
        lock.lock();
        try {
            if (growing) {
                return;
            }
        } finally {
            lock.unlock();
        }

        boolean waiting = true;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            lock.lock();
            try {
                if (growing) {
                    waiting = false;
                }
            } finally {
                lock.unlock();
            }
        } while (waiting);
    }

    boolean isGrowing() {
        lock.lock();
        try {
            return growing;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(T transferCandidate) {
        return backingQueue.contains(transferCandidate);
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try{
            updateGrowingStatus();
        } finally {
            lock.unlock();
        }
        return backingQueue.poll(timeout, unit);
    }
}
