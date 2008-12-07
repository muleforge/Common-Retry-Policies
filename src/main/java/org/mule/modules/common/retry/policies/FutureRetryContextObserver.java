package org.mule.modules.common.retry.policies;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.retry.RetryContext;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;

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

            logger.info("The asynchronous retry policy has returned: "
                    + retryContextDescription);

            recoverConnectables(retryContextDescription);

        } catch (final InterruptedException ie) {
            // restore interrupted state
            Thread.currentThread().interrupt();
        } catch (final ExecutionException ee) {
            logger.error("The asynchronous retry policy has failed!", ee);
        }

    }

    private void recoverConnectables(final String retryContextDescription) {
        @SuppressWarnings("unchecked")
        final Collection<AbstractConnector> connectors = muleContext
                .getRegistry().lookupObjects(AbstractConnector.class);

        for (final AbstractConnector connector : connectors) {
            if (hasConnectorBeenReconnected(retryContextDescription, connector)) {

                recoverConnector(connector);

                // we do not recover message requesters and dispatchers because
                // they *seem* to auto-recover when used after reconnection - we
                // may need to do it explicitly depending on community's
                // feedback

                recoverMessageReceivers(connector);
            }

        }
    }

    private void recoverConnector(final AbstractConnector connector) {
        logger.info("Recovering connector: " + connector);

        try {
            connector.start();
            connector.connect();
        } catch (final Exception e) {
            logger.error("Error when recovering connector: " + connector, e);
        }
    }

    private void recoverMessageReceivers(final AbstractConnector connector) {
        for (final MessageReceiver messageReceiver : connector
                .getReceivers("*")) {

            logger.info("Recovering message receiver: " + messageReceiver);

            try {
                messageReceiver.connect();
            } catch (final Exception e) {
                logger.error("Error when recovering message receiver: "
                        + messageReceiver, e);
            }
        }
    }

    /**
     * This method is the Achile's heel of the whole policy: it is based on the
     * fact that the retry context description will contain the string
     * representation of the recovered connector which will be of the expected
     * form.
     * 
     * There must be a better way to do this.
     */
    private boolean hasConnectorBeenReconnected(
            final String retryContextDescription,
            final AbstractConnector connector) {

        return retryContextDescription
                .contains("'" + connector.getName() + "'");
    }
}