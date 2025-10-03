package io.github.syntaxpresso.core.command.extra;

/**
 * Enum representing Java visibility modifiers for field and method declarations.
 * 
 * <p>This enum defines the four visibility levels available in Java:
 * <ul>
 *   <li>PUBLIC - Accessible from anywhere</li>
 *   <li>PRIVATE - Accessible only within the same class</li>
 *   <li>PROTECTED - Accessible within the same package and subclasses</li>
 *   <li>PACKAGE_PRIVATE - Accessible only within the same package (no modifier)</li>
 * </ul>
 */
public enum JavaVisibilityModifier {
  /** Public visibility - accessible from anywhere */
  PUBLIC("public"),
  
  /** Private visibility - accessible only within the same class */
  PRIVATE("private"),
  
  /** Protected visibility - accessible within package and subclasses */
  PROTECTED("protected"),
  
  /** Package-private visibility - no explicit modifier keyword */
  PACKAGE_PRIVATE("");
  
  private final String keyword;
  
  JavaVisibilityModifier(String keyword) {
    this.keyword = keyword;
  }
  
  /**
   * Gets the Java keyword for this visibility modifier.
   * 
   * @return The keyword string, or empty string for package-private
   */
  public String getKeyword() {
    return keyword;
  }
  
  /**
   * Checks if this modifier has an explicit keyword.
   * 
   * @return True if the modifier has a keyword, false for package-private
   */
  public boolean hasKeyword() {
    return !keyword.isEmpty();
  }
  
  @Override
  public String toString() {
    return keyword.isEmpty() ? "package-private" : keyword;
  }
}