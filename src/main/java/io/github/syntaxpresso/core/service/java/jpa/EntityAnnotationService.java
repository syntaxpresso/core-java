package io.github.syntaxpresso.core.service.java.jpa;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/**
 * Service for handling JPA @Entity annotations
 */
@RequiredArgsConstructor
public class EntityAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Adds @Entity annotation to a class
   *
   * @param tsFile the TypeScript file
   * @param classNode the class node
   * @param tableName optional table name
   * @return the created entity annotation node
   */
  public Optional<TSNode> addEntityAnnotation(TSFile tsFile, TSNode classNode, String tableName) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, classNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, classNode, annotationInsertionPoint, "@Entity");
    Optional<TSNode> entityAnnotationNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, classNode, "@Entity");

    if (entityAnnotationNode.isPresent() && !Strings.isNullOrEmpty(tableName)) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, entityAnnotationNode.get(), "name", tableName);
    }
    return entityAnnotationNode;
  }
}