package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

/**
 * A retry policy that retries for ever and waits for a configured amount of
 * time between retries.
 * 
 * @author John D'Emic
 * @author David Dossot (david@dossot.net)
 */
public class ForeverRetryPolicy implements RetryPolicy {

    private final long sleepTime;

    public ForeverRetryPolicy(final long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public PolicyStatus applyPolicy(final Throwable throwable) {
        try {
            Thread.sleep(sleepTime);
            return PolicyStatus.policyOk();

        } catch (final InterruptedException e) {
            // restore the interrupted status and deem this policy exhausted
            Thread.currentThread().interrupt();
            return PolicyStatus.policyExhausted(e);
        }
    }
}
