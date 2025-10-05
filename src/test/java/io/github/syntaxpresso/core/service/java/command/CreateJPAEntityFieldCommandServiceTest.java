package io.github.syntaxpresso.core.service.java.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.command.extra.JavaFieldType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.JPAAnnotationService;
import io.github.syntaxpresso.core.service.java.language.AnnotationDeclarationService;
import io.github.syntaxpresso.core.service.java.language.AnnotationService;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.EnumDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FormalParameterService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.InterfaceDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.RecordDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration tests for CreateJPAEntityFieldCommandService.
 *
 * <p>Tests actual file modifications with real JPA entity files to ensure the service correctly:
 * - Validates input parameters and entity files
 * - Generates appropriate JPA annotations for different field types
 * - Handles import statement management
 * - Inserts fields at correct positions within classes
 * - Preserves existing code structure and formatting
 */
@DisplayName("CreateJPAEntityFieldCommandService Integration Tests")
class CreateJPAEntityFieldCommandServiceTest {

  @TempDir Path tempDir;
  
  private CreateJPAEntityFieldCommandService service;
  private JavaLanguageService javaLanguageService;

  @BeforeEach
  void setUp() {
    // Set up all JavaLanguageService dependencies
    PathHelper pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    FormalParameterService formalParameterService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterService);
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService(pathHelper);
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService();
    EnumDeclarationService enumDeclarationService = new EnumDeclarationService();
    RecordDeclarationService recordDeclarationService = new RecordDeclarationService();
    AnnotationDeclarationService annotationDeclarationService = new AnnotationDeclarationService();
    AnnotationService annotationService = new AnnotationService();
    InterfaceDeclarationService interfaceDeclarationService = new InterfaceDeclarationService();

    javaLanguageService = new JavaLanguageService(
        pathHelper,
        variableNamingService,
        classDeclarationService,
        interfaceDeclarationService,
        enumDeclarationService,
        recordDeclarationService,
        annotationDeclarationService,
        packageDeclarationService,
        importDeclarationService,
        localVariableDeclarationService,
        annotationService,
        fieldDeclarationService);
    
    // StringHelper is not needed as dependency since it's used statically
    JPAAnnotationService jpaAnnotationService = null; // Mock not needed for these tests
    service = new CreateJPAEntityFieldCommandService(javaLanguageService, jpaAnnotationService);
  }

  @Nested
  @DisplayName("Basic field creation tests")
  class BasicFieldCreationTests {

    @Test
    @DisplayName("Should create basic String field with default column annotation")
    void shouldCreateBasicStringFieldWithDefaultColumnAnnotation() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          import javax.persistence.Table;
          
          @Entity
          @Table(name = "users")
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "username", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed(), "Service should succeed: " + result.getErrorReason());
      String modifiedContent = Files.readString(entityFile);
      
      // Debug: Print the actual file content

      
      assertAll("Field creation validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Column"),
          () -> assertThat(modifiedContent).contains("@Column"),
          () -> assertThat(modifiedContent).contains("private String username;"),
          () -> assertThat(modifiedContent).doesNotContain("nullable = false"),
          () -> assertThat(modifiedContent).doesNotContain("unique = true")
      );
    }

    @Test
    @DisplayName("Should create String field with length constraint")
    void shouldCreateStringFieldWithLengthConstraint() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "email", "String", "java.lang",
          100, null, null, null, null,
          false, true, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("String field with constraints",
          () -> assertThat(modifiedContent).contains("@Column(nullable = false, unique = true, length = 100)"),
          () -> assertThat(modifiedContent).contains("private String email;")
      );
    }

    @Test
    @DisplayName("Should create BigDecimal field with precision and scale")
    void shouldCreateBigDecimalFieldWithPrecisionAndScale() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("Product.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class Product {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "price", "BigDecimal", "java.math",
          null, 10, 2, null, null,
          false, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("BigDecimal field with precision and scale",
          () -> assertThat(modifiedContent).contains("@Column(nullable = false, precision = 10, scale = 2)"),
          () -> assertThat(modifiedContent).contains("private BigDecimal price;")
      );
    }

    @Test
    @DisplayName("Should create temporal field with Temporal annotation")
    void shouldCreateTemporalFieldWithTemporalAnnotation() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("Event.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class Event {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "eventDate", "Date", "java.util",
          null, null, null, JavaFieldTemporal.DATE, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("Temporal field validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Temporal"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.TemporalType"),
          () -> assertThat(modifiedContent).contains("@Column"),
          () -> assertThat(modifiedContent).contains("@Temporal(TemporalType.DATE)"),
          () -> assertThat(modifiedContent).contains("private Date eventDate;")
      );
    }

    @Test
    @DisplayName("Should create large object field with Lob annotation")
    void shouldCreateLargeObjectFieldWithLobAnnotation() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("Document.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class Document {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "content", "String", null,
          null, null, null, null, null,
          true, false, true, true,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("Large object field validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Lob"),
          () -> assertThat(modifiedContent).contains("@Column"),
          () -> assertThat(modifiedContent).contains("@Lob"),
          () -> assertThat(modifiedContent).contains("private String content;")
      );
    }
  }

  @Nested
  @DisplayName("ID field creation tests")
  class IdFieldCreationTests {

    @Test
    @DisplayName("Should create ID field with proper annotations")
    void shouldCreateIdFieldWithProperAnnotations() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "id", "Long", null,
          null, null, null, null, null,
          false, false, false, false,
          JavaFieldType.ID);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("ID field validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Id"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.GeneratedValue"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.GenerationType"),
          () -> assertThat(modifiedContent).contains("@Id"),
          () -> assertThat(modifiedContent).contains("@GeneratedValue(strategy = GenerationType.IDENTITY)"),
          () -> assertThat(modifiedContent).contains("private Long id;")
      );
    }

    @Test
    @DisplayName("Should reject duplicate ID field creation")
    void shouldRejectDuplicateIdFieldCreation() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          import javax.persistence.Id;
          
          @Entity
          public class User {
            @Id
            private Long existingId;
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "newId", "Long", null,
          null, null, null, null, null,
          false, false, false, false,
          JavaFieldType.ID);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("already has an @Id field");
    }
  }

  @Nested
  @DisplayName("Enum field creation tests")
  class EnumFieldCreationTests {

    @Test
    @DisplayName("Should create enum field with proper annotations")
    void shouldCreateEnumFieldWithProperAnnotations() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("Order.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class Order {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "status", "OrderStatus", "com.example.enums",
          null, null, null, null, null,
          false, false, true, false,
          JavaFieldType.ENUM);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("Enum field validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Column"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.Enumerated"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.EnumType"),
          () -> assertThat(modifiedContent).contains("@Column(nullable = false)"),
          () -> assertThat(modifiedContent).contains("@Enumerated(EnumType.STRING)"),
          () -> assertThat(modifiedContent).contains("private OrderStatus status;")
      );
    }
  }

  @Nested
  @DisplayName("Field insertion position tests")
  class FieldInsertionPositionTests {

    @Test
    @DisplayName("Should insert field after existing fields")
    void shouldInsertFieldAfterExistingFields() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          import javax.persistence.Id;
          
          @Entity
          public class User {
            @Id
            private Long id;
            
            private String name;
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "email", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      
      // Verify field order by checking indices
      int nameIndex = modifiedContent.indexOf("private String name;");
      int emailIndex = modifiedContent.indexOf("private String email;");
      assertTrue(nameIndex < emailIndex, "New field should be inserted after existing fields");
    }

    @Test
    @DisplayName("Should handle class with methods after fields")
    void shouldHandleClassWithMethodsAfterFields() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
            private String name;
            
            public String getName() {
              return this.name;
            }
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "email", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      
      // Verify email field is inserted before the method
      int emailIndex = modifiedContent.indexOf("private String email;");
      int methodIndex = modifiedContent.indexOf("public String getName()");
      assertTrue(emailIndex < methodIndex, "Field should be inserted before methods");
    }
  }

  @Nested
  @DisplayName("Error handling tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should reject non-JPA entity class")
    void shouldRejectNonJpaEntityClass() throws IOException {
      // Given
      Path javaFile = tempDir.resolve("RegularClass.java");
      Files.writeString(javaFile, """
          package com.example;
          
          public class RegularClass {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "field", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("is not a JPA entity");
    }

    @Test
    @DisplayName("Should reject duplicate field names")
    void shouldRejectDuplicateFieldNames() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
            private String existingField;
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "existingField", "Integer", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("already exists");
    }

    @Test
    @DisplayName("Should reject invalid Java identifiers")
    void shouldRejectInvalidJavaIdentifiers() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "123invalid", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("not a valid Java identifier");
    }

    @Test
    @DisplayName("Should reject Java keywords as field names")
    void shouldRejectJavaKeywordsAsFieldNames() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "class", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("not a valid Java identifier");
    }

    @Test
    @DisplayName("Should handle missing public class")
    void shouldHandleMissingPublicClass() throws IOException {
      // Given
      Path javaFile = tempDir.resolve("EmptyFile.java");
      Files.writeString(javaFile, """
          package com.example;
          
          // Empty file with no classes
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "field", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result.getSucceed());
      assertThat(result.getErrorReason()).contains("No public class found");
    }

    @Test
    @DisplayName("Should handle null and empty field names")
    void shouldHandleNullAndEmptyFieldNames() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When - null field name
      DataTransferObject<Void> result1 = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          null, "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // When - empty field name
      DataTransferObject<Void> result2 = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertFalse(result1.getSucceed());
      assertThat(result1.getErrorReason()).contains("Field name cannot be null or empty");
      
      assertFalse(result2.getSucceed());
      assertThat(result2.getErrorReason()).contains("Field name cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("Import management tests")
  class ImportManagementTests {

    @Test
    @DisplayName("Should add required imports for JPA annotations")
    void shouldAddRequiredImportsForJpaAnnotations() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          
          @Entity
          public class User {
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "createdAt", "Date", "java.util",
          null, null, null, JavaFieldTemporal.TIMESTAMP, null, // temporal field
          false, false, false, true, // large object
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      assertAll("Import management validation",
          () -> assertThat(modifiedContent).contains("import javax.persistence.Column"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.Lob"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.Temporal"),
          () -> assertThat(modifiedContent).contains("import javax.persistence.TemporalType")
      );
    }

    @Test
    @DisplayName("Should not duplicate existing imports")
    void shouldNotDuplicateExistingImports() throws IOException {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      Files.writeString(entityFile, """
          package com.example.entity;
          
          import javax.persistence.Entity;
          import javax.persistence.Column;
          import javax.persistence.Id;
          
          @Entity
          public class User {
            @Id
            private Long id;
          }
          """);

      // When
      DataTransferObject<Void> result = service.run(
          tempDir, entityFile, SupportedLanguage.JAVA, SupportedIDE.NONE,
          "name", "String", null,
          null, null, null, null, null,
          true, false, true, false,
          JavaFieldType.BASIC);

      // Then
      assertTrue(result.getSucceed());
      String modifiedContent = Files.readString(entityFile);
      
      // Count occurrences of Column import
      int columnImportCount = modifiedContent.split("import javax.persistence.Column", -1).length - 1;
      assertEquals(1, columnImportCount, "Column import should not be duplicated");
    }
  }

  /** Helper assertion method for more readable string assertions */
  private StringAssert assertThat(String actual) {
    return new StringAssert(actual);
  }

  /** Custom assertion class for string validation */
  private static class StringAssert {
    private final String actual;

    public StringAssert(String actual) {
      this.actual = actual;
    }

    public StringAssert contains(String expected) {
      assertTrue(actual.contains(expected), 
          "Expected string to contain: '" + expected + "' but was: '" + actual + "'");
      return this;
    }

    public StringAssert doesNotContain(String notExpected) {
      assertFalse(actual.contains(notExpected), 
          "Expected string to not contain: '" + notExpected + "' but was: '" + actual + "'");
      return this;
    }
  }
}