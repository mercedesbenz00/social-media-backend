package iq.earthlink.social.common.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.util.UriComponentsBuilder;

import java.beans.PropertyDescriptor;
import java.util.*;

public class CommonUtil {
    public static final String SUFFIX = "...";
    public static final int SHORT_CONTENT_MAX_WORDS = 3;
    private static final Random rnd = new Random();

    private CommonUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String[] getPropertyNamesToIgnore (Object source, boolean ignoreNullProperties, String... names) {
        Set<String> propertiesToIgnore = new HashSet<>();

        if (ignoreNullProperties) {
            // Get NULL properties names:
            final BeanWrapper src = new BeanWrapperImpl(source);
            PropertyDescriptor[] pds = src.getPropertyDescriptors();

            for (PropertyDescriptor pd : pds) {
                Object srcValue = src.getPropertyValue(pd.getName());
                if (srcValue == null) propertiesToIgnore.add(pd.getName());
            }
        }
        // Append additional names:
        propertiesToIgnore.addAll(List.of(names));

        String[] result = new String[propertiesToIgnore.size()];
        return propertiesToIgnore.toArray(result);
    }

    public static String getPartialString(String source) {
        if (StringUtils.isNotEmpty(source)) {
            // Regular expression to match HTML tags
            String regex = "<[^>]*>";

            // Remove all occurrences of the regex pattern
            source = source.replaceAll(regex, "");

            String[] words = source.split("\\s+");
            if (words.length > SHORT_CONTENT_MAX_WORDS) {
                int index = 0;
                for (int i = 0; i < words.length; i++){
                    if (i < SHORT_CONTENT_MAX_WORDS) {
                        index += words[i].length() + 1;
                    }
                }
                source = StringUtils.substring(source, 0, index - 1) + SUFFIX;
            }
        }

        return source;
    }

    public static String getRandomNumberString() {
        // Generate 6 digit random number from 0 to 999999:
        int number = rnd.nextInt(999999);

        // Convert any number sequence into 6 characters:
        return String.format("%06d", number);
    }

    public static String generateURL(String url, String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(url)
                .path(path);
        queryParams.forEach(builder::queryParam);
        return builder.build().toUriString();
    }

}
