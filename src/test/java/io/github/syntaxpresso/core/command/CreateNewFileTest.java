package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.java.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.JavaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CreateNewFileTest Tests")
class CreateNewFileTest {

    private JavaService javaService;
    private CreateNewFileCommand command;
    private CommandLine cmd;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        javaService = mock(JavaService.class);
        command = new CreateNewFileCommand(javaService);
        cmd = new CommandLine(command);
    }

    @Nested
    @DisplayName("Argument Tests")
    class ArgumentTests {

        @Test
        @DisplayName("should fail when --cwd is not provided")
        void execute_withoutCwd_shouldThrowException() {
            String[] args = {"--packageName", "io.github", "--fileName", "MyClass.java", "--fileType", "CLASS"};
            assertThrows(CommandLine.MissingParameterException.class, () -> cmd.parseArgs(args));
        }

        @Test
        @DisplayName("should fail when --cwd does not exist")
        void execute_withNonExistentCwd_shouldThrowException() {
            String nonExistentPath = "/invalid/path";
            cmd.parseArgs("--cwd", nonExistentPath, "--packageName", "io.github", "--fileName", "MyClass.java", "--fileType", "CLASS");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> command.call(),
                    "Current working directory does not exist.");
        }

        @Test
        @DisplayName("should fail when --packageName is empty")
        void execute_withEmptyPackageName_shouldThrowException() {
            cmd.parseArgs("--cwd", tempDir.toString(), "--packageName", "", "--fileName", "MyClass.java", "--fileType", "CLASS");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> command.call(),
                    "Package name invalid.");
        }

        @Test
        @DisplayName("should fail when --fileName is empty")
        void execute_withEmptyFileName_shouldThrowException() {
            cmd.parseArgs("--cwd", tempDir.toString(), "--packageName", "io.github", "--fileName", "", "--fileType", "CLASS");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> command.call(),
                    "File name invalid.");
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {

        @Test
        @DisplayName("should create Java file successfully")
        void call_shouldCreateJavaFileSuccessfully() throws Exception {
            // Arrange
            Path targetPath = tempDir.resolve("src/main/java/io/github");
            Files.createDirectories(targetPath);

            JavaFileTemplate template = mock(JavaFileTemplate.class);
            when(template.getSourceContent("io.github", "MyClass"))
                    .thenReturn("package io.github;\npublic class MyClass {}");

            when(javaService.findFilePath(any(), eq("io.github"), eq(SourceDirectoryType.MAIN)))
                    .thenReturn(Optional.of(targetPath));

            // Act
            cmd.parseArgs(
                    "--cwd", tempDir.toString(),
                    "--packageName", "io.github",
                    "--fileName", "MyClass.java",
                    "--fileType", "CLASS", // Assuming JavaFileTemplate.CLASS maps to a string "CLASS"
                    "--sourceDirectoryType", "MAIN" // Assuming SourceDirectoryType.MAIN maps to a string "MAIN"
            );
            // Manually set fileType as it's an enum and not directly parsed by picocli without custom converters
            command.setFileType(template);
            command.setSourceDirectoryType(SourceDirectoryType.MAIN);

            DataTransferObject<CreateNewJavaFileResponse> result = command.call();

            // Assert
            assertTrue(result.getSucceed());
            assertNotNull(result.getData());
            assertTrue(result.getData().getFilePath().endsWith("MyClass.java"));
            assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
        }

        @Test
        @DisplayName("should return error if path not found by JavaService")
        void call_shouldReturnErrorIfFilePathNotFound() throws Exception {
            // Arrange
            when(javaService.findFilePath(any(), anyString(), any()))
                    .thenReturn(Optional.empty());

            // Act
            cmd.parseArgs(
                    "--cwd", tempDir.toString(),
                    "--packageName", "io.github",
                    "--fileName", "MyClass.java",
                    "--fileType", "CLASS",
                    "--sourceDirectoryType", "MAIN"
            );
            command.setFileType(JavaFileTemplate.CLASS);
            command.setSourceDirectoryType(SourceDirectoryType.MAIN);

            DataTransferObject<CreateNewJavaFileResponse> result = command.call();

            // Assert
            assertFalse(result.getSucceed());
            assertEquals("Package name couldn't be determined.", result.getErrorReason());
        }
    }
}