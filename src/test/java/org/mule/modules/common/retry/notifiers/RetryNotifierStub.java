package org.mule.modules.common.retry.notifiers;

import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;

/**
 * @author David Dossot (david@dossot.net)
 */
public class RetryNotifierStub implements RetryNotifier {

    private int failedCount = 0;

    private int successCount = 0;

    public void failed(final RetryContext context, final Throwable e) {
        failedCount++;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void sucess(final RetryContext context) {
        successCount++;
    }

    public int getSuccessCount() {
        return successCount;
    }

}
