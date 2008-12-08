package org.mule.modules.common.retry.policies;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.retry.RetryContext;

/**
 * A runnable that waits for a <code>Future&lt;RetryContext&gt;</code> to
 * finish, then locates the recovered connector and receivers in order to
 * restart and reconnect them.
 * 
 * @author David Dossot (david@dossot.net)
 */
final class FutureRetryContextObserver implements Runnable {
    private final Log logger = LogFactory.getLog(getClass());

    private final MuleContext muleContext;

    private final Future<RetryContext> futureRetryContext;

    FutureRetryContextObserver(final MuleContext muleContext,
            final Future<RetryContext> futureRetryContext) {

        this.muleContext = muleContext;
        this.futureRetryContext = futureRetryContext;
    }

    public void run() {
        try {
            final RetryContext retryContext = futureRetryContext.get();

            final String retryContextDescription = retryContext
                    .getDescription();

            if (logger.isDebugEnabled()) {
                logger.debug("The asynchronous retry policy has returned: "
                        + retryContextDescription);
            }

            RetryContextUtil.recoverConnectables(muleContext,
                    retryContextDescription);

        } catch (final InterruptedException ie) {
            // restore interrupted state
            Thread.currentThread().interrupt();
        } catch (final ExecutionException ee) {
            logger.error("The asynchronous retry policy has failed!", ee);
        }

    }

}