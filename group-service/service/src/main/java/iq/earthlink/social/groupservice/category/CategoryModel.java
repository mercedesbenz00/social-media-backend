package iq.earthlink.social.groupservice.category;

import iq.earthlink.social.common.file.MediaFile;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryModel {
    Long id;
    UUID categoryUUID;
    String name;
    Date createdAt;
    Category parentCategory;
    Long groupCount;
    MediaFile avatar;
    MediaFile cover;
    Long personCount;

    public CategoryModel(Long id, String name, Date createdAt, Category parentCategory) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.parentCategory = parentCategory;
    }

    public CategoryModel(Long id, String name, Date createdAt, UUID categoryUUID, Category parentCategory,
                         MediaFile avatar, MediaFile cover, Long personCount, Long groupCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.categoryUUID = categoryUUID;
        this.parentCategory = parentCategory;
        this.avatar = avatar;
        this.cover = cover;
        this.personCount = personCount;
        this.groupCount = groupCount;
    }

    public CategoryModel(Long id, Long groupCount) {
        this.id = id;
        this.groupCount = groupCount;
    }

    public CategoryModel(Long id, Long personCount, Category parentCategory) {
        this.id = id;
        this.personCount = personCount;
        this.parentCategory = parentCategory;
    }

    public CategoryModel(Category parentCategory, Long id, Long groupCount) {
        this.id = id;
        this.groupCount = groupCount;
        this.parentCategory = parentCategory;
    }
}
