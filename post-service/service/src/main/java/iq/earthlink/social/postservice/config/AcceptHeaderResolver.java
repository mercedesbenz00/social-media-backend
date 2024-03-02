package iq.earthlink.social.postservice.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Locale;

@Component
public class AcceptHeaderResolver extends AcceptHeaderLocaleResolver {

    @Override
    public @NotNull Locale resolveLocale(HttpServletRequest request) {
        String headerLang = request.getHeader("Accept-Language");
        return headerLang == null || headerLang.isEmpty()
                ? Locale.ENGLISH
                : Locale.lookup(Locale.LanguageRange.parse(headerLang), Arrays.asList(Locale.getAvailableLocales()));
    }
}