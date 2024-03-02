package iq.earthlink.social.common.filestorage;

import javax.annotation.Nonnull;

public interface FileStorageProvider {

  @Nonnull
  FileStorage getStorage(@Nonnull StorageType type);
}
