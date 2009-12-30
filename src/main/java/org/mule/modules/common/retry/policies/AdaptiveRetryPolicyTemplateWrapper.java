package org.mule.modules.common.retry.policies;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
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
public class AdaptiveRetryPolicyTemplateWrapper implements RetryPolicyTemplate, MuleContextAware {

    private final Log logger = LogFactory.getLog(getClass());

    private RetryPolicyTemplate delegate;

    private MuleContext muleContext;

    @SuppressWarnings("unchecked")
    private Map metaInfo;

    private int initialAttemptTimeout = 5000;

    public RetryPolicy createRetryInstance() {
        return delegate.createRetryInstance();
    }

    public RetryContext execute(final RetryCallback callback, final WorkManager workManager) throws Exception {

        final RetryWork retryWork = new RetryWork(muleContext, workManager, delegate, callback);

        if (muleContext.isStarted()) {
            return doSynchronousReconnection(callback, retryWork);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Executing retry callback asynchronously: " + callback);
        }

        workManager.scheduleWork(retryWork);

        return trySynchronousConnection(callback, retryWork);
    }

    private RetryContext doSynchronousReconnection(final RetryCallback callback, final RetryWork retryWork) {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing retry callback synchronously: " + callback);
        }

        retryWork.run();

        return retryWork.getRetryContextResult();
    }

    private RetryContext trySynchronousConnection(final RetryCallback callback, final RetryWork retryWork)
            throws InterruptedException {

        if (retryWork.await(initialAttemptTimeout, TimeUnit.MILLISECONDS)) {

            final RetryContext retryContext = retryWork.getRetryContextResult();

            if (logger.isDebugEnabled()) {
                logger.debug("Successful synchronous execution of callback: " + retryContext.getDescription());
            }

            return retryContext;
        }

        return new DefaultRetryContext(callback.getWorkDescription());
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

    @SuppressWarnings("unchecked")
    public Map getMetaInfo() {
        return metaInfo;
    }

    @SuppressWarnings("unchecked")
    public void setMetaInfo(final Map metaInfo) {
        this.metaInfo = metaInfo;
    }

}
