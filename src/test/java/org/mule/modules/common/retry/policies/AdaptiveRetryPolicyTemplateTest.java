package org.mule.modules.common.retry.policies;

import static org.junit.Assert.assertEquals;
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

    private static final String QUEUE_NAME = "targetQueue";
    private static final String BROKER_ID = "crepol-broker";

    @Test
    public void initialSynchronousConnection() throws Exception {
        final BrokerService amqBroker = newActiveMQBroker();
        final MuleContext muleContext = newMuleServer();

        final AbstractConnector connector = (AbstractConnector) muleContext
                .getRegistry().lookupConnector("JmsConnector");

        assertTrue("JmsConnector connected", connector.isConnected());
        assertTrue("JmsConnector started", connector.isStarted());

        assertAllMessageReceiversConnected(connector);

        String payload = sendMessageToQueueUsingJmsClient();
        assertEquals(payload, waitForMessageInFunctionalComponent(muleContext));

        payload = sendMessageToQueueUsingMuleClient(muleContext);
        assertEquals(payload, waitForMessageInFunctionalComponent(muleContext));

        muleContext.dispose();
        amqBroker.stop();
    }

    private String sendMessageToQueueUsingMuleClient(
            final MuleContext muleContext) throws MuleException {
        String payload;
        payload = RandomStringUtils.randomAlphanumeric(10);

        new MuleClient(muleContext).dispatch("jms://" + QUEUE_NAME, payload,
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

        while ((testComponent.getReceivedMessages() == 0) && (i++ < 100)) {
            Thread.sleep(500L);
        }

        assertEquals(1, testComponent.getReceivedMessages());

        final Object receivedMessagePayload = testComponent
                .getReceivedMessage(1);

        testComponent.initialise();

        return receivedMessagePayload;
    }

    private String sendMessageToQueueUsingJmsClient() throws JMSException {
        final Connection connection = new ActiveMQConnectionFactory("vm://"
                + BROKER_ID + "?create=false").createConnection();

        final Session session = connection.createSession(false,
                Session.AUTO_ACKNOWLEDGE);

        final String payload = RandomStringUtils.randomAlphanumeric(10);

        session.createProducer(session.createQueue(QUEUE_NAME)).send(
                session.createTextMessage(payload));

        connection.close();
        return payload;
    }

    private BrokerService newActiveMQBroker() throws Exception {
        final BrokerService amqBroker = new BrokerService();
        amqBroker.setPersistent(false);
        amqBroker.setDeleteAllMessagesOnStartup(true);
        amqBroker.setBrokerName(BROKER_ID);
        amqBroker.start();
        return amqBroker;
    }
}
