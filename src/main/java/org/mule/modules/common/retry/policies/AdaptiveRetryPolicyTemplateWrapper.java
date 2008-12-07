package org.mule.modules.common.retry.policies;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.DefaultRetryContext;

/**
 * A retry policy template that uses an adaptative strategy for executing it: if
 * Mule is not started, it will execute it in a different thread, if Mule is
 * started it will execute it in the current thread.
 * 
 * The idea is to allow Mule to start with a failed connector by not holding the
 * bootstrap thread but, after startup, to hold all receiver/dispatcher threads
 * until the reconnection happens.
 * 
 * @author David Dossot (david@dossot.net)
 */
public class AdaptiveRetryPolicyTemplateWrapper implements RetryPolicyTemplate,
        MuleContextAware {

    private final Log logger = LogFactory.getLog(getClass());
    private RetryPolicyTemplate delegate;

    private MuleContext muleContext;

    private ExecutorService retryPolicyExecutor;

    private int initialAttemptTimeout = 5000;

    public RetryPolicy createRetryInstance() {
        return delegate.createRetryInstance();
    }

    public RetryContext execute(final RetryCallback callback) throws Exception {
        if (muleContext.isStarted()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Executing retry callback synchronously: "
                        + callback);
            }

            return delegate.execute(callback);
        }

        if (logger.isDebugEnabled()) {
            logger
                    .debug("Executing retry callback asynchronously: "
                            + callback);
        }

        final Future<RetryContext> futureRetryContext = retryPolicyExecutor
                .submit(new Callable<RetryContext>() {
                    public RetryContext call() throws Exception {
                        return delegate.execute(callback);
                    }
                });

        try {
            final RetryContext retryContext = futureRetryContext.get(
                    initialAttemptTimeout, TimeUnit.MILLISECONDS);

            if (logger.isDebugEnabled()) {
                logger.debug("Successful synchronous execution of callback: "
                        + retryContext.getDescription());
            }

            return retryContext;

        } catch (final Exception e) {
            logger.warn("Failed first synchronous attempt to execute: "
                    + callback.getWorkDescription(), e);

            retryPolicyExecutor.execute(new FutureRetryContextObserver(
                    muleContext, futureRetryContext));

            return new DefaultRetryContext(callback.getWorkDescription());
        }
    }

    public RetryNotifier getNotifier() {
        return delegate.getNotifier();
    }

    public void setDelegate(final RetryPolicyTemplate delegate) {
        this.delegate = delegate;
    }

    public void setInitialAttemptTimeout(final int initialAttemptTimeout) {
        this.initialAttemptTimeout = initialAttemptTimeout;
    }

    public void setMuleContext(final MuleContext muleContext) {
        this.muleContext = muleContext;
    }

    public void setNotifier(final RetryNotifier retryNotifier) {
        delegate.setNotifier(retryNotifier);
    }

    public void setRetryPolicyExecutor(final ExecutorService retryPolicyExecutor) {
        this.retryPolicyExecutor = retryPolicyExecutor;
    }

}
