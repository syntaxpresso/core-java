package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/**
 * Service for handling JPA field-specific annotations
 */
@RequiredArgsConstructor
public class FieldAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Adds @Temporal annotation to a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @param temporalType the temporal type
   * @return the created temporal annotation node
   */
  public Optional<TSNode> addTemporalAnnotation(
      TSFile tsFile, TSNode fieldNode, JavaFieldTemporal temporalType) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Temporal");
    Optional<TSNode> temporalAnnotationNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, fieldNode, "@Temporal");

    if (temporalAnnotationNode.isPresent()) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile, temporalAnnotationNode.get(), "value", "TemporalType." + temporalType.name());
    }
    return temporalAnnotationNode;
  }

  /**
   * Adds @Lob annotation to a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created lob annotation node
   */
  public Optional<TSNode> addLobAnnotation(TSFile tsFile, TSNode fieldNode) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Lob");
    return this.javaLanguageService
        .getAnnotationService()
        .findAnnotationByName(tsFile, fieldNode, "@Lob");
  }

  /**
   * Adds @TimeZoneStorage annotation to a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @param timeZoneStorage the timezone storage type
   * @return the created timezone storage annotation node
   */
  public Optional<TSNode> addTimeZoneStorageAnnotation(
      TSFile tsFile, TSNode fieldNode, JavaFieldTimeZoneStorage timeZoneStorage) {
    AnnotationInsertionPoint annotationInsertionPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@TimeZoneStorage");
    Optional<TSNode> timeZoneStorageAnnotationNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, fieldNode, "@TimeZoneStorage");

    if (timeZoneStorageAnnotationNode.isPresent()) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile,
              timeZoneStorageAnnotationNode.get(),
              "value",
              "TimeZoneStorageType." + timeZoneStorage.name());
    }
    return timeZoneStorageAnnotationNode;
  }
}