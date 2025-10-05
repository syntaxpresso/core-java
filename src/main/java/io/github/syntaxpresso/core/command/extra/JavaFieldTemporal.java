package io.github.syntaxpresso.core.command.extra;

import java.util.Arrays;

/** Represents the temporal type of a field, such as date, time, or timestamp. */
public enum JavaFieldTemporal {
  DATE("DATE"),
  TIME("TIME"),
  TIMESTAMP("TIMESTAMP");

  private final String value;

  /**
   * Private constructor to assign the string value.
   *
   * @param value The string representation of the enum constant.
   */
  JavaFieldTemporal(String value) {
    this.value = value;
  }

  /**
   * Gets the string value of the enum constant.
   *
   * @return The string value.
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Finds a FieldTemporal constant that matches the given string value.
   *
   * <p>This lookup is case-sensitive.
   *
   * @param value The string value to find.
   * @return The corresponding FieldTemporal enum constant.
   * @throws IllegalArgumentException if no matching constant is found for the given value.
   */
  public static JavaFieldTemporal fromValue(String value) {
    return Arrays.stream(values())
        .filter(member -> member.value.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException("No matching enum member for value '" + value + "'"));
  }
}
