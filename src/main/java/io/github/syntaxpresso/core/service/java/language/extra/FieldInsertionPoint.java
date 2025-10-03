package io.github.syntaxpresso.core.service.java.language.extra;

import lombok.Data;

/**
 * Represents an insertion point for adding field declarations to Java classes.
 * 
 * <p>This class defines where and how a new field declaration should be inserted
 * into a class, following the same pattern as {@link AnnotationInsertionPoint}.
 */
@Data
public class FieldInsertionPoint {
  private boolean breakLineBefore = false;
  private boolean breakLineAfter = true;  // Fields typically need newline after
  
  /** Defines the possible insertion positions for a field declaration. */
  public enum FieldInsertionPosition {
    /** After the last existing field declaration in the class. */
    AFTER_LAST_FIELD,
    
    /** Before the first method declaration in the class. */
    BEFORE_FIRST_METHOD,
    
    /** At the end of the class body, before the closing brace. */
    END_OF_CLASS_BODY
  }
  
  int insertByte;
  FieldInsertionPosition position;
}