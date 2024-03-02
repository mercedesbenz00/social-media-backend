package iq.earthlink.social.util;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import iq.earthlink.social.exception.RestApiException;
import org.springframework.stereotype.Component;

@Component
public class CustomBeforeSendCallback implements SentryOptions.BeforeSendCallback {
    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        if (event.getThrowable() instanceof RestApiException) {
            return null;
        }
        return event;
    }
}