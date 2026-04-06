package org.stockwellness.batch.lifecycle;

public interface BatchLifecycleEventPort {
    void send(BatchLifecycleEvent event);
}
