package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;

public class ExhaustingRetryPolicyTemplate extends
        AbstractSleepingPolicyTemplate {

    private int retryLimit = Integer.MAX_VALUE;

    public void setRetryLimit(final int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public RetryPolicy createRetryInstance() {
        return new ExhaustingRetryPolicy(getSleepTime(), retryLimit);
    }
}