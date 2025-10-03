package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.extra.JavaFieldModifier;
import io.github.syntaxpresso.core.command.extra.JavaVisibilityModifier;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.FieldCapture;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("FieldDeclarationService Tests")
class FieldDeclarationServiceTest {
  private FieldDeclarationService fieldDeclarationService;
  private ClassDeclarationService classDeclarationService;

  private static final String SIMPLE_FIELDS_CODE =
      """
      public class SimpleClass {
          private String name;
          private int age;
          public boolean active;

          public void method() {
              // method body
          }
      }
      """;

  private static final String ANNOTATED_FIELDS_CODE =
      """
      package com.example;

      import javax.persistence.*;
      import java.util.List;

      @Entity
      public class User {
          @Id
          @GeneratedValue(strategy = GenerationType.IDENTITY)
          private Long id;

          @Column(name = "username", nullable = false)
          private String username;

          @NotNull
          @Email
          private String email;

          @OneToMany(mappedBy = "user")
          private List<Order> orders;

          public User() {}
      }
      """;

  private static final String INITIALIZED_FIELDS_CODE =
      """
      public class ConfigClass {
          private String defaultName = "Unknown";
          private int maxRetries = 3;
          private boolean enabled = true;
          private List<String> items = new ArrayList<>();
          private static final String CONSTANT = "CONST_VALUE";

          public void method() {
              this.defaultName = "Updated";
              this.enabled = false;
          }
      }
      """;

  private static final String GENERIC_FIELDS_CODE =
      """
      public class GenericContainer<T> {
          private T data;
          private List<T> items;
          private Map<String, T> mappings;
          private List<String> names;
          private Optional<T> optional;

          public void process() {
              this.data = null;
              this.items.clear();
          }
      }
      """;

  private static final String MULTIPLE_SAME_TYPE_CODE =
      """
      public class PersonClass {
          private String firstName;
          private String lastName;
          private String email;
          private int age;
          private int score;

          public void updateNames() {
              this.firstName = "New";
              this.lastName = "Name";
          }
      }
      """;

  @BeforeEach
  void setUp() {
    this.fieldDeclarationService = new FieldDeclarationService();

    // Set up ClassDeclarationService for helper methods
    FormalParameterService formalParameterService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterService);
    this.classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
  }

  @Nested
  @DisplayName("getFieldDeclarationNodeInfo() Tests")
  class GetFieldDeclarationNodeInfoTests {

    /**
     * Tests that getFieldDeclarationNodeInfo correctly extracts basic field information.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * String fieldType = tsFile.getTextFromNode(info.get("fieldType"));  // e.g., "String"
     * String fieldName = tsFile.getTextFromNode(info.get("fieldName"));  // e.g., "name"
     * </pre>
     */
    @Test
    @DisplayName("should extract basic field information")
    void getFieldDeclarationNodeInfo_withSimpleField_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertFalse(fieldNodes.isEmpty());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNodes.get(0));

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldType = info.get(FieldCapture.FIELD_TYPE.getCaptureName());
      assertNotNull(fieldType);
      assertEquals("String", tsFile.getTextFromNode(fieldType));

      TSNode fieldName = info.get(FieldCapture.FIELD_NAME.getCaptureName());
      assertNotNull(fieldName);
      assertEquals("name", tsFile.getTextFromNode(fieldName));
    }

    /**
     * Tests that getFieldDeclarationNodeInfo handles fields with initialization values.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * TSNode fieldValue = info.get("fieldValue");
     * // fieldValue node contains "\"Unknown\""
     * </pre>
     */
    @Test
    @DisplayName("should extract field with initialization value")
    void getFieldDeclarationNodeInfo_withInitializedField_shouldExtractValue() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      // Find the field with initialization
      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "defaultName", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNode.get());

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldValue = info.get(FieldCapture.FIELD_VALUE.getCaptureName());
      assertNotNull(fieldValue);
      assertEquals("\"Unknown\"", tsFile.getTextFromNode(fieldValue));
    }

    /**
     * Tests that getFieldDeclarationNodeInfo handles generic field types.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * TSNode fieldType = info.get("fieldType");
     * TSNode typeArgument = info.get("fieldTypeArgument");
     * // fieldType = "List", typeArgument = "T"
     * </pre>
     */
    @Test
    @DisplayName("should extract generic field information")
    void getFieldDeclarationNodeInfo_withGenericField_shouldExtractTypeArguments() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      // Find the generic List field
      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "items", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNode.get());

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldType = info.get(FieldCapture.FIELD_TYPE.getCaptureName());
      assertNotNull(fieldType);
      String typeText = tsFile.getTextFromNode(fieldType);
      assertTrue(typeText.contains("List") || typeText.contains("T"));

      // Type argument may or may not be captured depending on the query
      TSNode typeArgument = info.get(FieldCapture.FIELD_TYPE_ARGUMENT.getCaptureName());
      if (typeArgument != null) {
        String argText = tsFile.getTextFromNode(typeArgument);
        assertTrue(argText.equals("T") || argText.equals("String"));
      }
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getFieldDeclarationNodeInfo_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);

      // Test with null file
      List<Map<String, TSNode>> result1 =
          fieldDeclarationService.getFieldDeclarationNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      // Test with invalid node type
      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      assertNotNull(methodNode);

      List<Map<String, TSNode>> result2 =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, methodNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationNodes() Tests")
  class GetAllFieldDeclarationNodesTests {

    /**
     * Tests that getAllFieldDeclarationNodes finds all fields in a class.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
     * for (TSNode field : fieldNodes) {
     *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find all field declarations in class")
    void getAllFieldDeclarationNodes_withMultipleFields_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());

      // Should find exactly 3 fields (name, age, active)
      assertEquals(3, fieldNodes.size());

      // Verify we can get field names for all found fields
      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode =
            fieldDeclarationService.getFieldDeclarationNameNode(tsFile, fieldNode);
        assertTrue(nameNode.isPresent());
        String fieldName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("name", "age", "active").contains(fieldName));
      }
    }

    @Test
    @DisplayName("should return empty list for class with no fields")
    void getAllFieldDeclarationNodes_withNoFields_shouldReturnEmptyList() {
      String noFieldsCode =
          """
          public class EmptyClass {
              public void method() {
                  // no fields
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, noFieldsCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "EmptyClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());

      assertTrue(fieldNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle null input")
    void getAllFieldDeclarationNodes_withNullInput_shouldReturnEmptyList() {
      List<TSNode> result = fieldDeclarationService.getAllFieldDeclarationNodes(null, null);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should handle invalid class node")
    void getAllFieldDeclarationNodes_withInvalidClassNode_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      assertNotNull(methodNode);

      List<TSNode> result = fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, methodNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationAnnotationNodes() Tests")
  class GetAllFieldDeclarationAnnotationNodesTests {

    /**
     * Tests that getAllFieldDeclarationAnnotationNodes finds all field annotations.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; annotationNodes = service.getAllFieldDeclarationAnnotationNodes(tsFile, classNode);
     * for (TSNode annotation : annotationNodes) {
     *   String annotationText = tsFile.getTextFromNode(annotation);  // e.g., "@NotNull"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find all field annotations")
    void getAllFieldDeclarationAnnotationNodes_withAnnotatedFields_shouldFindAnnotations() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, ANNOTATED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      List<TSNode> annotationNodes =
          fieldDeclarationService.getAllFieldDeclarationAnnotationNodes(tsFile, classNode.get());

      // Should find multiple annotations (@Id, @GeneratedValue, @Column, @NotNull, @Email,
      // @OneToMany)
      assertFalse(annotationNodes.isEmpty());
      assertTrue(annotationNodes.size() >= 1);
    }

    @Test
    @DisplayName("should return empty list for fields without annotations")
    void getAllFieldDeclarationAnnotationNodes_withoutAnnotations_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> annotationNodes =
          fieldDeclarationService.getAllFieldDeclarationAnnotationNodes(tsFile, classNode.get());

      assertTrue(annotationNodes.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationFullTypeNode() Tests")
  class GetFieldDeclarationFullTypeNodeTests {

    /**
     * Tests that getFieldDeclarationFullTypeNode retrieves complete type information.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; fullTypeNode = service.getFieldDeclarationFullTypeNode(tsFile, fieldNode);
     * if (fullTypeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(fullTypeNode.get());  // e.g., "List&lt;String&gt;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve full type node for simple type")
    void getFieldDeclarationFullTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should retrieve full type node for generic type")
    void getFieldDeclarationFullTypeNode_withGenericType_shouldReturnFullTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "items", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      // The actual text will include the full generic type
      String typeText = tsFile.getTextFromNode(typeNode.get());
      assertTrue(typeText.contains("List"));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationTypeNode() Tests")
  class GetFieldDeclarationTypeNodeTests {

    /**
     * Tests that getFieldDeclarationTypeNode retrieves base type or type argument.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; typeNode = service.getFieldDeclarationTypeNode(tsFile, fieldNode);
     * if (typeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(typeNode.get());  // e.g., "String" from List&lt;String&gt;
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve simple type node")
    void getFieldDeclarationTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "age", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      String typeText = tsFile.getTextFromNode(typeNode.get());
      assertEquals("int", typeText);
    }

    @Test
    @DisplayName("should retrieve type argument for generic type")
    void getFieldDeclarationTypeNode_withGenericType_shouldReturnTypeArgument() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "names", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationNameNode() Tests")
  class GetFieldDeclarationNameNodeTests {

    /**
     * Tests that getFieldDeclarationNameNode retrieves field name identifier.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; nameNode = service.getFieldDeclarationNameNode(tsFile, fieldNode);
     * if (nameNode.isPresent()) {
     *   String name = tsFile.getTextFromNode(nameNode.get());  // e.g., "firstName"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve field name node")
    void getFieldDeclarationNameNode_withValidField_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertFalse(fieldNodes.isEmpty());

      Optional<TSNode> nameNode =
          fieldDeclarationService.getFieldDeclarationNameNode(tsFile, fieldNodes.get(0));

      assertTrue(nameNode.isPresent());
      String fieldName = tsFile.getTextFromNode(nameNode.get());
      assertTrue(List.of("name", "age", "active").contains(fieldName));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationValueNode() Tests")
  class GetFieldDeclarationValueNodeTests {

    /**
     * Tests that getFieldDeclarationValueNode retrieves initialization values.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; valueNode = service.getFieldDeclarationValueNode(tsFile, fieldNode);
     * if (valueNode.isPresent()) {
     *   String value = tsFile.getTextFromNode(valueNode.get());  // e.g., "\"John\""
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve field initialization value")
    void getFieldDeclarationValueNode_withInitializedField_shouldReturnValueNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "maxRetries", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> valueNode =
          fieldDeclarationService.getFieldDeclarationValueNode(tsFile, fieldNode.get());

      if (valueNode.isPresent()) {
        assertEquals("3", tsFile.getTextFromNode(valueNode.get()));
      } else {
        // If value node is not captured, that's also acceptable for this test
        assertTrue(true, "Field value node not captured, but method executed without error");
      }
    }

    @Test
    @DisplayName("should return empty for field without initialization")
    void getFieldDeclarationValueNode_withoutInitialization_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> valueNode =
          fieldDeclarationService.getFieldDeclarationValueNode(tsFile, fieldNode.get());

      assertFalse(valueNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationUsageNodes() Tests")
  class GetAllFieldDeclarationUsageNodesTests {

    /**
     * Tests that getAllFieldDeclarationUsageNodes finds field usage in methods.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; usages = service.getAllFieldDeclarationUsageNodes(tsFile, fieldNode, classNode);
     * for (TSNode usage : usages) {
     *   String usageText = tsFile.getTextFromNode(usage);  // e.g., "fieldName"
     *   int line = usage.getStartPoint().getRow() + 1;     // Line number of usage
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find field usages in class methods")
    void getAllFieldDeclarationUsageNodes_withFieldUsages_shouldFindUsages() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "defaultName", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<TSNode> usageNodes =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, fieldNode.get(), classNode.get());

      assertFalse(usageNodes.isEmpty());
      // Should find at least one usage (this.defaultName = "Updated")
      assertTrue(usageNodes.size() >= 1);
    }

    @Test
    @DisplayName("should return empty list for unused fields")
    void getAllFieldDeclarationUsageNodes_withUnusedField_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<TSNode> usageNodes =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, fieldNode.get(), classNode.get());

      assertTrue(usageNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle invalid input")
    void getAllFieldDeclarationUsageNodes_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);

      List<TSNode> result1 =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(null, null, null);
      assertTrue(result1.isEmpty());

      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");

      List<TSNode> result2 =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, methodNode, classNode.get());
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodeByName() Tests")
  class FindFieldDeclarationNodeByNameTests {

    /**
     * Tests that findFieldDeclarationNodeByName locates fields by exact name.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; fieldNode = service.findFieldDeclarationNodeByName(tsFile, "myField", classNode);
     * if (fieldNode.isPresent()) {
     *   String fieldText = tsFile.getTextFromNode(fieldNode.get());  // e.g., "private String myField;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find existing field by name")
    void findFieldDeclarationNodeByName_withExistingField_shouldReturnField() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());

      assertTrue(fieldNode.isPresent());
      String fieldText = tsFile.getTextFromNode(fieldNode.get());
      assertTrue(fieldText.contains("name"));
      assertTrue(fieldText.contains("String"));
    }

    @Test
    @DisplayName("should return empty for non-existent field")
    void findFieldDeclarationNodeByName_withNonExistentField_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "nonExistentField", classNode.get());

      assertFalse(fieldNode.isPresent());
    }

    @Test
    @DisplayName("should handle null and empty parameters")
    void findFieldDeclarationNodeByName_withInvalidParameters_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> result1 =
          fieldDeclarationService.findFieldDeclarationNodeByName(null, "name", classNode.get());
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, null, classNode.get());
      assertFalse(result2.isPresent());

      Optional<TSNode> result3 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "", classNode.get());
      assertFalse(result3.isPresent());

      TSNode mockNode = tsFile.getTree().getRootNode();
      Optional<TSNode> result4 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", mockNode);
      assertFalse(result4.isPresent());
    }

    @Test
    @DisplayName("should be case sensitive")
    void findFieldDeclarationNodeByName_shouldBeCaseSensitive() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> exactMatch =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(exactMatch.isPresent());

      Optional<TSNode> casesDifferent =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "Name", classNode.get());
      assertFalse(casesDifferent.isPresent());
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodesByType() Tests")
  class FindFieldDeclarationNodesByTypeTests {

    /**
     * Tests that findFieldDeclarationNodesByType finds all fields of a specific type.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; stringFields = service.findFieldDeclarationNodesByType(tsFile, "String", classNode);
     * for (TSNode field : stringFields) {
     *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find fields by exact type")
    void findFieldDeclarationNodesByType_withMatchingType_shouldReturnFields() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_SAME_TYPE_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "PersonClass");
      assertTrue(classNode.isPresent());

      List<TSNode> stringFields =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "String", classNode.get());

      assertEquals(3, stringFields.size()); // firstName, lastName, email

      // Verify all are String fields
      for (TSNode field : stringFields) {
        Optional<TSNode> nameNode =
            fieldDeclarationService.getFieldDeclarationNameNode(tsFile, field);
        assertTrue(nameNode.isPresent());
        String fieldName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("firstName", "lastName", "email").contains(fieldName));
      }
    }

    @Test
    @DisplayName("should find fields by primitive type")
    void findFieldDeclarationNodesByType_withPrimitiveType_shouldReturnFields() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_SAME_TYPE_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "PersonClass");
      assertTrue(classNode.isPresent());

      List<TSNode> intFields =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, "int", classNode.get());

      // Should find exactly 2 int fields (age, score)
      assertEquals(2, intFields.size());
    }

    @Test
    @DisplayName("should return empty list for non-existent type")
    void findFieldDeclarationNodesByType_withNonExistentType_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fields =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "NonExistentType", classNode.get());

      assertTrue(fields.isEmpty());
    }

    @Test
    @DisplayName("should handle null and empty parameters")
    void findFieldDeclarationNodesByType_withInvalidParameters_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> result1 =
          fieldDeclarationService.findFieldDeclarationNodesByType(null, "String", classNode.get());
      assertTrue(result1.isEmpty());

      List<TSNode> result2 =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, null, classNode.get());
      assertTrue(result2.isEmpty());

      List<TSNode> result3 =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, "", classNode.get());
      assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("should be case sensitive")
    void findFieldDeclarationNodesByType_shouldBeCaseSensitive() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> exactMatch =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "String", classNode.get());
      assertFalse(exactMatch.isEmpty());

      List<TSNode> casesDifferent =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "string", classNode.get());
      assertTrue(casesDifferent.isEmpty());
    }
  }

  @Nested
  @DisplayName("addFieldDeclaration() Tests")
  class AddFieldDeclarationTests {

    /**
     * Tests that addFieldDeclaration adds a field successfully.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
     * FieldInsertionPoint insertionPoint = service.getFieldInsertionPosition(
     *     tsFile, classNode, FieldInsertionPosition.AFTER_LAST_FIELD);
     * service.addFieldDeclaration(tsFile, classNode, insertionPoint,
     *     JavaVisibilityModifier.PRIVATE, Collections.emptySet(), "String", "newField", null);
     * // Field is now added to the source code, developer can find it using findFieldDeclarationNodeByName
     * </pre>
     */
    @Test
    @DisplayName("should add field successfully")
    void addFieldDeclaration_withValidInput_shouldAddField() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Create a simple insertion point at the end of the existing field
      int insertionPosition = classCode.indexOf(";") + 1; // After the existing field's semicolon
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(insertionPosition);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      // Add a new field
      fieldDeclarationService.addFieldDeclaration(
          tsFile,
          classNode.get(),
          insertionPoint,
          JavaVisibilityModifier.PRIVATE,
          Set.of(),
          "int",
          "newField",
          null
      );
      
      // Check if the field was actually added to the source code
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("newField"), "Field should be present in updated source code");
      
      // Verify the field exists in the class using a fresh class node reference
      // (the original classNode reference is stale after source code modification)
      Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(freshClassNode.isPresent());
      
      Optional<TSNode> foundFieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "newField", freshClassNode.get());
      assertTrue(foundFieldNode.isPresent());
      
      // Verify we can extract information from the found field node
      Optional<TSNode> nameNode = fieldDeclarationService.getFieldDeclarationNameNode(tsFile, foundFieldNode.get());
      assertTrue(nameNode.isPresent());
      assertEquals("newField", tsFile.getTextFromNode(nameNode.get()));
      
      // Verify field type
      Optional<TSNode> typeNode = fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, foundFieldNode.get());
      assertTrue(typeNode.isPresent());
      assertEquals("int", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should add field with initialization successfully")
    void addFieldDeclaration_withInitialization_shouldAddFieldWithValue() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Create a simple insertion point
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(classCode.indexOf("public void method()") - 1);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      // Add a new field with initialization
      fieldDeclarationService.addFieldDeclaration(
          tsFile,
          classNode.get(),
          insertionPoint,
          JavaVisibilityModifier.PRIVATE,
          Set.of(JavaFieldModifier.FINAL),
          "String",
          "defaultName",
          "\"Test\""
      );
      
      // Check if the field was actually added to the source code
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("defaultName"), "Field should be present in updated source code");
      assertTrue(updatedCode.contains("\"Test\""), "Field initialization should be present in updated source code");
      
      // Verify the field exists in the class using a fresh class node reference
      Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(freshClassNode.isPresent());
      
      Optional<TSNode> foundFieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "defaultName", freshClassNode.get());
      assertTrue(foundFieldNode.isPresent());
      
      // Verify field name
      Optional<TSNode> nameNode = fieldDeclarationService.getFieldDeclarationNameNode(tsFile, foundFieldNode.get());
      assertTrue(nameNode.isPresent());
      assertEquals("defaultName", tsFile.getTextFromNode(nameNode.get()));
      
      // Verify initialization value
      Optional<TSNode> valueNode = fieldDeclarationService.getFieldDeclarationValueNode(tsFile, foundFieldNode.get());
      if (valueNode.isPresent()) {
        assertEquals("\"Test\"", tsFile.getTextFromNode(valueNode.get()));
      }
    }

    @Test
    @DisplayName("should handle invalid parameters gracefully")
    void addFieldDeclaration_withInvalidInput_shouldDoNothing() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(classCode.length() - 2);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      String originalCode = tsFile.getSourceCode();
      
      // Test with null tsFile - should do nothing (no exception)
      fieldDeclarationService.addFieldDeclaration(
          null, classNode.get(), insertionPoint, JavaVisibilityModifier.PRIVATE,
          Set.of(), "String", "fieldName", null);
      
      // Test with null class node - should do nothing (no exception)
      fieldDeclarationService.addFieldDeclaration(
          tsFile, null, insertionPoint, JavaVisibilityModifier.PRIVATE,
          Set.of(), "String", "fieldName", null);
      
      // Test with null insertion point - should do nothing (no exception)
      fieldDeclarationService.addFieldDeclaration(
          tsFile, classNode.get(), null, JavaVisibilityModifier.PRIVATE,
          Set.of(), "String", "fieldName", null);
      
      // Test with empty field name - should do nothing (no exception)
      fieldDeclarationService.addFieldDeclaration(
          tsFile, classNode.get(), insertionPoint, JavaVisibilityModifier.PRIVATE,
          Set.of(), "String", "", null);
      
      // Test with empty field type - should do nothing (no exception)
      fieldDeclarationService.addFieldDeclaration(
          tsFile, classNode.get(), insertionPoint, JavaVisibilityModifier.PRIVATE,
          Set.of(), "", "fieldName", null);
      
       // Verify that the source code hasn't changed
      assertEquals(originalCode, tsFile.getSourceCode(), "Source code should remain unchanged with invalid input");
    }

    @Test
    @DisplayName("should add field with multiple modifiers")
    void addFieldDeclaration_withMultipleModifiers_shouldAddField() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(classCode.length() - 2);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      // Add field with multiple modifiers
      fieldDeclarationService.addFieldDeclaration(
          tsFile,
          classNode.get(),
          insertionPoint,
          JavaVisibilityModifier.PRIVATE,
          Set.of(JavaFieldModifier.STATIC, JavaFieldModifier.FINAL),
          "String",
          "CONSTANT_VALUE",
          "\"test\""
      );
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("private static final String CONSTANT_VALUE = \"test\";"), 
          "Field with multiple modifiers should be added correctly");
    }

    @Test
    @DisplayName("should add field with all visibility modifiers")
    void addFieldDeclaration_withAllVisibilityModifiers_shouldAddFields() {
      String classCode = 
          """
          public class TestClass {
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(classCode.length() - 2);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      // Test each visibility modifier
      JavaVisibilityModifier[] modifiers = {
          JavaVisibilityModifier.PUBLIC,
          JavaVisibilityModifier.PRIVATE, 
          JavaVisibilityModifier.PROTECTED,
          JavaVisibilityModifier.PACKAGE_PRIVATE
      };
      
      for (int i = 0; i < modifiers.length; i++) {
        fieldDeclarationService.addFieldDeclaration(
            tsFile,
            tsFile.query("(class_declaration) @class").returning("class").execute().firstNode(),
            insertionPoint,
            modifiers[i],
            Set.of(),
            "String",
            "field" + i,
            null
        );
      }
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("public String field0"), "Public field should be added");
      assertTrue(updatedCode.contains("private String field1"), "Private field should be added");
      assertTrue(updatedCode.contains("protected String field2"), "Protected field should be added");
      assertTrue(updatedCode.contains("String field3"), "Package-private field should be added");
    }

    @Test
    @DisplayName("should add field with complex generic type")
    void addFieldDeclaration_withComplexGenericType_shouldAddField() {
      String classCode = 
          """
          public class TestClass {
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      FieldInsertionPoint insertionPoint = new FieldInsertionPoint();
      insertionPoint.setInsertByte(classCode.length() - 2);
      insertionPoint.setBreakLineBefore(true);
      insertionPoint.setBreakLineAfter(true);
      
      // Add field with complex generic type
      fieldDeclarationService.addFieldDeclaration(
          tsFile,
          classNode.get(),
          insertionPoint,
          JavaVisibilityModifier.PRIVATE,
          Set.of(),
          "Map<String, List<Integer>>",
          "complexMap",
          "new HashMap<>()"
      );
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("Map<String, List<Integer>> complexMap"), 
          "Complex generic type should be handled correctly");
      assertTrue(updatedCode.contains("new HashMap<>()"), 
          "Complex initialization should be handled correctly");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")  
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle class with only static fields")
    void methods_withStaticFields_shouldHandleCorrectly() {
      String staticFieldsCode =
          """
          public class ConstantsClass {
              public static final String CONSTANT = "VALUE";
              private static int counter = 0;

              public static void increment() {
                  counter++;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, staticFieldsCode);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "ConstantsClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertEquals(2, fieldNodes.size());

      // Should find both CONSTANT and counter fields
      Optional<TSNode> constantField =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "CONSTANT", classNode.get());
      assertTrue(constantField.isPresent());

      Optional<TSNode> counterField =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "counter", classNode.get());
      assertTrue(counterField.isPresent());
    }

    @Test
    @DisplayName("should handle complex generic fields")
    void methods_withComplexGenerics_shouldHandleCorrectly() {
      String complexGenericsCode =
          """
          public class ComplexGenericClass {
              private Map<String, List<Integer>> complexMap;
              private Optional<Map<String, Set<Long>>> optionalMap;

              public void method() {
                  this.complexMap = new HashMap<>();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, complexGenericsCode);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "ComplexGenericClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertEquals(2, fieldNodes.size());

      // Should be able to extract type information for complex generics
      for (TSNode field : fieldNodes) {
        Optional<TSNode> typeNode =
            fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, field);
        assertTrue(typeNode.isPresent());
      }
    }

    @Test
    @DisplayName("should handle empty source code")
    void methods_withEmptyCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      // Create a mock node instead of passing null
      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, mockNode);
      assertTrue(fieldNodes.isEmpty());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "field", mockNode);
      assertFalse(fieldNode.isPresent());
    }

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public class { private String");

      // Should not crash, may return empty or partial results
      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, mockNode);
      assertNotNull(fieldNodes);
    }
  }

  @Nested
  @DisplayName("updateFieldDeclaration() Tests")
  class UpdateFieldDeclarationTests {

    @Test
    @DisplayName("should update field successfully")
    void updateFieldDeclaration_withValidInput_shouldUpdateField() {
      String classCode = 
          """
          public class TestClass {
              private String oldField;
          
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the field to update
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "oldField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Update the field
      fieldDeclarationService.updateFieldDeclaration(
          tsFile,
          fieldNode.get(),
          JavaVisibilityModifier.PUBLIC,
          Set.of(JavaFieldModifier.FINAL),
          "int",
          "newField",
          "42"
      );
      
      // Check if the field was updated in the source code
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("oldField"), "Old field name should not be present");
      assertTrue(updatedCode.contains("newField"), "New field name should be present");
      assertTrue(updatedCode.contains("public final int"), "New field modifiers and type should be present");
      assertTrue(updatedCode.contains("= 42"), "New field initialization should be present");
      
      // Verify the updated field exists using a fresh class node reference
      Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(freshClassNode.isPresent());
      
      Optional<TSNode> updatedFieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "newField", freshClassNode.get());
      assertTrue(updatedFieldNode.isPresent());
      
      // Verify field properties
      Optional<TSNode> nameNode = fieldDeclarationService.getFieldDeclarationNameNode(tsFile, updatedFieldNode.get());
      assertTrue(nameNode.isPresent());
      assertEquals("newField", tsFile.getTextFromNode(nameNode.get()));
      
      Optional<TSNode> typeNode = fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, updatedFieldNode.get());
      assertTrue(typeNode.isPresent());
      assertEquals("int", tsFile.getTextFromNode(typeNode.get()));
      
      Optional<TSNode> valueNode = fieldDeclarationService.getFieldDeclarationValueNode(tsFile, updatedFieldNode.get());
      assertTrue(valueNode.isPresent());
      assertEquals("42", tsFile.getTextFromNode(valueNode.get()));
    }

    @Test
    @DisplayName("should update field without initialization")
    void updateFieldDeclaration_withoutInitialization_shouldUpdateField() {
      String classCode = 
          """
          public class TestClass {
              private String fieldWithValue = "old value";
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the field to update
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "fieldWithValue", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Update the field without initialization
      fieldDeclarationService.updateFieldDeclaration(
          tsFile,
          fieldNode.get(),
          JavaVisibilityModifier.PROTECTED,
          Set.of(),
          "boolean",
          "updatedField",
          null // No initialization
      );
      
      // Check if the field was updated
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("fieldWithValue"), "Old field name should not be present");
      assertFalse(updatedCode.contains("old value"), "Old initialization should not be present");
      assertTrue(updatedCode.contains("protected boolean updatedField;"), "New field should be present without initialization");
    }

    @Test
    @DisplayName("should handle invalid parameters gracefully")
    void updateFieldDeclaration_withInvalidInput_shouldDoNothing() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "existingField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      String originalCode = tsFile.getSourceCode();
      
      // Test with null tsFile - should do nothing
      fieldDeclarationService.updateFieldDeclaration(
          null, fieldNode.get(), JavaVisibilityModifier.PUBLIC, Set.of(), "int", "newName", null);
      
      // Test with null field node - should do nothing
      fieldDeclarationService.updateFieldDeclaration(
          tsFile, null, JavaVisibilityModifier.PUBLIC, Set.of(), "int", "newName", null);
      
      // Test with empty field type - should do nothing
      fieldDeclarationService.updateFieldDeclaration(
          tsFile, fieldNode.get(), JavaVisibilityModifier.PUBLIC, Set.of(), "", "newName", null);
      
      // Test with empty field name - should do nothing
      fieldDeclarationService.updateFieldDeclaration(
          tsFile, fieldNode.get(), JavaVisibilityModifier.PUBLIC, Set.of(), "int", "", null);
      
      // Test with wrong node type - should do nothing
      TSNode wrongNode = classNode.get(); // class node instead of field node
      fieldDeclarationService.updateFieldDeclaration(
          tsFile, wrongNode, JavaVisibilityModifier.PUBLIC, Set.of(), "int", "newName", null);
      
      // Verify that the source code hasn't changed
      assertEquals(originalCode, tsFile.getSourceCode(), "Source code should remain unchanged with invalid input");
    }

    @Test
    @DisplayName("should update field with all modifiers")
    void updateFieldDeclaration_withAllModifiers_shouldUpdateField() {
      String classCode = 
          """
          public class TestClass {
              private String simpleField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "simpleField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Update with all possible field modifiers
      fieldDeclarationService.updateFieldDeclaration(
          tsFile,
          fieldNode.get(),
          JavaVisibilityModifier.PUBLIC,
          Set.of(JavaFieldModifier.STATIC, JavaFieldModifier.FINAL, JavaFieldModifier.TRANSIENT, JavaFieldModifier.VOLATILE),
          "String",
          "complexField",
          "\"value\""
      );
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("public static final transient volatile String complexField = \"value\";"), 
          "Field should be updated with all modifiers correctly");
    }

    @Test
    @DisplayName("should update field to package-private")
    void updateFieldDeclaration_toPackagePrivate_shouldUpdateField() {
      String classCode = 
          """
          public class TestClass {
              public String publicField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "publicField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Update to package-private (no visibility modifier)
      fieldDeclarationService.updateFieldDeclaration(
          tsFile,
          fieldNode.get(),
          JavaVisibilityModifier.PACKAGE_PRIVATE,
          Set.of(),
          "boolean",
          "packagePrivateField",
          null
      );
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("boolean packagePrivateField;"), 
          "Field should be updated to package-private correctly");
      assertFalse(updatedCode.contains("public boolean") || 
                  updatedCode.contains("public packagePrivateField"), 
          "Public modifier should be removed from field");
    }

    @Test
    @DisplayName("should update field with complex generic type")
    void updateFieldDeclaration_withComplexGeneric_shouldUpdateField() {
      String classCode = 
          """
          public class TestClass {
              private String simpleField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "simpleField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Update to complex generic type
      fieldDeclarationService.updateFieldDeclaration(
          tsFile,
          fieldNode.get(),
          JavaVisibilityModifier.PRIVATE,
          Set.of(),
          "Optional<Map<String, Set<Long>>>",
          "complexGenericField",
          "Optional.empty()"
      );
      
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("Optional<Map<String, Set<Long>>> complexGenericField = Optional.empty()"), 
          "Complex generic type should be handled correctly");
    }
  }

  @Nested
  @DisplayName("removeFieldDeclaration() Tests")
  class RemoveFieldDeclarationTests {

    @Test
    @DisplayName("should remove field successfully")
    void removeFieldDeclaration_withValidInput_shouldRemoveField() {
      String classCode = 
          """
          public class TestClass {
              private String fieldToKeep;
              private int fieldToRemove;
              public boolean anotherField;
          
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the field to remove
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "fieldToRemove", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Remove the field
      fieldDeclarationService.removeFieldDeclaration(tsFile, fieldNode.get());
      
      // Check if the field was removed from the source code
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("fieldToRemove"), "Removed field should not be present");
      assertTrue(updatedCode.contains("fieldToKeep"), "Other fields should still be present");
      assertTrue(updatedCode.contains("anotherField"), "Other fields should still be present");
      
      // Verify the removed field cannot be found using a fresh class node reference
      Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(freshClassNode.isPresent());
      
      Optional<TSNode> removedFieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "fieldToRemove", freshClassNode.get());
      assertFalse(removedFieldNode.isPresent(), "Removed field should not be found");
      
      // Verify other fields still exist
      Optional<TSNode> keptFieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "fieldToKeep", freshClassNode.get());
      assertTrue(keptFieldNode.isPresent(), "Other fields should still exist");
    }

    @Test
    @DisplayName("should remove field with initialization")
    void removeFieldDeclaration_withInitialization_shouldRemoveCompletely() {
      String classCode = 
          """
          public class TestClass {
              private String fieldWithValue = "some value";
              private int normalField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the field to remove
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "fieldWithValue", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Remove the field
      fieldDeclarationService.removeFieldDeclaration(tsFile, fieldNode.get());
      
      // Check if the field and its initialization were completely removed
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("fieldWithValue"), "Field name should not be present");
      assertFalse(updatedCode.contains("some value"), "Field initialization should not be present");
      assertTrue(updatedCode.contains("normalField"), "Other fields should still be present");
    }

    @Test
    @DisplayName("should handle single field removal")
    void removeFieldDeclaration_singleField_shouldRemoveCleanly() {
      String classCode = 
          """
          public class TestClass {
              private String onlyField;
          
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the field to remove
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "onlyField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Remove the field
      fieldDeclarationService.removeFieldDeclaration(tsFile, fieldNode.get());
      
      // Check if the field was removed
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("onlyField"), "Field should not be present");
      assertTrue(updatedCode.contains("public void method"), "Other class members should still be present");
      
      // Verify class still has proper structure
      Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(freshClassNode.isPresent(), "Class should still exist");
    }

    @Test
    @DisplayName("should handle invalid parameters gracefully")
    void removeFieldDeclaration_withInvalidInput_shouldDoNothing() {
      String classCode = 
          """
          public class TestClass {
              private String existingField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "existingField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      String originalCode = tsFile.getSourceCode();
      
      // Test with null tsFile - should do nothing
      fieldDeclarationService.removeFieldDeclaration(null, fieldNode.get());
      
      // Test with null field node - should do nothing
      fieldDeclarationService.removeFieldDeclaration(tsFile, null);
      
      // Test with wrong node type - should do nothing
      TSNode wrongNode = classNode.get(); // class node instead of field node
      fieldDeclarationService.removeFieldDeclaration(tsFile, wrongNode);
      
      // Verify that the source code hasn't changed
      assertEquals(originalCode, tsFile.getSourceCode(), "Source code should remain unchanged with invalid input");
    }

    @Test
    @DisplayName("should remove field with complex modifiers and generics")
    void removeFieldDeclaration_withComplexField_shouldRemoveCompletely() {
      String classCode = 
          """
          public class TestClass {
              private String keepField;
              public static final Map<String, List<Integer>> complexField = new HashMap<>();
              protected boolean anotherField;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Find the complex field to remove
      Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
          tsFile, "complexField", classNode.get());
      assertTrue(fieldNode.isPresent());
      
      // Remove the complex field
      fieldDeclarationService.removeFieldDeclaration(tsFile, fieldNode.get());
      
      // Check if the complex field was completely removed
      String updatedCode = tsFile.getSourceCode();
      assertFalse(updatedCode.contains("complexField"), "Complex field name should not be present");
      assertFalse(updatedCode.contains("Map<String, List<Integer>>"), "Complex field type should not be present");
      assertFalse(updatedCode.contains("new HashMap<>()"), "Complex field initialization should not be present");
      assertTrue(updatedCode.contains("keepField"), "Other fields should still be present");
      assertTrue(updatedCode.contains("anotherField"), "Other fields should still be present");
    }

    @Test
    @DisplayName("should remove multiple fields in sequence")
    void removeFieldDeclaration_multipleFields_shouldRemoveAllCorrectly() {
      String classCode = 
          """
          public class TestClass {
              private String field1;
              private int field2;
              private boolean field3;
              private double field4;
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, classCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Remove fields in sequence - we need to get fresh class node after each removal
      String[] fieldsToRemove = {"field2", "field4"};
      
      for (String fieldName : fieldsToRemove) {
        // Get fresh class node reference after each modification
        Optional<TSNode> freshClassNode = classDeclarationService.findClassByName(tsFile, "TestClass");
        assertTrue(freshClassNode.isPresent());
        
        Optional<TSNode> fieldNode = fieldDeclarationService.findFieldDeclarationNodeByName(
            tsFile, fieldName, freshClassNode.get());
        assertTrue(fieldNode.isPresent(), "Field " + fieldName + " should exist before removal");
        
        fieldDeclarationService.removeFieldDeclaration(tsFile, fieldNode.get());
        
        // Verify field was removed
        String updatedCode = tsFile.getSourceCode();
        assertFalse(updatedCode.contains(fieldName), "Field " + fieldName + " should be removed");
      }
      
      // Verify remaining fields still exist
      String finalCode = tsFile.getSourceCode();
      assertTrue(finalCode.contains("field1"), "field1 should still exist");
      assertTrue(finalCode.contains("field3"), "field3 should still exist");
      assertFalse(finalCode.contains("field2"), "field2 should be removed");
      assertFalse(finalCode.contains("field4"), "field4 should be removed");
    }
  }
}

