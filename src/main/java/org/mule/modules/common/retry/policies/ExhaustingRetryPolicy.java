package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

/**
 * A retry policy that retries for a configured amount of attempts and waits for
 * a configured amount of time between retries.
 * 
 * @author John D'Emic
 * @author David Dossot (david@dossot.net)
 */
public class ExhaustingRetryPolicy implements RetryPolicy {
    private final int retryLimit;
    private int retryCounter = 0;

    private final long sleepTime;

    public ExhaustingRetryPolicy(final long sleepTime, final int retryLimit) {
        this.sleepTime = sleepTime;
        this.retryLimit = retryLimit;
    }

    public PolicyStatus applyPolicy(final Throwable throwable) {
        if (retryCounter >= retryLimit) {
            return PolicyStatus.policyExhausted(throwable);
        }

        try {
            Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
            // restore the interrupted status and deem this policy exhausted
            Thread.currentThread().interrupt();
            return PolicyStatus.policyExhausted(e);
        }

        retryCounter++;

        return PolicyStatus.policyOk();
    }
}
