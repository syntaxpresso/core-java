package io.github.syntaxpresso.core.service.java.command;

import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.command.extra.JavaFieldType;
import io.github.syntaxpresso.core.command.extra.JavaVisibilityModifier;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JPAAnnotationService;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint.FieldInsertionPosition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateJPAEntityFieldCommandService {
  private final JavaLanguageService javaLanguageService;
  private final JPAAnnotationService jpaAnnotationService;
  private Set<Map<String, String>> importsToAdd = new HashSet<>();
  private Set<Map<String, String>> columnAnnotationArgs = new HashSet<>();

  /**
   * Adds an import to the imports collection
   *
   * @param packageName the package name to import
   * @param className the class name to import
   */
  private void addToImports(String packageName, String className) {
    Map<String, String> importEntry = Map.of("package", packageName, "class", className);
    this.importsToAdd.add(importEntry);
  }

  private void processImports(TSFile tsFile) {
    Optional<TSNode> packageDeclarationNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageDeclarationNode.isPresent()) {
      for (Map<String, String> importMap : this.importsToAdd) {
        this.javaLanguageService
            .getImportDeclarationService()
            .addImport(
                tsFile,
                importMap.get("package"),
                importMap.get("class"),
                packageDeclarationNode.get());
      }
    }
  }

  private Optional<TSNode> createField(
      TSFile tsFile, TSNode classNode, String fieldType, String fieldName) {
    FieldInsertionPoint fieldInsertionPoint =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .getFieldInsertionPosition(tsFile, classNode, FieldInsertionPosition.END_OF_CLASS_BODY);
    this.javaLanguageService
        .getClassDeclarationService()
        .getFieldDeclarationService()
        .addFieldDeclaration(
            tsFile,
            classNode,
            fieldInsertionPoint,
            JavaVisibilityModifier.PUBLIC,
            null,
            fieldType,
            fieldName,
            null);
    Optional<TSNode> publicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClass.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> fieldNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .findFieldDeclarationNodeByName(tsFile, fieldName, publicClass.get());
    return fieldNode;
  }

  private void createBasicEntityField(
      TSFile tsFile,
      String fieldName,
      String fieldType,
      String fieldTypePackageName,
      Integer fieldLength,
      Integer fieldPrecision,
      Integer fieldScale,
      JavaFieldTemporal fieldTemporal,
      JavaFieldTimeZoneStorage fieldTimeZoneStorage,
      Boolean fieldNullable,
      Boolean fieldUnique,
      Boolean fieldLargeObject) {
    Optional<TSNode> publicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClassNode.isEmpty()) {
      return;
    }
    Optional<TSNode> fieldNode =
        this.createField(tsFile, publicClassNode.get(), fieldType, fieldName);
    if (fieldNode.isEmpty()) {
      return;
    }
    Optional<TSNode> columnAnnotationNode =
        this.jpaAnnotationService
            .getColumnAnnotationService()
            .createColumnAnnotation(tsFile, fieldNode.get());
    if (columnAnnotationNode.isEmpty()) {
      return;
    }
    this.addToImports("jakarta.persistence", "Column");
    this.jpaAnnotationService
        .getColumnAnnotationService()
        .addColumnAnnotationArguments(
            tsFile,
            columnAnnotationNode.get(),
            fieldName,
            fieldUnique,
            fieldNullable,
            null,
            null,
            fieldLength,
            fieldPrecision,
            fieldScale,
            null,
            null);
    fieldNode = this.refreshFieldNode(tsFile, fieldName);
    if (fieldNode.isEmpty()) {
      return;
    }
    this.addToImports(fieldTypePackageName, fieldType);
    boolean shouldAddTimeZoneStorageAnnotation =
        this.jpaAnnotationService
            .getTimeZoneStorageAnnotationService()
            .shouldAddTimeZoneStorageAnnotation(
                fieldType, fieldTypePackageName, fieldTimeZoneStorage);
    if (shouldAddTimeZoneStorageAnnotation) {
      Optional<TSNode> timeZoneStorageAnnotationNode =
          this.jpaAnnotationService
              .getTimeZoneStorageAnnotationService()
              .createTimeZoneStorageAnnotation(tsFile, fieldNode.get());
      this.addToImports("org.hibernate.annotations", "TimeZoneStorage");
      this.addToImports("org.hibernate.annotations", "TimeZoneStorageType");
      this.addToImports("java.time", "OffsetDateTime");
      this.addToImports("java.time", "OffsetDateTime");
      this.jpaAnnotationService
          .getTimeZoneStorageAnnotationService()
          .addTimeZoneStorageAnnotationArguments(
              tsFile, timeZoneStorageAnnotationNode.get(), fieldTimeZoneStorage);
      fieldNode = this.refreshFieldNode(tsFile, fieldName);
      if (fieldNode.isEmpty()) {
        return;
      }
    }
    boolean shouldAddTemporalAnnotation =
        this.jpaAnnotationService
            .getTemporalAnnotationService()
            .shouldAddTemporalAnnotation(fieldType, fieldTypePackageName, fieldTemporal);
    if (shouldAddTemporalAnnotation) {
      Optional<TSNode> temporalAnnotationNode =
          this.jpaAnnotationService
              .getTemporalAnnotationService()
              .createTemporalAnnotation(tsFile, fieldNode.get());
      this.addToImports("jakarta.persistence", "Temporal");
      this.addToImports("jakarta.persistence", "TemporalType");
      this.addToImports("java.sql", "Date");
      this.jpaAnnotationService
          .getTemporalAnnotationService()
          .addTemporalAnnotationArguments(tsFile, temporalAnnotationNode.get(), fieldTemporal);
      fieldNode = this.refreshFieldNode(tsFile, fieldName);
      if (fieldNode.isEmpty()) {
        return;
      }
    }
    boolean shouldAddLobAnnotation =
        this.jpaAnnotationService
            .getLobAnnotationService()
            .shouldAddLobAnnotation(fieldType, fieldTypePackageName);
    if (shouldAddLobAnnotation && fieldLargeObject) {
      this.addToImports("jakarta.persistence", "Lob");
      this.jpaAnnotationService
          .getLobAnnotationService()
          .createLobAnnotation(tsFile, fieldNode.get());
    }
    this.processImports(tsFile);
  }

  public DataTransferObject<Void> run(
      Path cwd,
      Path filePath,
      SupportedLanguage language,
      SupportedIDE ide,
      String fieldName,
      String fieldType,
      String fieldTypePackageName,
      Integer fieldLength,
      Integer fieldPrecision,
      Integer fieldScale,
      JavaFieldTemporal fieldTemporal,
      JavaFieldTimeZoneStorage fieldTimeZoneStorage,
      boolean fieldNullable,
      boolean fieldUnique,
      boolean fieldUpdatable,
      boolean fieldLargeObject,
      JavaFieldType type) {
    this.importsToAdd.clear();
    this.columnAnnotationArgs.clear();
    TSFile tsFile = new TSFile(language, filePath);
    if (type.equals(JavaFieldType.BASIC)) {
      this.createBasicEntityField(
          tsFile,
          fieldName,
          fieldType,
          fieldTypePackageName,
          fieldLength,
          fieldPrecision,
          fieldScale,
          fieldTemporal,
          fieldTimeZoneStorage,
          fieldNullable,
          fieldUnique,
          fieldLargeObject);
    }
    try {
      tsFile.save();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    return DataTransferObject.success();
  }

  /**
   * Refreshes the field node after tree modifications to ensure it points to the current tree state
   *
   * @param tsFile the TSFile containing the source code
   * @param fieldName the name of the field to find
   * @return Optional containing the fresh field node, or empty if not found
   */
  private Optional<TSNode> refreshFieldNode(TSFile tsFile, String fieldName) {
    Optional<TSNode> publicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClass.isEmpty()) {
      return Optional.empty();
    }
    return this.javaLanguageService
        .getClassDeclarationService()
        .getFieldDeclarationService()
        .findFieldDeclarationNodeByName(tsFile, fieldName, publicClass.get());
  }
}
