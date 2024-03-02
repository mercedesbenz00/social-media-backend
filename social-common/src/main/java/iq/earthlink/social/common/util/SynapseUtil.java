package iq.earthlink.social.common.util;


public class SynapseUtil {
    public static final String USER_PREFIX = "user.";
    public static final String GUEST_PREFIX = "guest.";
    public static final String NOT_FOUND = "NOT FOUND";

    public static final String AT_SIGN = "@";
    public static final String COLON_SIGN = ":";
    public static final String HASH_SIGN = "#";
    public static final String DASH_SIGN = "-";
    public static final String USE_POST_REQUEST = "USE POST REQUEST";

    private SynapseUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateMatrixUserId(Long personId, String synapseServerName) {
        return AT_SIGN + USER_PREFIX + personId + COLON_SIGN + synapseServerName;
    }
}
