package iq.earthlink.social.util;

import iq.earthlink.social.exception.RestApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
public class LocalizationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalizationUtil.class);
    private final MessageSource messageSource;

    public LocalizationUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    public String getLocalizedMessage(String messageId, Object... args) {
        return getLocalizedMessage(messageId, null, args);
    }

    public String getLocalizedMessage(String messageId, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(messageId, args, locale == null ? LocaleContextHolder.getLocale() : locale);
        } catch (NoSuchMessageException exception) {
            LOGGER.error("getLocalizedMessage: message not found", exception);
            // Ok- just use msg
        }
        return messageId;
    }

    public static void checkLocalization(String localization) {
        if (Arrays.stream(Locale.getAvailableLocales()).noneMatch(locale -> locale.getLanguage().equals(localization))) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, "error.unsupported.locale", localization);
        }
    }
}
