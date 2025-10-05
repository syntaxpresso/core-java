package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/** Service for handling JPA @Lob annotations */
@RequiredArgsConstructor
public class LobAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Creates a @Lob annotation on a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created lob annotation node
   */
  public Optional<TSNode> createLobAnnotation(TSFile tsFile, TSNode fieldNode) {
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
                tsFile, fieldNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Lob");
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
        .findAnnotationByName(tsFile, freshFieldNode.get(), "@Lob");
  }

  /**
   * Determines if a @Lob annotation should be added
   *
   * @param fieldType the field type
   * @param fieldTypePackageName the field type package name
   * @return true if lob annotation should be added
   */
  public boolean shouldAddLobAnnotation(String fieldType, String fieldTypePackageName) {
    Set<String> lobTypes =
        Set.of("java.lang.String", "byte[]", "java.lang.Byte[]", "char[]", "java.lang.Character[]");
    String fullTypeName = fieldTypePackageName + "." + fieldType;
    if (lobTypes.contains(fullTypeName) || lobTypes.contains(fieldType)) {
      return true;
    }
    return false;
  }
}
