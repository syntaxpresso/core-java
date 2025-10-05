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
 * Service for handling JPA @Id and @GeneratedValue annotations
 */
@RequiredArgsConstructor
public class IdAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Adds @Id annotation to a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created id annotation node
   */
  public Optional<TSNode> addIdAnnotation(TSFile tsFile, TSNode fieldNode) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Id");
    return this.javaLanguageService
        .getAnnotationService()
        .findAnnotationByName(tsFile, fieldNode, "@Id");
  }

  /**
   * Adds @GeneratedValue annotation to a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @param strategy the generation strategy
   * @return the created generated value annotation node
   */
  public Optional<TSNode> addGeneratedValueAnnotation(
      TSFile tsFile, TSNode fieldNode, String strategy) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@GeneratedValue");
    Optional<TSNode> generatedValueAnnotationNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, fieldNode, "@GeneratedValue");

    if (generatedValueAnnotationNode.isPresent() && !Strings.isNullOrEmpty(strategy)) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile, generatedValueAnnotationNode.get(), "strategy", "GenerationType." + strategy);
    }
    return generatedValueAnnotationNode;
  }
}