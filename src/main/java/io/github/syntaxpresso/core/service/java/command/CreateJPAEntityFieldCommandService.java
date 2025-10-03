package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.extra.JavaFieldModifier;
import io.github.syntaxpresso.core.command.extra.JavaFieldType;
import io.github.syntaxpresso.core.command.extra.JavaVisibilityModifier;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint.FieldInsertionPosition;
import io.github.syntaxpresso.core.util.StringHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateJPAEntityFieldCommandService {
  private final JavaLanguageService javaLanguageService;

  private boolean shouldAddLengthArgument(
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

  private boolean shouldAddTimeZoneStorageAnnotation(
      String fieldType, String fieldTypePackageName, Integer fieldTimeZoneStorage) {
    Set<String> timeZoneStorageTypes =
        Set.of("java.time.OffsetDateTime", "java.time.OffsetTime", "java.time.ZonedDateTime");
    if (timeZoneStorageTypes.contains(fieldTypePackageName + "." + fieldType)
        && fieldTimeZoneStorage != null) {
      return true;
    }
    return false;
  }

  private boolean shouldAddTemporalAnnotation(
      String fieldType, String fieldTypePackageName, Integer fieldTemporal) {
    Set<String> temporalTypes = Set.of("java.util.Date", "java.util.Calendar");
    if (temporalTypes.contains(fieldTypePackageName + "." + fieldType) && fieldTemporal != null) {
      return true;
    }
    return false;
  }

  private boolean shouldAddPrecisionAndScaleArguments(
      String fieldType, String fieldTypePackageName, Integer fieldPrecision, Integer fieldScale) {
    Set<String> temporalTypes = Set.of("java.math.BigDecimal");
    if (temporalTypes.contains(fieldTypePackageName + "." + fieldType)
        && fieldPrecision != null
        && fieldScale != null) {
      return true;
    }
    return false;
  }

  private boolean shouldAddLobAnnotation(Boolean fieldLargeObject) {
    return fieldLargeObject;
  }

  private boolean shouldAddMandatoryArgument(Boolean fieldNullable) {
    return !fieldNullable;
  }

  private boolean shouldAddUniqueArgument(Boolean fieldUnique) {
    return fieldUnique;
  }

  public void createBasicEntityField(
      String fieldName,
      String fieldType,
      String fieldTypePackageName,
      Integer fieldLength,
      Integer fieldPrecision,
      Integer fieldScale,
      Integer fieldTemporal,
      Integer fieldTimeZoneStorage,
      boolean fieldNullable,
      boolean fieldUnique,
      boolean fieldLargeObject) {
    String snakeCaseFieldName = StringHelper.toSnakeCase(fieldName);
    boolean shouldAddLengthArgument =
        this.shouldAddLengthArgument(fieldType, fieldTypePackageName, fieldLength);
    boolean shouldAddTimeZoneStorageAnnotation =
        this.shouldAddTimeZoneStorageAnnotation(
            fieldType, fieldTypePackageName, fieldTimeZoneStorage);
    boolean shouldAddTemporalAnnotation =
        this.shouldAddTemporalAnnotation(fieldType, fieldTypePackageName, fieldTemporal);
    boolean shouldAddPrecisionAndScaleArguments =
        this.shouldAddPrecisionAndScaleArguments(
            fieldType, fieldTypePackageName, fieldPrecision, fieldScale);
    boolean shouldAddLobAnnotation = this.shouldAddLobAnnotation(fieldLargeObject);
    boolean shouldAddMandatoryArgument = this.shouldAddMandatoryArgument(fieldNullable);
    boolean shouldAddUniqueArgument = this.shouldAddUniqueArgument(fieldUnique);
  }

  private boolean isJPAEntity(TSFile tsFile, TSNode publicClassNode) {
    Optional<TSNode> entityAnnotation = this.javaLanguageService
        .getAnnotationService()
        .findAnnotationByName(tsFile, publicClassNode, "Entity");
    return entityAnnotation.isPresent();
  }

  private List<String> generateJPAAnnotations(
      String fieldType, String fieldTypePackageName, Integer fieldLength, 
      Integer fieldPrecision, Integer fieldScale, Integer fieldTemporal, 
      Integer fieldTimeZoneStorage, boolean fieldNullable, boolean fieldUnique, 
      boolean fieldUpdatable, boolean fieldLargeObject, JavaFieldType type) {
    
    List<String> annotations = new ArrayList<>();
    
    switch (type) {
      case ID:
        annotations.add("@Id");
        annotations.add("@GeneratedValue(strategy = GenerationType.IDENTITY)");
        // ID fields typically don't need Column annotation, but if special constraints are needed
        if (!fieldUpdatable || fieldUnique) {
          annotations.add(this.buildColumnAnnotation(false, fieldUnique, fieldUpdatable, fieldLength));
        }
        break;
      case BASIC:
        annotations.add(this.buildColumnAnnotation(fieldNullable, fieldUnique, fieldUpdatable, fieldLength, 
            fieldPrecision, fieldScale));
        if (fieldLargeObject) {
          annotations.add("@Lob");
        }
        if (this.shouldAddTemporalAnnotation(fieldType, fieldTypePackageName, fieldTemporal)) {
          annotations.add("@Temporal(TemporalType." + this.getTemporalType(fieldTemporal) + ")");
        }
        if (this.shouldAddTimeZoneStorageAnnotation(fieldType, fieldTypePackageName, fieldTimeZoneStorage)) {
          annotations.add("@TimeZoneStorage(TimeZoneStorageType." + this.getTimeZoneStorageType(fieldTimeZoneStorage) + ")");
        }
        break;
      case ENUM:
        annotations.add(this.buildColumnAnnotation(fieldNullable, fieldUnique, fieldUpdatable, fieldLength));
        annotations.add("@Enumerated(EnumType.STRING)");
        break;
    }
    
    return annotations;
  }

  private String buildColumnAnnotation(boolean fieldNullable, boolean fieldUnique, 
      boolean fieldUpdatable, Integer fieldLength) {
    return this.buildColumnAnnotation(fieldNullable, fieldUnique, fieldUpdatable, fieldLength, null, null);
  }

  private String buildColumnAnnotation(boolean fieldNullable, boolean fieldUnique, 
      boolean fieldUpdatable, Integer fieldLength, Integer fieldPrecision, Integer fieldScale) {
    List<String> arguments = new ArrayList<>();
    
    if (!fieldNullable) {
      arguments.add("nullable = false");
    }
    if (fieldUnique) {
      arguments.add("unique = true");
    }
    if (!fieldUpdatable) {
      arguments.add("updatable = false");
    }
    if (fieldLength != null && fieldLength != 255) {
      arguments.add("length = " + fieldLength);
    }
    if (fieldPrecision != null && fieldScale != null) {
      arguments.add("precision = " + fieldPrecision);
      arguments.add("scale = " + fieldScale);
    }
    
    if (arguments.isEmpty()) {
      return "@Column";
    }
    return "@Column(" + String.join(", ", arguments) + ")";
  }

  private String getTemporalType(Integer fieldTemporal) {
    switch (fieldTemporal) {
      case 1: return "DATE";
      case 2: return "TIME";
      case 3: return "TIMESTAMP";
      default: return "TIMESTAMP";
    }
  }

  private String getTimeZoneStorageType(Integer fieldTimeZoneStorage) {
    switch (fieldTimeZoneStorage) {
      case 1: return "NATIVE";
      case 2: return "NORMALIZE";
      case 3: return "COLUMN";
      default: return "NATIVE";
    }
  }

  private void addJPAImports(TSFile tsFile, List<String> annotations, JavaFieldType type) {
    Optional<TSNode> packageDeclarationNode = this.javaLanguageService
        .getPackageDeclarationService()
        .getPackageDeclarationNode(tsFile);
    
    // Determine required imports based on type and annotations
    List<String> requiredImports = new ArrayList<>();
    
    if (type == JavaFieldType.ID) {
      requiredImports.add("javax.persistence.Id");
      requiredImports.add("javax.persistence.GeneratedValue");
      requiredImports.add("javax.persistence.GenerationType");
    } else if (type == JavaFieldType.ENUM) {
      requiredImports.add("javax.persistence.Column");
      requiredImports.add("javax.persistence.Enumerated");
      requiredImports.add("javax.persistence.EnumType");
    } else {
      requiredImports.add("javax.persistence.Column");
    }
    
    // Add conditional imports based on annotations
    for (String annotation : annotations) {
      if (annotation.contains("@Lob")) {
        requiredImports.add("javax.persistence.Lob");
      }
      if (annotation.contains("@Temporal")) {
        requiredImports.add("javax.persistence.Temporal");
        requiredImports.add("javax.persistence.TemporalType");
      }
      if (annotation.contains("@TimeZoneStorage")) {
        requiredImports.add("org.hibernate.annotations.TimeZoneStorage");
        requiredImports.add("org.hibernate.annotations.TimeZoneStorageType");
      }
    }
    
    // Add imports (removing duplicates)
    requiredImports.stream().distinct().forEach(importPath -> {
      String[] parts = importPath.split("\\.");
      String packageScope = importPath.substring(0, importPath.lastIndexOf('.'));
      String className = parts[parts.length - 1];
      
      this.javaLanguageService
          .getImportDeclarationService()
          .addImport(tsFile, packageScope, className, packageDeclarationNode.orElse(null));
    });
  }

  private String buildFullFieldType(String fieldType, String fieldTypePackageName) {
    if (fieldTypePackageName == null || fieldTypePackageName.isEmpty() || 
        fieldTypePackageName.equals("java.lang")) {
      return fieldType;
    }
    return fieldType; // Assume import will be added separately if needed
  }

  private String buildCompleteFieldDeclaration(List<String> annotations, String fieldType, String fieldName) {
    StringBuilder declaration = new StringBuilder();
    
    // Add each annotation on its own line with proper indentation
    for (String annotation : annotations) {
      declaration.append("  ").append(annotation).append("\n");
    }
    
    // Add the field declaration with proper indentation
    declaration.append("  private ").append(fieldType).append(" ").append(fieldName).append(";");
    
    return declaration.toString();
  }

  private DataTransferObject<Void> validateInputParameters(String fieldName, String fieldType, 
      String fieldTypePackageName, JavaFieldType type) {
    if (Strings.isNullOrEmpty(fieldName)) {
      return DataTransferObject.error("Field name cannot be null or empty.");
    }
    
    if (!this.isValidJavaIdentifier(fieldName)) {
      return DataTransferObject.error("Field name '" + fieldName + "' is not a valid Java identifier.");
    }
    
    if (Strings.isNullOrEmpty(fieldType)) {
      return DataTransferObject.error("Field type cannot be null or empty.");
    }
    
    if (type == null) {
      return DataTransferObject.error("Field type (BASIC, ENUM, ID) must be specified.");
    }
    
    return DataTransferObject.success();
  }

  private boolean isValidJavaIdentifier(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return false;
    }
    
    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
      return false;
    }
    
    for (int i = 1; i < identifier.length(); i++) {
      if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
        return false;
      }
    }
    
    // Check if it's a Java keyword
    Set<String> javaKeywords = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", 
        "class", "const", "continue", "default", "do", "double", "else", "enum", 
        "extends", "final", "finally", "float", "for", "goto", "if", "implements", 
        "import", "instanceof", "int", "interface", "long", "native", "new", 
        "package", "private", "protected", "public", "return", "short", "static", 
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", 
        "transient", "try", "void", "volatile", "while"
    );
    
    return !javaKeywords.contains(identifier);
  }

  private String getClassName(TSFile tsFile, TSNode publicClassNode) {
    Optional<TSNode> classNameNode = this.javaLanguageService
        .getClassDeclarationService()
        .getClassDeclarationNameNode(tsFile, publicClassNode);
    
    if (classNameNode.isPresent()) {
      return tsFile.getTextFromNode(classNameNode.get());
    }
    return "Unknown";
  }

  private boolean fieldAlreadyExists(TSFile tsFile, TSNode publicClassNode, String fieldName) {
    List<TSNode> fieldNodes = this.javaLanguageService
        .getClassDeclarationService()
        .getFieldDeclarationService()
        .getAllFieldDeclarationNodes(tsFile, publicClassNode);
    
    for (TSNode fieldNode : fieldNodes) {
      Optional<TSNode> fieldNameNode = this.javaLanguageService
          .getClassDeclarationService()
          .getFieldDeclarationService()
          .getFieldDeclarationNameNode(tsFile, fieldNode);
      
      if (fieldNameNode.isPresent()) {
        String existingFieldName = tsFile.getTextFromNode(fieldNameNode.get());
        if (fieldName.equals(existingFieldName)) {
          return true;
        }
      }
    }
    
    return false;
  }

  private boolean hasExistingIdField(TSFile tsFile, TSNode publicClassNode) {
    List<TSNode> fieldNodes = this.javaLanguageService
        .getClassDeclarationService()
        .getFieldDeclarationService()
        .getAllFieldDeclarationNodes(tsFile, publicClassNode);
    
    for (TSNode fieldNode : fieldNodes) {
      Optional<TSNode> idAnnotation = this.javaLanguageService
          .getAnnotationService()
          .findAnnotationByName(tsFile, fieldNode, "Id");
      
      if (idAnnotation.isPresent()) {
        return true;
      }
    }
    
    return false;
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
      Integer fieldTemporal,
      Integer fieldTimeZoneStorage,
      boolean fieldNullable,
      boolean fieldUnique,
      boolean fieldUpdatable,
      boolean fieldLargeObject,
      JavaFieldType type) {
    
    try {
      // Validate input parameters
      DataTransferObject<Void> validationResult = this.validateInputParameters(
          fieldName, fieldType, fieldTypePackageName, type);
      if (!validationResult.getSucceed()) {
        return validationResult;
      }
      
      TSFile tsFile = new TSFile(language, filePath);
      
      // 1. Find the target class in the file
      Optional<TSNode> publicClassNode = 
          this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
      if (publicClassNode.isEmpty()) {
        return DataTransferObject.error("No public class found in file: " + filePath);
      }
      
      // 2. Verify this is a JPA entity
      boolean isJPAEntity = this.isJPAEntity(tsFile, publicClassNode.get());
      if (!isJPAEntity) {
        String className = this.getClassName(tsFile, publicClassNode.get());
        return DataTransferObject.error("Class '" + className + "' is not a JPA entity. Add @Entity annotation first.");
      }
      
      // 3. Check if field already exists
      if (this.fieldAlreadyExists(tsFile, publicClassNode.get(), fieldName)) {
        String className = this.getClassName(tsFile, publicClassNode.get());
        return DataTransferObject.error("Field '" + fieldName + "' already exists in class '" + className + "'.");
      }
      
      // 4. For ID fields, check if an ID field already exists
      if (type == JavaFieldType.ID && this.hasExistingIdField(tsFile, publicClassNode.get())) {
        String className = this.getClassName(tsFile, publicClassNode.get());
        return DataTransferObject.error("Class '" + className + "' already has an @Id field. Only one @Id field is allowed per entity.");
      }
      
      // 5. Generate JPA annotations based on the field parameters
      List<String> annotations = this.generateJPAAnnotations(
          fieldType, fieldTypePackageName, fieldLength, fieldPrecision, fieldScale, 
          fieldTemporal, fieldTimeZoneStorage, fieldNullable, fieldUnique, 
          fieldUpdatable, fieldLargeObject, type);
      
      // 6. Add necessary imports for JPA annotations FIRST (this changes byte positions and invalidates nodes)
      this.addJPAImports(tsFile, annotations, type);
      
      // 7. Re-fetch the public class node after imports were added (tree structure changed)
      Optional<TSNode> updatedPublicClassNode = 
          this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
      if (updatedPublicClassNode.isEmpty()) {
        return DataTransferObject.error("Could not re-locate public class after adding imports");
      }
      
      // 8. Determine the appropriate field insertion position AFTER imports are added
      FieldInsertionPoint insertionPoint = this.javaLanguageService
          .getClassDeclarationService()
          .getFieldDeclarationService()
          .getFieldInsertionPosition(tsFile, updatedPublicClassNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
      
      if (insertionPoint == null) {
        return DataTransferObject.error("Could not determine where to insert the field in the class.");
      }
      
      // 9. Build the complete field declaration with annotations
      String fullFieldType = this.buildFullFieldType(fieldType, fieldTypePackageName);
      String completeFieldDeclaration = this.buildCompleteFieldDeclaration(
          annotations, fullFieldType, fieldName);
      
      // 10. Insert the complete field at the insertion point
      String insertText = completeFieldDeclaration;
      if (insertionPoint.isBreakLineBefore()) {
        insertText = "\n" + insertText;
      }
      if (insertionPoint.isBreakLineAfter()) {
        insertText = insertText + "\n";
      }
      
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), insertText);
      
      // Save changes to disk
      try {
        tsFile.save();
      } catch (IOException e) {
        return DataTransferObject.error("Failed to save changes to file: " + e.getMessage());
      }
      
      return DataTransferObject.success();
    } catch (Exception e) {
      return DataTransferObject.error("Failed to create JPA entity field '" + fieldName + "': " + e.getMessage());
    }
  }
}
