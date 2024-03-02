package iq.earthlink.social.classes.enumeration;

import lombok.Getter;

import java.util.stream.Stream;

public enum PostState {

  DELETED("DELETED",true, false),
  REJECTED("REJECTED", true, new PostState[2]),
  PUBLISHED("PUBLISHED",true, new PostState[3]),
  WAITING_TO_BE_PUBLISHED("PENDING", false, true, REJECTED, PUBLISHED, DELETED),
  DRAFT("DRAFT",false, true, WAITING_TO_BE_PUBLISHED, PUBLISHED, REJECTED, DELETED);

  static {
    REJECTED.nextStates[0] = PUBLISHED; // It cannot be done in constructor since REJECTED is created before PUBLISHED
    REJECTED.nextStates[1] = DELETED; // It cannot be done in constructor since REJECTED is created before PUBLISHED

    PUBLISHED.nextStates[0]=REJECTED;
    PUBLISHED.nextStates[1]=DELETED;
    PUBLISHED.nextStates[2]=WAITING_TO_BE_PUBLISHED;
  }

  PostState(String displayName, boolean notifyAuthor, PostState... nextStates) {
    this(displayName, notifyAuthor, false, nextStates);
  }

  PostState(String displayName, boolean notifyAuthor, boolean editable, PostState... nextStates) {
    this.notifyAuthor = notifyAuthor;
    this.editable = editable;
    this.nextStates = nextStates;
    this.displayName = displayName;
  }

  private final PostState[] nextStates;
  @Getter
  private final boolean editable;

  @Getter
  private final boolean notifyAuthor;

  @Getter
  private final String displayName;

  public boolean canBeChangedTo(PostState nextState) {
    if (nextState == null) {
      return false;
    }

    return Stream.of(this.nextStates)
        .anyMatch(s -> s == nextState);
  }
}

