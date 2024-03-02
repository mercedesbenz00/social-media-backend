package iq.earthlink.social.common.file.rest;

import iq.earthlink.social.common.file.MediaFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;

import static com.google.common.net.HttpHeaders.ACCEPT_RANGES;
import static com.google.common.net.HttpHeaders.CONTENT_RANGE;
import static jdk.jfr.DataAmount.BYTES;

public final class DownloadUtils {

    public static final String DASH = "-";
    public static final String SLASH = "/";
    public static final String SPACE = " ";

    private DownloadUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static ResponseEntity<Resource> fileResponse(MediaFile file, InputStream is) {
        return fileResponse(file, is, null);
    }

    public static ResponseEntity<Resource> fileResponse(MediaFile file, InputStream is, String rangeHeader) {
        return StringUtils.isEmpty(rangeHeader) ? getResponse(file, is) : getRangeResponse(file, is, rangeHeader);
    }

    public static Long getOffset(String rangeHeader) {
        return StringUtils.isNotEmpty(rangeHeader) ? Long.parseLong(rangeHeader.split(DASH)[0].substring(6)) : 0L;
    }

    public static Long getRangeEnd(MediaFile file, String rangeHeader) {
        if (StringUtils.isNotEmpty(rangeHeader)) {
            String[] ranges = rangeHeader.split(DASH);
            long rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1].trim()) : file.getSize() - 1;

            if (file.getSize() < rangeEnd) {
                rangeEnd = file.getSize() - 1;
            }
            return rangeEnd;
        }
        return null;
    }

    public static String getContentRangeValue(MediaFile file, String rangeHeader) {
        return BYTES + SPACE + getOffset(rangeHeader) + DASH + getRangeEnd(file, rangeHeader) + SLASH + file.getSize();
    }

    public static Long getLength(MediaFile file, String rangeHeader) {
        Long rangeEnd = getRangeEnd(file, rangeHeader);
        return rangeEnd != null ? ((rangeEnd - getOffset(rangeHeader)) + 1) : file.getSize();
    }

    private static ResponseEntity<Resource> getResponse(MediaFile file, InputStream is) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
        headers.setContentLength(file.getSize());

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(new InputStreamResource(is));
    }

    private static ResponseEntity<Resource> getRangeResponse(MediaFile file, InputStream is, String rangeHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
        headers.setContentLength(getLength(file, rangeHeader));
        headers.add(ACCEPT_RANGES, BYTES);
        headers.add(CONTENT_RANGE, getContentRangeValue(file, rangeHeader));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(new InputStreamResource(is));
    }
}
