package org.mule.modules.common.retry.policies;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;

/**
 * @author David Dossot (david@dossot.net)
 */
abstract class RetryContextUtil {

    private final static Log LOGGER = LogFactory.getLog(RetryContextUtil.class);

    private RetryContextUtil() {
        throw new UnsupportedOperationException("Do not instantiate");
    }

    static void recoverConnectables(final MuleContext muleContext, final String retryContextDescription) {

        final Collection<AbstractConnector> connectors = muleContext.getRegistry().lookupObjects(AbstractConnector.class);

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

    private static void recoverConnector(final AbstractConnector connector) {
        LOGGER.info("Recovering connector: " + connector);

        try {
            connector.start();
            connector.connect();
        } catch (final Exception e) {
            LOGGER.error("Error when recovering connector: " + connector, e);
        }
    }

    private static void recoverMessageReceivers(final AbstractConnector connector) {
        for (final MessageReceiver messageReceiver : connector.getReceivers("*")) {

            LOGGER.info("Recovering message receiver: " + messageReceiver);

            try {
                messageReceiver.connect();
            } catch (final Exception e) {
                LOGGER.error("Error when recovering message receiver: " + messageReceiver, e);
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
    private static boolean hasConnectorBeenReconnected(final String retryContextDescription, final AbstractConnector connector) {

        return retryContextDescription.contains("'" + connector.getName() + "'");
    }

}
