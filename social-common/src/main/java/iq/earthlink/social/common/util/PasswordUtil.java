package iq.earthlink.social.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class PasswordUtil {

    private static final String COMMON_PASSWORD_WORDS_TXT = "commonPasswordWords.txt";

    private static final Set<String> commonWords = FileUtil.readLinesFromFile(COMMON_PASSWORD_WORDS_TXT);

    private PasswordUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isCommonWord(String password) {
        return commonWords.contains(StringUtils.lowerCase(password));
    }
}
