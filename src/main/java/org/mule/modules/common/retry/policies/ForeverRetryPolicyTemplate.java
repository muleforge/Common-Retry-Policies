package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;

public class ForeverRetryPolicyTemplate extends AbstractSleepingPolicyTemplate {

    public RetryPolicy createRetryInstance() {
        return new ForeverRetryPolicy(getSleepTime());
    }
}
