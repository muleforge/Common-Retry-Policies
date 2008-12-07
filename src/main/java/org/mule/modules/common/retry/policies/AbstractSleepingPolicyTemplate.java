package org.mule.modules.common.retry.policies;

import org.mule.retry.policies.AbstractPolicyTemplate;

/**
 * @author David Dossot (david@dossot.net)
 */
public abstract class AbstractSleepingPolicyTemplate extends
        AbstractPolicyTemplate {

    private long sleepTime = 5000L;

    public void setSleepTime(final long sleepTime) {
        this.sleepTime = sleepTime;
    }

    protected long getSleepTime() {
        return sleepTime;
    }

}
