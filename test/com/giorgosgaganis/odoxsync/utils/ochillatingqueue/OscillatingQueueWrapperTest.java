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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by gaganis on 29/01/17.
 */
public class OscillatingQueueWrapperTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(1);

    @Test
    public void test_stop_growing() throws Exception {
        int maxGrowLimit = 20;
        OscillatingQueueWrapper<Integer> oscillatingQueueWrapper = new OscillatingQueueWrapper<>(5, maxGrowLimit);

        for (int i = 0; i < maxGrowLimit; i++) {
            oscillatingQueueWrapper.put(i);
        }

        Thread blocked = new Thread(() -> {
            try {
                oscillatingQueueWrapper.put(1);
            } catch (InterruptedException e) {
            }
        });
        blocked.start();
        //Make sure the above thread has run.
        while (blocked.getState() == Thread.State.RUNNABLE);
        assertThat(oscillatingQueueWrapper.isGrowing()).isFalse();
    }

    @Test
    public void test_stop_shrinking() throws Exception {
        int maxGrowLimit = 20;
        OscillatingQueueWrapper<Integer> oscillatingQueueWrapper = new OscillatingQueueWrapper<>(5, maxGrowLimit);

        for (int i = 0; i < maxGrowLimit; i++) {
            oscillatingQueueWrapper.put(i);
        }

        Thread toSleepThread = new Thread(() -> {
            try {
                oscillatingQueueWrapper.put(1);
            } catch (InterruptedException e) {
            }
        });
        toSleepThread.start();
        //Make sure the above thread has run.
        while (toSleepThread.getState() == Thread.State.RUNNABLE);
        assertThat(oscillatingQueueWrapper.isGrowing()).isFalse();

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < maxGrowLimit + 1; i++) {
                try {
                    oscillatingQueueWrapper.poll(100, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        consumer.start();

        while (consumer.getState() == Thread.State.RUNNABLE);
        oscillatingQueueWrapper.put(0);
        assertThat(oscillatingQueueWrapper.isGrowing()).isTrue();
    }
}