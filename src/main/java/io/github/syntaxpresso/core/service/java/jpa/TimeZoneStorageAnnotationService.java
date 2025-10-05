package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/** Service for handling JPA @TimeZoneStorage annotations */
@RequiredArgsConstructor
public class TimeZoneStorageAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Adds arguments to a @TimeZoneStorage annotation based on provided parameters
   *
   * @param tsFile the TypeScript file
   * @param timeZoneStorageAnnotationNode the time zone storage annotation node
   * @param timeZoneStorageType the time zone storage type
   */
  public void addTimeZoneStorageAnnotationArguments(
      TSFile tsFile,
      TSNode timeZoneStorageAnnotationNode,
      JavaFieldTimeZoneStorage timeZoneStorageType) {
    if (timeZoneStorageType != null && !timeZoneStorageType.equals(JavaFieldTimeZoneStorage.AUTO)) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile,
              timeZoneStorageAnnotationNode,
              "value",
              "TimeZoneStorageType." + timeZoneStorageType.name());
    }
  }

  /**
   * Creates a @TimeZoneStorage annotation on a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created time zone storage annotation node
   */
  public Optional<TSNode> createTimeZoneStorageAnnotation(TSFile tsFile, TSNode fieldNode) {
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
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@TimeZoneStorage");
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
        .findAnnotationByName(tsFile, freshFieldNode.get(), "TimeZoneStorage");
  }

  /**
   * Determines if a @TimeZoneStorage annotation should be added
   *
   * @param fieldType the field type
   * @param fieldTypePackageName the field type package name
   * @param fieldTimeZoneStorage the time zone storage type
   * @return true if time zone storage annotation should be added
   */
  public boolean shouldAddTimeZoneStorageAnnotation(
      String fieldType,
      String fieldTypePackageName,
      JavaFieldTimeZoneStorage fieldTimeZoneStorage) {
    Set<String> timeZoneAwareTypes =
        Set.of("java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.time.OffsetTime");
    if (timeZoneAwareTypes.contains(fieldTypePackageName + "." + fieldType)
        && fieldTimeZoneStorage != null) {
      return true;
    }
    return false;
  }
}
