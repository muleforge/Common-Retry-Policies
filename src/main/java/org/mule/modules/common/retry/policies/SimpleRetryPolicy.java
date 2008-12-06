package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

public class SimpleRetryPolicy implements RetryPolicy {

    public PolicyStatus applyPolicy(final Throwable throwable) {
        try {
            // TODO make this value configurable
            Thread.sleep(5000);
            return PolicyStatus.policyOk();

        } catch (final InterruptedException e) {
            // restore the interrupted status and deem this policy exhausted
            Thread.currentThread().interrupt();
            return PolicyStatus.policyExhausted(e);
        }
    }
}
