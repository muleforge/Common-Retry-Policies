package org.mule.modules.common.retry.policies;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.modules.common.retry.notifiers.RetryNotifierStub;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Dossot (david@dossot.net)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:synchronous-policies-config.xml" })
public class SynchronousPoliciesTest {

    private static final int RETRIES_BEFORE_SUCCESS = 10;

    private static class EventuallySuccessfulRetryCallback implements
            RetryCallback {

        private int retryCount = 0;

        public void doWork(final RetryContext context) throws Exception {
            retryCount++;

            if (retryCount <= RETRIES_BEFORE_SUCCESS) {
                throw new Exception("Failed attempt #" + retryCount);
            }
        }

        public String getWorkDescription() {
            return "EventuallySuccessfulRetryCallback";
        }
    }

    @Resource
    private RetryPolicyTemplate foreverRetryPolicyTemplate;

    private RetryCallback eventuallySuccessfulRetryCallback;

    @Before
    public void inialize() {
        eventuallySuccessfulRetryCallback = new EventuallySuccessfulRetryCallback();
    }

    @Test
    public void testForeverRetryPolicyTemplate() throws Exception {
        foreverRetryPolicyTemplate.execute(eventuallySuccessfulRetryCallback);

        assertEquals(1, ((RetryNotifierStub) foreverRetryPolicyTemplate
                .getNotifier()).getSuccessCount());

        assertEquals(10, ((RetryNotifierStub) foreverRetryPolicyTemplate
                .getNotifier()).getFailedCount());
    }
}
