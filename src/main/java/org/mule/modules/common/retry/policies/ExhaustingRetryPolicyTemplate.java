package org.mule.modules.common.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.policies.AbstractPolicyTemplate;

public class ExhaustingRetryPolicyTemplate extends AbstractPolicyTemplate {

    public RetryPolicy createRetryInstance() {
        return new ExhaustingRetryPolicy();
    }
}