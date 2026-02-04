package launcher;

public enum Theme {
  INTELLIJ,
  DARCULA,
  LIGHT,
  DARK;

  public static Theme fromString(String value, Theme fallback) {
    if (value == null) {
      return fallback;
    }

    String normalized = value.trim().toLowerCase();
    if (normalized.isEmpty()) {
      return fallback;
    }

    if ("light".equals(normalized)) {
      return LIGHT;
    }
    if ("dark".equals(normalized)) {
      return DARK;
    }
    if ("darcula".equals(normalized)) {
      return DARCULA;
    }
    if ("intellij".equals(normalized)) {
      return INTELLIJ;
    }

    return fallback;
  }
}

