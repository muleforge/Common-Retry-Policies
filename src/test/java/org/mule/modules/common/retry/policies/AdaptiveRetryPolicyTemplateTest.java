package org.mule.modules.common.retry.policies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultJavaComponent;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.AbstractConnector;

/**
 * @author David Dossot (david@dossot.net)
 */
public class AdaptiveRetryPolicyTemplateTest {

    private static final String LISTENED_QUEUE_NAME = "listenedQueue";

    private static final String REQUESTED_QUEUE_NAME = "requestedQueue";

    private static final String BROKER_ID = "crepol-broker";

    @Test
    public void jmsBrokerAllUp() throws Exception {
        final BrokerService amqBroker = newActiveMQBroker();
        final MuleContext muleContext = newMuleServer();

        final AbstractConnector connector = getJmsConnector(muleContext);

        assertJmsConnectorFullyFunctional(muleContext, connector);

        muleContext.dispose();
        amqBroker.stop();
    }

    @Test
    public void jmsBrokerDownThenUp() throws Exception {
        final MuleContext muleContext = newMuleServer();

        final AbstractConnector connector = getJmsConnector(muleContext);

        assertFalse("JmsConnector connected", connector.isConnected());
        assertFalse("JmsConnector started",
                isConnectorAndItsReceiversConnected(connector));

        final BrokerService amqBroker = newActiveMQBroker();

        waitForFullyConnectedConnector(connector);

        assertJmsConnectorFullyFunctional(muleContext, connector);

        muleContext.dispose();
        amqBroker.stop();
    }

    @Test
    public void jmsBrokerUpThenDownThenUp() throws Exception {
        BrokerService amqBroker = newActiveMQBroker();
        final MuleContext muleContext = newMuleServer();

        final AbstractConnector connector = getJmsConnector(muleContext);

        assertJmsConnectorFullyFunctional(muleContext, connector);

        amqBroker.stop();
        amqBroker.waitUntilStopped();
        amqBroker = newActiveMQBroker();

        waitForFullyConnectedConnector(connector);

        assertJmsConnectorFullyFunctional(muleContext, connector);

        muleContext.dispose();
        amqBroker.stop();
    }

    private void waitForFullyConnectedConnector(
            final AbstractConnector connector) throws InterruptedException {

        int i = 0;

        while ((!isConnectorAndItsReceiversConnected(connector)) && (i++ < 60)) {
            Thread.sleep(500L);
        }
    }

    private boolean isConnectorAndItsReceiversConnected(
            final AbstractConnector connector) {

        if (!connector.isStarted()) {
            return false;
        }

        if (!connector.isConnected()) {
            return false;
        }

        for (final MessageReceiver messageReceiver : connector
                .getReceivers("*")) {

            if (!messageReceiver.isConnected()) {
                return false;
            }
        }

        return true;
    }

    private void assertJmsConnectorFullyFunctional(
            final MuleContext muleContext, final AbstractConnector connector)
            throws JMSException, Exception, InterruptedException, MuleException {

        assertTrue("JmsConnector connected", connector.isConnected());
        assertTrue("JmsConnector started",
                isConnectorAndItsReceiversConnected(connector));

        assertAllMessageReceiversConnected(connector);

        // test listener
        String payload = sendMessageToQueueUsingJmsClient(LISTENED_QUEUE_NAME);
        assertEquals(payload, waitForMessageInFunctionalComponent(muleContext));

        // test dispatcher
        payload = sendMessageToQueueUsingMuleClient(muleContext,
                LISTENED_QUEUE_NAME);
        assertEquals(payload, waitForMessageInFunctionalComponent(muleContext));

        // test requester
        payload = sendMessageToQueueUsingJmsClient(REQUESTED_QUEUE_NAME);
        assertEquals(payload, requestMessageFromJmsQueue(muleContext,
                REQUESTED_QUEUE_NAME));
    }

    private Object requestMessageFromJmsQueue(final MuleContext muleContext,
            final String queueName) throws MuleException {

        return new MuleClient(muleContext).request("jms://" + queueName, 5000L)
                .getPayload();
    }

    private AbstractConnector getJmsConnector(final MuleContext muleContext) {
        final AbstractConnector connector = (AbstractConnector) muleContext
                .getRegistry().lookupConnector("JmsConnector");

        return connector;
    }

    private String sendMessageToQueueUsingMuleClient(
            final MuleContext muleContext, final String queueName)
            throws MuleException {

        final String payload = RandomStringUtils.randomAlphanumeric(10);

        new MuleClient(muleContext).dispatch("jms://" + queueName, payload,
                Collections.EMPTY_MAP);

        return payload;
    }

    private void assertAllMessageReceiversConnected(
            final AbstractConnector connector) {

        for (final MessageReceiver messageReceiver : connector
                .getReceivers("*")) {

            assertTrue(messageReceiver.getReceiverKey() + " connected",
                    messageReceiver.isConnected());
        }
    }

    private MuleContext newMuleServer() throws Exception {
        final MuleContext muleContext = new DefaultMuleContextFactory()
                .createMuleContext("adaptative-policy-config.xml");

        muleContext.start();

        return muleContext;
    }

    private Object waitForMessageInFunctionalComponent(
            final MuleContext muleContext) throws Exception,
            InterruptedException {

        final DefaultJavaComponent defaultComponent = (DefaultJavaComponent) muleContext
                .getRegistry().lookupService("TestService").getComponent();

        final FunctionalTestComponent testComponent = (FunctionalTestComponent) defaultComponent
                .getObjectFactory().getInstance();

        int i = 0;

        while ((testComponent.getReceivedMessages() == 0) && (i++ < 60)) {
            Thread.sleep(500L);
        }

        assertEquals(1, testComponent.getReceivedMessages());

        final Object receivedMessagePayload = testComponent
                .getReceivedMessage(1);

        testComponent.initialise();

        return receivedMessagePayload;
    }

    private String sendMessageToQueueUsingJmsClient(final String queueName)
            throws JMSException {
        final Connection connection = new ActiveMQConnectionFactory("vm://"
                + BROKER_ID + "?create=false").createConnection();

        final Session session = connection.createSession(false,
                Session.AUTO_ACKNOWLEDGE);

        final String payload = RandomStringUtils.randomAlphanumeric(10);

        session.createProducer(session.createQueue(queueName)).send(
                session.createTextMessage(payload));

        connection.close();
        return payload;
    }

    private BrokerService newActiveMQBroker() throws Exception {
        final BrokerService amqBroker = new BrokerService();
        amqBroker.setPersistent(false);
        amqBroker.setUseJmx(false);
        amqBroker.setUseShutdownHook(false);
        amqBroker.setDeleteAllMessagesOnStartup(true);
        amqBroker.setBrokerName(BROKER_ID);
        amqBroker.start();
        return amqBroker;
    }
}
