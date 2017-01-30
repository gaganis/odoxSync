package com.giorgosgaganis.filesynchronizer.utils.ochillatingqueue;

import com.giorgosgaganis.filesynchronizer.utils.ochillatingqueue.OchillatingQueueWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

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
        OchillatingQueueWrapper.OscillatingQueueWrapper<Integer> oscillatingQueueWrapper = new OchillatingQueueWrapper.OscillatingQueueWrapper<>(5, maxGrowLimit);

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
        OchillatingQueueWrapper.OscillatingQueueWrapper<Integer> oscillatingQueueWrapper = new OchillatingQueueWrapper.OscillatingQueueWrapper<>(5, maxGrowLimit);

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
                    oscillatingQueueWrapper.getBackingQueue().take();
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