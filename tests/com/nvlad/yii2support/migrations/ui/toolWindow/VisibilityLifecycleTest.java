package com.nvlad.yii2support.migrations.ui.toolWindow;

import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicInteger;

public class VisibilityLifecycleTest extends TestCase {
    public void testShowHideDoesNotAccumulateSubscriptions() {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger opened = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        VisibilityLifecycle lifecycle = new VisibilityLifecycle(() -> {
            opened.incrementAndGet();
            assertEquals(1, active.incrementAndGet());
            return () -> {
                closed.incrementAndGet();
                assertEquals(0, active.decrementAndGet());
            };
        });

        lifecycle.setVisible(false);
        lifecycle.setVisible(true);
        lifecycle.setVisible(true);
        assertEquals(1, opened.get());
        assertEquals(1, active.get());

        lifecycle.setVisible(false);
        lifecycle.setVisible(false);
        assertEquals(1, closed.get());
        assertEquals(0, active.get());

        lifecycle.setVisible(true);
        assertEquals(2, opened.get());
        assertEquals(1, active.get());

        lifecycle.close();
        lifecycle.close();
        assertEquals(2, closed.get());
        assertEquals(0, active.get());

        lifecycle.setVisible(true);
        assertEquals(2, opened.get());
    }
}
