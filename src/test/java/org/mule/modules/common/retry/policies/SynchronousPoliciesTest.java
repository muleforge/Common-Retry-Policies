package org.mule.modules.common.retry.policies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.config.ChainedThreadingProfile;
import org.mule.modules.common.retry.notifiers.RetryNotifierStub;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.work.MuleWorkManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Dossot (david@dossot.net)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:synchronous-policies-config.xml" })
public class SynchronousPoliciesTest {

    private static class EventuallySuccessfulRetryCallback implements RetryCallback {

        private final int retriesBeforeSuccess;

        private int retryCount = 0;

        EventuallySuccessfulRetryCallback(final int retriesBeforeSuccess) {
            this.retriesBeforeSuccess = retriesBeforeSuccess;
        }

        public void doWork(final RetryContext context) throws Exception {
            retryCount++;

            if (retryCount <= retriesBeforeSuccess) {
                throw new Exception("Failed attempt #" + retryCount);
            }
        }

        public String getWorkDescription() {
            return "EventuallySuccessfulRetryCallback";
        }
    }

    @Resource
    private RetryPolicyTemplate foreverRetryPolicyTemplate;

    @Resource
    private RetryPolicyTemplate exhaustingRetryPolicyTemplate;

    private WorkManager workManager;

    @Before
    public void initializeRetryNotifier() {
        foreverRetryPolicyTemplate.setNotifier(new RetryNotifierStub());
        exhaustingRetryPolicyTemplate.setNotifier(new RetryNotifierStub());
        workManager = new MuleWorkManager(new ChainedThreadingProfile(), "unit-test-wm", 5000);
    }

    @Test
    public void testForeverRetryPolicyTemplate() throws Exception {
        foreverRetryPolicyTemplate.execute(new EventuallySuccessfulRetryCallback(10), workManager);

        final RetryNotifierStub retryNotifierStub = (RetryNotifierStub) foreverRetryPolicyTemplate.getNotifier();

        assertEquals(1, retryNotifierStub.getSuccessCount());
        assertEquals(10, retryNotifierStub.getFailedCount());
    }

    @Test
    public void testExhaustingRetryPolicyTemplateWithoutSuccess() throws Exception {

        try {
            exhaustingRetryPolicyTemplate.execute(new EventuallySuccessfulRetryCallback(10), workManager);

            fail("Should have got a RetryPolicyExhaustedException");
        } catch (final RetryPolicyExhaustedException rpee) {

        }

        final RetryNotifierStub retryNotifierStub = (RetryNotifierStub) exhaustingRetryPolicyTemplate.getNotifier();

        assertEquals(0, retryNotifierStub.getSuccessCount());
        assertEquals(6, retryNotifierStub.getFailedCount());
    }

    @Test
    public void testExhaustingRetryPolicyTemplateWithSuccess() throws Exception {

        exhaustingRetryPolicyTemplate.execute(new EventuallySuccessfulRetryCallback(3), workManager);

        final RetryNotifierStub retryNotifierStub = (RetryNotifierStub) exhaustingRetryPolicyTemplate.getNotifier();

        assertEquals(1, retryNotifierStub.getSuccessCount());
        assertEquals(3, retryNotifierStub.getFailedCount());
    }
}
