package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.command.extra.JavaFieldType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateJPAEntityFieldCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for CreateJPAEntityFieldCommand.
 *
 * <p>Example usage:
 *
 * <pre>
 * CreateJPAEntityFieldCommand command = new CreateJPAEntityFieldCommand(service);
 * // Set command options: --cwd /path/to/project --file-path /path/to/Entity.java --type BASIC --field-name name
 * DataTransferObject&lt;CreateNewFileResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     CreateNewFileResponse response = result.getData();
 *     System.out.println("Field created in: " + response.getFilePath());
 * }
 * </pre>
 */
@DisplayName("CreateJPAEntityFieldCommand Tests")
class CreateJPAEntityFieldCommandTest {

  @TempDir Path tempDir;
  private CreateJPAEntityFieldCommand createJPAEntityFieldCommand;
  private TestCreateJPAEntityFieldCommandService testService;

  @BeforeEach
  void setUp() {
    testService = new TestCreateJPAEntityFieldCommandService();
    createJPAEntityFieldCommand = new CreateJPAEntityFieldCommand(testService);
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {

    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Given & When
      var commandAnnotation =
          CreateJPAEntityFieldCommand.class.getAnnotation(picocli.CommandLine.Command.class);

      // Then
      assertNotNull(commandAnnotation);
      assertEquals("create-jpa-entity-field", commandAnnotation.name());
      assertEquals(
          "Create JPA Repository for the current JPA Entity.", commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() {
      // Given & When
      assertTrue(createJPAEntityFieldCommand instanceof Callable);

      // Then - verify return type through successful call
      setupCreateJPAEntityFieldCommand(
          tempDir.resolve("Entity.java"), JavaFieldType.BASIC, "testField", "String", null);

      // When
      assertDoesNotThrow(
          () -> {
            DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();
            assertNotNull(result);
            assertTrue(result.getData() instanceof CreateNewFileResponse);
          });
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("TestEntity.java");

      // When
      setupCreateJPAEntityFieldCommand(entityFile, JavaFieldType.BASIC, "name", "String", null);
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(testService.wasServiceCalled());
      assertEquals(tempDir, testService.getLastCalledCwd());
      assertEquals(entityFile, testService.getLastCalledFilePath());
      assertEquals(SupportedLanguage.JAVA, testService.getLastCalledLanguage());
      assertEquals(SupportedIDE.NONE, testService.getLastCalledIde());
    }
  }

  @Nested
  @DisplayName("Field creation tests")
  class FieldCreationTests {

    @Test
    @DisplayName("Should successfully create basic field")
    void shouldSuccessfullyCreateBasicField() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      setupCreateJPAEntityFieldCommand(entityFile, JavaFieldType.BASIC, "username", "String", null);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertNotNull(result.getData().getFilePath());
      assertTrue(testService.wasServiceCalled());
      assertEquals("username", testService.getLastCalledFieldName());
      assertEquals("String", testService.getLastCalledFieldType());
      assertEquals(JavaFieldType.BASIC, testService.getLastCalledType());
    }

    @Test
    @DisplayName("Should successfully create ID field")
    void shouldSuccessfullyCreateIdField() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Product.java");
      setupCreateJPAEntityFieldCommand(entityFile, JavaFieldType.ID, "id", "Long", null);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("id", testService.getLastCalledFieldName());
      assertEquals("Long", testService.getLastCalledFieldType());
      assertEquals(JavaFieldType.ID, testService.getLastCalledType());
    }

    @Test
    @DisplayName("Should successfully create enum field")
    void shouldSuccessfullyCreateEnumField() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Order.java");
      setupCreateJPAEntityFieldCommand(
          entityFile, JavaFieldType.ENUM, "status", "OrderStatus", "com.example.enums");

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("status", testService.getLastCalledFieldName());
      assertEquals("OrderStatus", testService.getLastCalledFieldType());
      assertEquals("com.example.enums", testService.getLastCalledFieldTypePackageName());
      assertEquals(JavaFieldType.ENUM, testService.getLastCalledType());
    }

    @Test
    @DisplayName("Should handle field with length constraint")
    void shouldHandleFieldWithLengthConstraint() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("User.java");
      setupCreateJPAEntityFieldCommandWithConstraints(
          entityFile,
          JavaFieldType.BASIC,
          "email",
          "String",
          null,
          255,
          null,
          null,
          null,
          null,
          false,
          true,
          true,
          false);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(255, testService.getLastCalledFieldLength());
      assertFalse(testService.getLastCalledFieldNullable());
      assertTrue(testService.getLastCalledFieldUnique());
    }

    @Test
    @DisplayName("Should handle field with precision and scale")
    void shouldHandleFieldWithPrecisionAndScale() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Product.java");
      setupCreateJPAEntityFieldCommandWithConstraints(
          entityFile,
          JavaFieldType.BASIC,
          "price",
          "BigDecimal",
          "java.math",
          null,
          10,
          2,
          null,
          null,
          false,
          false,
          true,
          false);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(10, testService.getLastCalledFieldPrecision());
      assertEquals(2, testService.getLastCalledFieldScale());
      assertEquals("BigDecimal", testService.getLastCalledFieldType());
      assertEquals("java.math", testService.getLastCalledFieldTypePackageName());
    }

    @Test
    @DisplayName("Should handle temporal field configuration")
    void shouldHandleTemporalFieldConfiguration() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Event.java");
      setupCreateJPAEntityFieldCommandWithConstraints(
          entityFile,
          JavaFieldType.BASIC,
          "createdAt",
          "LocalDateTime",
          "java.time",
          null,
          null,
          null,
          JavaFieldTemporal.TIMESTAMP,
          JavaFieldTimeZoneStorage.NATIVE,
          true,
          false,
          false,
          false);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(JavaFieldTemporal.TIMESTAMP, testService.getLastCalledFieldTemporal());
      assertEquals(JavaFieldTimeZoneStorage.NATIVE, testService.getLastCalledFieldTimeZoneStorage());
      assertFalse(testService.getLastCalledFieldUpdatable());
    }

    @Test
    @DisplayName("Should handle large object field")
    void shouldHandleLargeObjectField() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Document.java");
      setupCreateJPAEntityFieldCommandWithConstraints(
          entityFile,
          JavaFieldType.BASIC,
          "content",
          "String",
          null,
          null,
          null,
          null,
          null,
          null,
          true,
          false,
          true,
          true);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.getLastCalledFieldLargeObject());
      assertEquals("content", testService.getLastCalledFieldName());
    }
  }

  @Nested
  @DisplayName("Language support tests")
  class LanguageSupportTests {

    @Test
    @DisplayName("Should support Java language")
    void shouldSupportJavaLanguage() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Entity.java");
      setupCreateJPAEntityFieldCommand(entityFile, JavaFieldType.BASIC, "field", "String", null);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData().getFilePath());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service error response")
    void shouldHandleServiceErrorResponse() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Entity.java");
      testService.setErrorResponse("Service error occurred");
      setupCreateJPAEntityFieldCommand(entityFile, JavaFieldType.BASIC, "field", "String", null);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Service error occurred", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle null field type package name")
    void shouldHandleNullFieldTypePackageName() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Entity.java");
      setupCreateJPAEntityFieldCommand(
          entityFile, JavaFieldType.BASIC, "field", "String", null); // null package name

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(testService.getLastCalledFieldTypePackageName());
    }

    @Test
    @DisplayName("Should handle all boolean options correctly")
    void shouldHandleAllBooleanOptionsCorrectly() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Entity.java");
      setupCreateJPAEntityFieldCommandWithConstraints(
          entityFile,
          JavaFieldType.BASIC,
          "complexField",
          "String",
          null,
          100,
          null,
          null,
          null,
          null,
          false,
          true,
          false,
          true);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(100, testService.getLastCalledFieldLength());
      assertFalse(testService.getLastCalledFieldNullable());
      assertTrue(testService.getLastCalledFieldUnique());
      assertFalse(testService.getLastCalledFieldUpdatable());
      assertTrue(testService.getLastCalledFieldLargeObject());
    }

    @Test
    @DisplayName("Should handle different IDE configurations")
    void shouldHandleDifferentIdeConfigurations() throws Exception {
      // Given
      Path entityFile = tempDir.resolve("Entity.java");
      setupCreateJPAEntityFieldCommandWithIde(
          entityFile, JavaFieldType.BASIC, "field", "String", null, SupportedIDE.VSCODE);

      // When
      DataTransferObject<CreateNewFileResponse> result = createJPAEntityFieldCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(SupportedIDE.VSCODE, testService.getLastCalledIde());
    }
  }

  /** Helper method to set up CreateJPAEntityFieldCommand with basic test data. */
  private void setupCreateJPAEntityFieldCommand(
      Path filePath,
      JavaFieldType type,
      String fieldName,
      String fieldType,
      String fieldTypePackageName) {
    setField(createJPAEntityFieldCommand, "cwd", tempDir);
    setField(createJPAEntityFieldCommand, "filePath", filePath);
    setField(createJPAEntityFieldCommand, "type", type);
    setField(createJPAEntityFieldCommand, "fieldName", fieldName);
    setField(createJPAEntityFieldCommand, "fieldType", fieldType);
    setField(createJPAEntityFieldCommand, "fieldTypePackageName", fieldTypePackageName);
    setField(createJPAEntityFieldCommand, "language", SupportedLanguage.JAVA);
    setField(createJPAEntityFieldCommand, "ide", SupportedIDE.NONE);

    // Set default boolean values
    setField(createJPAEntityFieldCommand, "fieldNullable", true);
    setField(createJPAEntityFieldCommand, "fieldUnique", false);
    setField(createJPAEntityFieldCommand, "fieldUpdatable", true);
    setField(createJPAEntityFieldCommand, "fieldLargeObject", false);
  }

  /** Helper method to set up CreateJPAEntityFieldCommand with constraints. */
  private void setupCreateJPAEntityFieldCommandWithConstraints(
      Path filePath,
      JavaFieldType type,
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
      boolean fieldLargeObject) {
    setupCreateJPAEntityFieldCommand(filePath, type, fieldName, fieldType, fieldTypePackageName);

    setField(createJPAEntityFieldCommand, "fieldLength", fieldLength);
    setField(createJPAEntityFieldCommand, "fieldPrecision", fieldPrecision);
    setField(createJPAEntityFieldCommand, "fieldScale", fieldScale);
    setField(createJPAEntityFieldCommand, "fieldTemporal", fieldTemporal);
    setField(createJPAEntityFieldCommand, "fieldTimeZoneStorage", fieldTimeZoneStorage);
    setField(createJPAEntityFieldCommand, "fieldNullable", fieldNullable);
    setField(createJPAEntityFieldCommand, "fieldUnique", fieldUnique);
    setField(createJPAEntityFieldCommand, "fieldUpdatable", fieldUpdatable);
    setField(createJPAEntityFieldCommand, "fieldLargeObject", fieldLargeObject);
  }

  /** Helper method to set up CreateJPAEntityFieldCommand with custom IDE. */
  private void setupCreateJPAEntityFieldCommandWithIde(
      Path filePath,
      JavaFieldType type,
      String fieldName,
      String fieldType,
      String fieldTypePackageName,
      SupportedIDE ide) {
    setupCreateJPAEntityFieldCommand(filePath, type, fieldName, fieldType, fieldTypePackageName);
    setField(createJPAEntityFieldCommand, "ide", ide);
  }

  /** Helper method to set private fields using reflection for testing purposes. */
  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field: " + fieldName, e);
    }
  }

  /** Test double for CreateJPAEntityFieldCommandService to control service behavior in tests. */
  private static class TestCreateJPAEntityFieldCommandService
      extends CreateJPAEntityFieldCommandService {
    private boolean serviceCalled = false;
    private DataTransferObject<Void> responseToReturn = DataTransferObject.success();
    private Path lastCalledCwd;
    private Path lastCalledFilePath;
    private SupportedLanguage lastCalledLanguage;
    private SupportedIDE lastCalledIde;
    private String lastCalledFieldName;
    private String lastCalledFieldType;
    private String lastCalledFieldTypePackageName;
    private Integer lastCalledFieldLength;
    private Integer lastCalledFieldPrecision;
    private Integer lastCalledFieldScale;
    private JavaFieldTemporal lastCalledFieldTemporal;
    private JavaFieldTimeZoneStorage lastCalledFieldTimeZoneStorage;
    private boolean lastCalledFieldNullable;
    private boolean lastCalledFieldUnique;
    private boolean lastCalledFieldUpdatable;
    private boolean lastCalledFieldLargeObject;
    private JavaFieldType lastCalledType;

    public TestCreateJPAEntityFieldCommandService() {
      super(null); // Mock dependencies are not used in tests
    }

    @Override
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
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      this.lastCalledFilePath = filePath;
      this.lastCalledLanguage = language;
      this.lastCalledIde = ide;
      this.lastCalledFieldName = fieldName;
      this.lastCalledFieldType = fieldType;
      this.lastCalledFieldTypePackageName = fieldTypePackageName;
      this.lastCalledFieldLength = fieldLength;
      this.lastCalledFieldPrecision = fieldPrecision;
      this.lastCalledFieldScale = fieldScale;
      this.lastCalledFieldTemporal = fieldTemporal;
      this.lastCalledFieldTimeZoneStorage = fieldTimeZoneStorage;
      this.lastCalledFieldNullable = fieldNullable;
      this.lastCalledFieldUnique = fieldUnique;
      this.lastCalledFieldUpdatable = fieldUpdatable;
      this.lastCalledFieldLargeObject = fieldLargeObject;
      this.lastCalledType = type;
      return this.responseToReturn;
    }

    public void setErrorResponse(String errorMessage) {
      this.responseToReturn = DataTransferObject.error(errorMessage);
    }

    public boolean wasServiceCalled() {
      return this.serviceCalled;
    }

    public Path getLastCalledCwd() {
      return this.lastCalledCwd;
    }

    public Path getLastCalledFilePath() {
      return this.lastCalledFilePath;
    }

    public SupportedLanguage getLastCalledLanguage() {
      return this.lastCalledLanguage;
    }

    public SupportedIDE getLastCalledIde() {
      return this.lastCalledIde;
    }

    public String getLastCalledFieldName() {
      return this.lastCalledFieldName;
    }

    public String getLastCalledFieldType() {
      return this.lastCalledFieldType;
    }

    public String getLastCalledFieldTypePackageName() {
      return this.lastCalledFieldTypePackageName;
    }

    public Integer getLastCalledFieldLength() {
      return this.lastCalledFieldLength;
    }

    public Integer getLastCalledFieldPrecision() {
      return this.lastCalledFieldPrecision;
    }

    public Integer getLastCalledFieldScale() {
      return this.lastCalledFieldScale;
    }

    public JavaFieldTemporal getLastCalledFieldTemporal() {
      return this.lastCalledFieldTemporal;
    }

    public JavaFieldTimeZoneStorage getLastCalledFieldTimeZoneStorage() {
      return this.lastCalledFieldTimeZoneStorage;
    }

    public boolean getLastCalledFieldNullable() {
      return this.lastCalledFieldNullable;
    }

    public boolean getLastCalledFieldUnique() {
      return this.lastCalledFieldUnique;
    }

    public boolean getLastCalledFieldUpdatable() {
      return this.lastCalledFieldUpdatable;
    }

    public boolean getLastCalledFieldLargeObject() {
      return this.lastCalledFieldLargeObject;
    }

    public JavaFieldType getLastCalledType() {
      return this.lastCalledType;
    }
  }
}

