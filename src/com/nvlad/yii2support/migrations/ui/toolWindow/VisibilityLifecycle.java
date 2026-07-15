package com.nvlad.yii2support.migrations.ui.toolWindow;

final class VisibilityLifecycle implements AutoCloseable {
    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    interface SubscriptionFactory {
        Subscription subscribe();
    }

    private final SubscriptionFactory subscriptionFactory;
    private Subscription subscription;
    private boolean visible;
    private boolean disposed;

    VisibilityLifecycle(SubscriptionFactory subscriptionFactory) {
        this.subscriptionFactory = subscriptionFactory;
    }

    synchronized void setVisible(boolean newVisible) {
        if (disposed || visible == newVisible) {
            return;
        }

        visible = newVisible;
        if (visible) {
            subscription = subscriptionFactory.subscribe();
        } else {
            closeSubscription();
        }
    }

    synchronized boolean isVisible() {
        return visible && !disposed;
    }

    @Override
    public synchronized void close() {
        if (disposed) {
            return;
        }

        disposed = true;
        visible = false;
        closeSubscription();
    }

    private void closeSubscription() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
