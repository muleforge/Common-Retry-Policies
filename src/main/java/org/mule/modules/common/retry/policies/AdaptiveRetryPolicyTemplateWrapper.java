
package org.mule.modules.common.retry.policies;

import java.util.Map;

import javax.resource.spi.work.WorkException;

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
 * A retry policy template that uses an adaptative strategy for executing it: if Mule
 * is not started, it will execute it in a different thread, if Mule is started it
 * will execute it in the current thread. The idea is to allow Mule to start with a
 * failed connector by not holding the bootstrap thread but, after startup, to hold
 * all receiver/dispatcher threads until the reconnection happens.
 * 
 * @author David Dossot (david@dossot.net)
 */
public class AdaptiveRetryPolicyTemplateWrapper implements RetryPolicyTemplate, MuleContextAware
{
    private final Log logger = LogFactory.getLog(getClass());

    private RetryPolicyTemplate delegateRetryPolicyTemplate;

    private Map<Object, Object> metaInfo;

    private MuleContext muleContext;

    public RetryPolicy createRetryInstance()
    {
        return delegateRetryPolicyTemplate.createRetryInstance();
    }

    public RetryContext execute(final RetryCallback callback, final WorkManager workManager) throws Exception
    {
        final RetryWork retryWork = new RetryWork(muleContext, workManager, delegateRetryPolicyTemplate,
            callback);

        if (muleContext.isStarted())
        {
            return doSynchronousReconnectionWithDelegateRetryPolicyTemplate(callback, retryWork);
        }

        final RetryContext initialResult = doSynchronousInitialConnectionAttempt(callback);

        if (initialResult.isOk())
        {
            return initialResult;
        }

        retryWork.setMustRecoverConnectables(true);
        return scheduleAsynchronousReconnection(callback, workManager, retryWork);
    }

    private RetryContext doSynchronousInitialConnectionAttempt(final RetryCallback callback)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Attempting one synchronous retry callback: " + callback);
        }

        final RetryContext retryContext = new DefaultRetryContext(callback.getWorkDescription(),
            getMetaInfo());

        try
        {
            callback.doWork(retryContext);
            retryContext.setOk();
        }
        catch (final Exception e)
        {
            logger.warn("Initial synchronous connection failed for: " + callback, e);
            retryContext.setFailed(e);
        }

        return retryContext;
    }

    private RetryContext scheduleAsynchronousReconnection(final RetryCallback callback,
                                                          final WorkManager workManager,
                                                          final RetryWork retryWork) throws WorkException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Scheduling asynchronous retry callback: " + callback);
        }

        getActiveWorkManager(workManager).scheduleWork(retryWork);

        return new DefaultRetryContext(callback.getWorkDescription(), getMetaInfo());
    }

    private WorkManager getActiveWorkManager(final WorkManager workManager)
    {
        WorkManager workManagerInUse = workManager;

        if (workManagerInUse == null)
        {
            logger.warn("Ouch! No work manager has been provided by Mule, using the global one.");
            workManagerInUse = muleContext.getWorkManager();
        }
        return workManagerInUse;
    }

    private RetryContext doSynchronousReconnectionWithDelegateRetryPolicyTemplate(final RetryCallback callback,
                                                                                  final RetryWork retryWork)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing retry callback synchronously: " + callback);
        }

        retryWork.run();

        return retryWork.getRetryContextResult();
    }

    public void setDelegate(final RetryPolicyTemplate delegate)
    {
        this.delegateRetryPolicyTemplate = delegate;
    }

    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public RetryNotifier getNotifier()
    {
        return delegateRetryPolicyTemplate.getNotifier();
    }

    public void setNotifier(final RetryNotifier retryNotifier)
    {
        delegateRetryPolicyTemplate.setNotifier(retryNotifier);
    }

    public Map<Object, Object> getMetaInfo()
    {
        return metaInfo;
    }

    @SuppressWarnings("unchecked")
    public void setMetaInfo(final Map metaInfo)
    {
        this.metaInfo = metaInfo;
    }

    // For Spring IoC only
    public void setId(final String id)
    {
        // ignore
    }

}
