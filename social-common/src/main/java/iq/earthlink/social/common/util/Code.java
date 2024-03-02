package iq.earthlink.social.common.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class Code {
    private Code() {
        throw new IllegalStateException("Utility class");
    }

    private static final Random random = new Random();

    public static String next() {
        return String.valueOf(random.nextInt(999999));
    }

    public static String next(int length) {
        return RandomStringUtils.randomNumeric(length);
    }
}
