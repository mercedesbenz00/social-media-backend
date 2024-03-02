package iq.earthlink.social.common.filestorage;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class CompositeFileStorageProvider implements FileStorageProvider {

  private final Map<StorageType, FileStorage> storages;

  public CompositeFileStorageProvider(List<FileStorage> storageList) {
    this.storages = Maps.uniqueIndex(storageList, FileStorage::getStorageType);
  }

  @Nonnull
  @Override
  public FileStorage getStorage(@Nonnull StorageType type) {
    FileStorage fileStorage = storages.get(type);
    if (fileStorage == null) {
      throw new IllegalArgumentException("File storage not found for type: " + type);
    }

    return fileStorage;
  }
}
