package iq.earthlink.social.classes.data.dto;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ImageSizeType {

  ORIGINAL(-1),
  SMALL(200),
  MEDIUM(480),
  BIG(1024),
  XL(1600),
  HD(1920);

  private final int rate;

  ImageSizeType(int rate) {
    this.rate = rate;
  }

  public static Optional<ImageSizeType> getByRate(int rate) {
    return Arrays.stream(values()).filter(it -> it.rate == rate).findFirst();
  }
}
