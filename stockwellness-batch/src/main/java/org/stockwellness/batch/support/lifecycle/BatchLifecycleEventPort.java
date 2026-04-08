package org.stockwellness.batch.support.lifecycle;

public interface BatchLifecycleEventPort {
    void send(BatchLifecycleEvent event);
}
