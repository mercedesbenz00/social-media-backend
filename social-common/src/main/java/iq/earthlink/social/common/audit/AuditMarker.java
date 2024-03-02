package iq.earthlink.social.common.audit;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * log4j2.xml configures Kafka Audit Appender to only forward messages with this
 * logging marker.
 */
public class AuditMarker {

    private AuditMarker() {
        throw new IllegalStateException("Utility class");
    }

    public static Marker getMarker() {
        return MarkerManager.getMarker("AuditRecord");
    }
}
