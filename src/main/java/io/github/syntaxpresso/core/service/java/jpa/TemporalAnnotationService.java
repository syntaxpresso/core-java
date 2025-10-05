package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/** Service for handling JPA @Temporal annotations */
@RequiredArgsConstructor
public class TemporalAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Adds arguments to a @Temporal annotation based on provided parameters
   *
   * @param tsFile the TypeScript file
   * @param temporalAnnotationNode the temporal annotation node
   * @param temporalType the temporal type
   */
  public void addTemporalAnnotationArguments(
      TSFile tsFile, TSNode temporalAnnotationNode, JavaFieldTemporal temporalType) {
    if (temporalType != null) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile, temporalAnnotationNode, "value", "TemporalType." + temporalType.name());
    }
  }

  /**
   * Creates a @Temporal annotation on a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created temporal annotation node
   */
  public Optional<TSNode> createTemporalAnnotation(TSFile tsFile, TSNode fieldNode) {
    Optional<TSNode> fieldNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .getFieldDeclarationNameNode(tsFile, fieldNode);
    if (fieldNameNode.isEmpty()) {
      return Optional.empty();
    }
    String fieldName = tsFile.getTextFromNode(fieldNameNode.get());
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Temporal");
    Optional<TSNode> freshPublicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (freshPublicClass.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> freshFieldNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .findFieldDeclarationNodeByName(tsFile, fieldName, freshPublicClass.get());
    if (freshFieldNode.isEmpty()) {
      return Optional.empty();
    }
    return this.javaLanguageService
        .getAnnotationService()
        .findAnnotationByName(tsFile, freshFieldNode.get(), "Temporal");
  }

  /**
   * Determines if a @Temporal annotation should be added
   *
   * @param fieldType the field type
   * @param fieldTypePackageName the field type package name
   * @param fieldTemporal the temporal type
   * @return true if temporal annotation should be added
   */
  public boolean shouldAddTemporalAnnotation(
      String fieldType, String fieldTypePackageName, JavaFieldTemporal fieldTemporal) {
    Set<String> temporalTypes = Set.of("java.util.Date", "java.util.Calendar", "java.sql.Date");
    if (temporalTypes.contains(fieldTypePackageName + "." + fieldType) && fieldTemporal != null) {
      return true;
    }
    return false;
  }
}
