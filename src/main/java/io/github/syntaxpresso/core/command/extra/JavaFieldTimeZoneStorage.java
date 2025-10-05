package io.github.syntaxpresso.core.command.extra;

import java.util.Arrays;

/** Defines strategies for handling time zone information in date-time fields. */
public enum JavaFieldTimeZoneStorage {
  NATIVE("NATIVE"),
  NORMALIZE("NORMALIZE"),
  NORMALIZE_UTC("NORMALIZE_UTC"),
  COLUMN("COLUMN"),
  AUTO("AUTO");

  private final String value;

  /**
   * Constructor for the enum.
   *
   * @param value The string representation of the enum constant.
   */
  JavaFieldTimeZoneStorage(String value) {
    this.value = value;
  }

  /**
   * Gets the string value associated with the enum constant.
   *
   * @return The string value.
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Finds an enum constant that matches the given string value.
   *
   * <p>This method is case-sensitive.
   *
   * @param value The string value to look up.
   * @return The corresponding FieldTimeZoneStorage enum constant.
   * @throws IllegalArgumentException if no matching constant is found.
   */
  public static JavaFieldTimeZoneStorage fromValue(String value) {
    return Arrays.stream(values())
        .filter(member -> member.value.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException("No matching enum member for value '" + value + "'"));
  }
}
