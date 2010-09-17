
package org.mule.modules.common.retry.policies;

import java.util.concurrent.atomic.AtomicReference;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;

/**
 * @author David Dossot (david@dossot.net)
 */
class RetryWork implements Work
{
    private final AtomicReference<RetryContext> retryContextReference = new AtomicReference<RetryContext>();

    private final Log logger = LogFactory.getLog(getClass());

    private final MuleContext muleContext;

    private final WorkManager workManager;

    private final RetryPolicyTemplate retryPolicyTemplate;

    private final RetryCallback retryCallback;

    private boolean mustRecoverConnectables = false;

    RetryWork(final MuleContext muleContext,
              final WorkManager workManager,
              final RetryPolicyTemplate retryPolicyTemplate,
              final RetryCallback retryCallback)
    {

        this.muleContext = muleContext;
        this.workManager = workManager;
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.retryCallback = retryCallback;
    }

    public RetryContext getRetryContextResult()
    {
        return retryContextReference.get();
    }

    public void run()
    {
        try
        {
            final RetryContext retryContext = retryPolicyTemplate.execute(retryCallback, workManager);

            retryContextReference.set(retryContext);

            if (logger.isDebugEnabled())
            {
                logger.debug("The retry policy has returned: " + retryContext + " ("
                             + retryContext.getDescription() + ")");
            }

            if (mustRecoverConnectables)
            {
                RetryContextUtil.recoverConnectables(muleContext, retryContext);
            }
        }
        catch (final InterruptedException ie)
        {
            // restore interrupted state
            Thread.currentThread().interrupt();
        }
        catch (final Exception e)
        {
            logger.error("The asynchronous retry policy has failed!", e);
        }

    }

    public void release()
    {
        // TODO use a meta info in the retryContext to signal the policy it
        // should stop
    }

    public void setMustRecoverConnectables(boolean b)
    {
        mustRecoverConnectables = b;
    }

}
