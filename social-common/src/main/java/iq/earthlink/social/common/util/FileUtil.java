package iq.earthlink.social.common.util;

import iq.earthlink.social.exception.BadRequestException;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public static final String IMAGE_PREFIX = "image/";
    public static final String VIDEO_PREFIX = "video/";

    private FileUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Reads lines from file
     *
     * @param filename - file name
     */
    public static Set<String> readLinesFromFile(String filename) {
        Set<String> lines = new HashSet<>();
        Resource resource = new ClassPathResource(filename);
        try {
            Stream<String> stream = Files.lines(resource.getFile().toPath());
            lines = stream.map(StringUtils::lowerCase).collect(Collectors.toSet());
            stream.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading {}", resource.getFilename());
        }
        return lines;
    }

    public static boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return StringUtils.isNotBlank(contentType) && contentType.startsWith(IMAGE_PREFIX);
    }

    public static boolean isVideo(MultipartFile file) {
        return isVideo(file.getContentType());
    }

    public static boolean isVideo(String contentType) {
        return StringUtils.isNotBlank(contentType) && contentType.startsWith(VIDEO_PREFIX);
    }

    public static void validateImageOrVideoFiles(List<MultipartFile> files, Long imageMaxSize, Long videoMaxSize) {
        validateImageOrVideoFiles(files, Integer.MAX_VALUE, imageMaxSize, Integer.MAX_VALUE, videoMaxSize);
    }

    public static void validateImageOrVideoFiles(List<MultipartFile> files, Integer imageMaxCount, Long imageMaxSize,
                                                 Integer videoMaxCount, Long videoMaxSize) {
        List<MultipartFile> imageFiles = files.stream().filter(FileUtil::isImage).toList();
        List<MultipartFile> videoFiles = files.stream().filter(FileUtil::isVideo).toList();

        FileUtil.checkFilesConfig(imageFiles, imageMaxCount, imageMaxSize);
        FileUtil.checkFilesConfig(videoFiles, videoMaxCount, videoMaxSize);
    }

    public static void checkFilesConfig(List<MultipartFile> fileList, int maxCount, long maxSize) {
        if (fileList.size() > maxCount) {
            throw new BadRequestException("error.file.max.number.exceeded");
        }
        fileList.forEach(file -> checkFileMaxSize(maxSize, file));
    }

    public static void checkFileMaxSize(long maxSizeMb, MultipartFile file) {
        // Get file size in Mb:
        long sizeInMb = file.getSize() / (1024 * 1024);

        if (sizeInMb > maxSizeMb) {
            throw new MaxUploadSizeExceededException(maxSizeMb);
        }
    }

    public static void checkContentType(MultipartFile file, String imagePrefix) throws FileUploadBase.InvalidContentTypeException {
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !contentType.startsWith(imagePrefix)) {
            throw new FileUploadBase.InvalidContentTypeException();
        }
    }

    // convert BufferedImage to byte[]
    public static byte[] toByteArray(BufferedImage bi, String format)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        return baos.toByteArray();

    }

    // convert byte[] to BufferedImage
    public static BufferedImage toBufferedImage(byte[] bytes)
            throws IOException {

        InputStream is = new ByteArrayInputStream(bytes);
        return ImageIO.read(is);

    }
}
