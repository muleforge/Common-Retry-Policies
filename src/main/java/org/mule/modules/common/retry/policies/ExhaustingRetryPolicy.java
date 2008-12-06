package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

public class ExhaustingRetryPolicy implements RetryPolicy {
    // TODO make this value configurable
    private static int RETRY_LIMIT = 5;
    private int retryCounter = 0;

    public PolicyStatus applyPolicy(final Throwable throwable) {
        if (retryCounter >= RETRY_LIMIT) {
            return PolicyStatus.policyExhausted(throwable);
        }

        try {
            // TODO make this value configurable
            Thread.sleep(5000);
        } catch (final InterruptedException e) {
            // restore the interrupted status and deem this policy exhausted
            Thread.currentThread().interrupt();
            return PolicyStatus.policyExhausted(e);
        }

        retryCounter++;
        return PolicyStatus.policyOk();
    }
}
