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
        // special case :( catch JMS connector issues and consider connector
        // dead if occurred
        if ((throwable instanceof IllegalStateException) && ("Deque full".equals(throwable.getMessage()))) {
            return PolicyStatus.policyExhausted(throwable);
        }

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
