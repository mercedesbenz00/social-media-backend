package iq.earthlink.social.classes.data.dto;

public class JsonViews {
    public static class Public { }
    private JsonViews() {
        throw new IllegalStateException("Utility class");
    }
    public static class ExtendedPublic extends Public { }
    public static class Internal extends ExtendedPublic { }
}
