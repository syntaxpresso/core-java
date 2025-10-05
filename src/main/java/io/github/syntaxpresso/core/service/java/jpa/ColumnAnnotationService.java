package io.github.syntaxpresso.core.service.java.jpa;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/** Service for handling JPA-specific annotations */
@RequiredArgsConstructor
public class ColumnAnnotationService {
  private final JavaLanguageService javaLanguageService;

  private TSNode refreshColumnAnnotationNode(
      TSFile tsFile, TSNode columnAnnotationNode, String fieldName) {
    Optional<TSNode> publicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClass.isEmpty()) {
      return null;
    }
    Optional<TSNode> fieldNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .findFieldDeclarationNodeByName(tsFile, fieldName, publicClass.get());
    if (fieldNode.isEmpty()) {
      return null;
    }
    Optional<TSNode> annotationNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, fieldNode.get(), "Column");
    if (annotationNode.isEmpty()) {
      return null;
    }
    return annotationNode.get();
  }

  /**
   * Adds arguments to a @Column annotation based on provided parameters
   *
   * @param tsFile the TypeScript file
   * @param columnAnnotationNode the column annotation node
   * @param name the column name
   * @param unique whether the column is unique
   * @param nullable whether the column is nullable
   * @param insertable whether the column is insertable
   * @param updatable whether the column is updatable
   * @param length the column length
   * @param precision the column precision
   * @param scale the column scale
   * @param columnDefinition the column definition
   * @param table the table name
   */
  public void addColumnAnnotationArguments(
      TSFile tsFile,
      TSNode columnAnnotationNode,
      String name,
      Boolean unique,
      Boolean nullable,
      Boolean insertable,
      Boolean updatable,
      Integer length,
      Integer precision,
      Integer scale,
      String columnDefinition,
      String table) {
    if (!Strings.isNullOrEmpty(name)) {
      String snakeName = StringHelper.toSnakeCase(name);
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, columnAnnotationNode, "name", snakeName);
    }
    TSNode refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, columnAnnotationNode, name);
    if (unique != null && unique != false) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "unique", unique);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (nullable != null && nullable != true) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "nullable", nullable);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (insertable != null && insertable != true) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "insertable", insertable);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (updatable != null && updatable != true) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "updatable", updatable);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (length != null && length != 255) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "length", length);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (precision != null && precision > 0) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "precision", precision);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (scale != null && scale > 0) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "scale", scale);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (!Strings.isNullOrEmpty(table)) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(tsFile, refreshedColumnAnnotationNode, "table", table);
    }
    refreshedColumnAnnotationNode =
        this.refreshColumnAnnotationNode(tsFile, refreshedColumnAnnotationNode, name);
    if (!Strings.isNullOrEmpty(columnDefinition)) {
      this.javaLanguageService
          .getAnnotationService()
          .addAnnotationArgument(
              tsFile, refreshedColumnAnnotationNode, "columnDefinition", columnDefinition);
    }
  }

  /**
   * Creates a @Column annotation on a field
   *
   * @param tsFile the TypeScript file
   * @param fieldNode the field node
   * @return the created column annotation node
   */
  public Optional<TSNode> createColumnAnnotation(TSFile tsFile, TSNode fieldNode) {
    Optional<TSNode> publicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClass.isEmpty()) {
      return Optional.empty();
    }
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
        .addAnnotation(tsFile, fieldNode, annotationInsertionPoint, "@Column");
    publicClass = this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClass.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> freshFieldNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .findFieldDeclarationNodeByName(tsFile, fieldName, publicClass.get());
    if (freshFieldNode.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> columnNode =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, freshFieldNode.get(), "Column");
    return columnNode;
  }

  /**
   * Determines if a length argument should be added to @Column annotation
   *
   * @param fieldType the field type
   * @param fieldTypePackageName the field type package name
   * @param fieldLength the field length
   * @return true if length argument should be added
   */
  public boolean shouldAddLengthArgument(
      String fieldType, String fieldTypePackageName, Integer fieldLength) {
    Set<String> lengthTypes =
        Set.of(
            "java.lang.String",
            "java.net.URL",
            "java.util.Locale",
            "java.util.Currency",
            "java.lang.Class",
            "java.lang.Character[]",
            "char[]",
            "java.util.TimeZone",
            "java.time.ZoneOffset");
    if (lengthTypes.contains(fieldTypePackageName + "." + fieldType)
        && fieldLength != null
        && fieldLength != 255) {
      return true;
    }
    return false;
  }

  /**
   * Determines if precision and scale arguments should be added to @Column annotation
   *
   * @param fieldType the field type
   * @param fieldTypePackageName the field type package name
   * @param fieldPrecision the field precision
   * @param fieldScale the field scale
   * @return true if precision and scale arguments should be added
   */
  public boolean shouldAddPrecisionAndScaleArguments(
      String fieldType, String fieldTypePackageName, Integer fieldPrecision, Integer fieldScale) {
    Set<String> temporalTypes = Set.of("java.math.BigDecimal");
    if (temporalTypes.contains(fieldTypePackageName + "." + fieldType)
        && fieldPrecision != null
        && fieldScale != null) {
      return true;
    }
    return false;
  }

  /**
   * Determines if a nullable=false argument should be added to @Column annotation
   *
   * @param fieldNullable whether the field is nullable
   * @return true if nullable argument should be added
   */
  public boolean shouldAddMandatoryArgument(Boolean fieldNullable) {
    return !fieldNullable;
  }

  /**
   * Determines if a unique=true argument should be added to @Column annotation
   *
   * @param fieldUnique whether the field is unique
   * @return true if unique argument should be added
   */
  public boolean shouldAddUniqueArgument(Boolean fieldUnique) {
    return fieldUnique;
  }
}
