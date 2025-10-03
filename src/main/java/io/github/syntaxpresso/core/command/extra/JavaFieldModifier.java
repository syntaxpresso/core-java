package io.github.syntaxpresso.core.command.extra;

/**
 * Enum representing Java field modifiers (excluding visibility modifiers).
 * 
 * <p>This enum defines non-visibility modifiers that can be applied to Java fields:
 * <ul>
 *   <li>STATIC - Field belongs to the class rather than instances</li>
 *   <li>FINAL - Field value cannot be changed after initialization</li>
 *   <li>TRANSIENT - Field is not serialized</li>
 *   <li>VOLATILE - Field access is synchronized across threads</li>
 * </ul>
 */
public enum JavaFieldModifier {
  /** Static modifier - field belongs to the class */
  STATIC("static"),
  
  /** Final modifier - field cannot be reassigned */
  FINAL("final"),
  
  /** Transient modifier - field is not serialized */
  TRANSIENT("transient"),
  
  /** Volatile modifier - field access is thread-safe */
  VOLATILE("volatile");
  
  private final String keyword;
  
  JavaFieldModifier(String keyword) {
    this.keyword = keyword;
  }
  
  /**
   * Gets the Java keyword for this field modifier.
   * 
   * @return The keyword string
   */
  public String getKeyword() {
    return keyword;
  }
  
  @Override
  public String toString() {
    return keyword;
  }
}