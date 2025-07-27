package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PathHelper Tests")
class PathHelperTest {

  private PathHelper pathHelper;

  @BeforeEach
  void setUp() {
    this.pathHelper = new PathHelper();
  }

  @Nested
  @DisplayName("convertToFile()")
  class ConvertToFileTests {
    @Test
    @DisplayName("should convert a String path to a File object")
    void convertToFile_fromString_shouldReturnFile() {
      String filePath = "test.txt";
      File file = pathHelper.convertToFile(filePath);
      assertEquals("test.txt", file.getName());
    }
  }

  @Nested
  @DisplayName("getFileSourceCode()")
  class GetFileSourceCodeTests {
    @Test
    @DisplayName("should return content for a valid file")
    void getFileSourceCode_forValidFile_shouldReturnContent(@TempDir Path tempDir)
        throws IOException {
      Path tempFile = tempDir.resolve("test.txt");
      String expectedContent = "Hello, World!";
      Files.writeString(tempFile, expectedContent);
      Optional<String> content = pathHelper.getFileSourceCode(tempFile.toFile());
      assertTrue(content.isPresent());
      assertEquals(expectedContent, content.get());
    }

    @Test
    @DisplayName("should return empty for a non-existent file")
    void getFileSourceCode_forNonExistentFile_shouldReturnEmpty() {
      File nonExistentFile = new File("non_existent_file_123.txt");
      Optional<String> content = pathHelper.getFileSourceCode(nonExistentFile);
      assertFalse(content.isPresent());
    }

    @Test
    @DisplayName("should handle String path correctly")
    void getFileSourceCode_forValidStringPath_shouldReturnContent(@TempDir Path tempDir)
        throws IOException {
      Path tempFile = tempDir.resolve("testFromString.txt");
      String expectedContent = "Content from string path";
      Files.writeString(tempFile, expectedContent);
      Optional<String> content = pathHelper.getFileSourceCode(tempFile.toString());
      assertTrue(content.isPresent());
      assertEquals(expectedContent, content.get());
    }
  }

  @Nested
  @DisplayName("createFile()")
  class CreateFileTests {
    @Test
    @DisplayName("should create a new file with the given content")
    void createFile_shouldSucceedAndWriteContent(@TempDir Path tempDir) throws IOException {
      File newFile = tempDir.resolve("myNewFile.txt").toFile();
      String expectedContent = "This is a test.";
      boolean result = pathHelper.createFile(newFile, expectedContent);
      assertTrue(result, "createFile should return true on success.");
      assertTrue(newFile.exists(), "The file should exist after creation.");
      String actualContent = Files.readString(newFile.toPath());
      assertEquals(
          expectedContent,
          actualContent,
          "The file content should match the provided source code.");
    }

    @Test
    @DisplayName("should return false if it cannot write to the path")
    void createFile_whenPathIsInvalid_shouldReturnFalse() throws IOException {
      File invalidFile = new File("/a/b/c/d/e/f/g/myNewFile.txt");
      boolean result = pathHelper.createFile(invalidFile, "some content");
      assertFalse(result, "createFile should return false when the path is not writable.");
    }
  }

  @Nested
  @DisplayName("findFilesByExtention()")
  class FindFilesByExtentionTests {
    @Test
    @DisplayName("should find all files with a given extension")
    void findFilesByExtention_shouldReturnMatchingFiles(@TempDir Path tempDir) throws IOException {
      Files.createFile(tempDir.resolve("test1.java"));
      Files.createFile(tempDir.resolve("test2.java"));
      Files.createFile(tempDir.resolve("test.txt"));
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectory(subDir);
      Files.createFile(subDir.resolve("test3.java"));
      List<File> javaFiles = pathHelper.findFilesByExtention(tempDir.toFile(), "java");
      assertEquals(3, javaFiles.size());
      assertTrue(javaFiles.stream().allMatch(f -> f.getName().endsWith(".java")));
    }

    @Test
    @DisplayName("should return empty list when no files match")
    void findFilesByExtention_whenNoMatches_shouldReturnEmptyList(@TempDir File tempDir)
        throws IOException {
      Files.createFile(tempDir.toPath().resolve("test.txt"));
      List<File> javaFiles = pathHelper.findFilesByExtention(tempDir, "java");
      assertTrue(javaFiles.isEmpty());
    }
  }

  @Nested
  @DisplayName("findDirectoryRecursively()")
  class FindDirectoryRecursivelyTests {
    @Test
    @DisplayName("should find an existing directory")
    void findDirectory_whenDirectoryExists_shouldReturnPath(@TempDir File tempDir)
        throws IOException {
      File subDir = tempDir.toPath().resolve("my_dir").toFile();
      subDir.mkdir();
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(tempDir, "my_dir");
      assertTrue(foundDir.isPresent());
      assertEquals(subDir, foundDir.get());
    }

    @Test
    @DisplayName("should return empty for a non-existent directory")
    void findDirectory_whenDirectoryDoesNotExist_shouldReturnEmpty(@TempDir File tempDir) {
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(tempDir, "non_existent_dir");
      assertFalse(foundDir.isPresent());
    }

    @Test
    @DisplayName("should return empty if rootDir is null")
    void findDirectory_whenRootDirIsNull_shouldReturnEmpty() {
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(null, "any_dir");
      assertFalse(foundDir.isPresent());
    }

    @Test
    @DisplayName("should find a deeply nested directory")
    void findDirectoryRecursively_whenDirIsNested_shouldReturnPath(@TempDir File tempDir)
        throws IOException {
      File level1 = new File(tempDir, "level1");
      File level2 = new File(level1, "level2");
      File deepDir = new File(level2, "deepDir");
      assertTrue(deepDir.mkdirs(), "Setup: Failed to create nested directories");
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(tempDir, "deepDir");
      assertTrue(foundDir.isPresent());
      assertEquals(deepDir, foundDir.get());
    }

    @Test
    @DisplayName("should find a multi-level path like 'src/main/java'")
    void findDirectoryRecursively_withMultiLevelPath_shouldReturnPath(@TempDir File tempDir)
        throws IOException {
      Path targetPath = tempDir.toPath().resolve("app/src/main/java");
      Files.createDirectories(targetPath);
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(tempDir, "src/main/java");
      assertTrue(foundDir.isPresent());
      assertEquals(targetPath.toFile(), foundDir.get());
    }

    @Test
    @DisplayName("should return empty if root is not a directory")
    void findDirectoryRecursively_whenRootIsNotDirectory_shouldReturnEmpty(@TempDir Path tempDir)
        throws IOException {
      File rootFile = tempDir.resolve("root.txt").toFile();
      rootFile.createNewFile();
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(rootFile, "any_dir");
      assertFalse(foundDir.isPresent());
    }

    @Test
    @DisplayName("should not match a directory that only contains the name")
    void findDirectoryRecursively_whenNameIsPartialMatch_shouldReturnEmpty(@TempDir File tempDir)
        throws IOException {
      File partialMatchDir = tempDir.toPath().resolve("my-java-project").toFile();
      partialMatchDir.mkdir();
      Optional<File> foundDir = pathHelper.findDirectoryRecursively(tempDir, "java");
      assertFalse(foundDir.isPresent());
    }
  }

  @Nested
  @DisplayName("replaceTextInFile()")
  class ReplaceTextInFileTests {
    @Test
    @DisplayName("should correctly modify the content of a file")
    void replaceTextInFile_shouldSucceed(@TempDir Path tempDir) throws IOException {
      File file = tempDir.resolve("test.txt").toFile();
      String initialContent = "Hello, old world!";
      Files.writeString(file.toPath(), initialContent);

      boolean result = pathHelper.replaceTextInFile(file, 7, 10, "new");
      assertTrue(result);

      String newContent = Files.readString(file.toPath());
      assertEquals("Hello, new world!", newContent);
    }
  }

  @Nested
  @DisplayName("moveFile()")
  class MoveFileTests {
    @Test
    @DisplayName("should move a file from source to destination")
    void moveFile_shouldSucceed(@TempDir Path tempDir) throws IOException {
      File sourceFile = tempDir.resolve("source.txt").toFile();
      assertTrue(sourceFile.createNewFile());
      File destFile = tempDir.resolve("destination.txt").toFile();

      boolean result = pathHelper.moveFile(sourceFile, destFile);
      assertTrue(result);
      assertFalse(sourceFile.exists());
      assertTrue(destFile.exists());
    }
  }

  @Nested
  @DisplayName("renameDirectory()")
  class RenameDirectoryTests {
    @Test
    @DisplayName("should rename a directory")
    void renameDirectory_shouldSucceed(@TempDir Path tempDir) throws IOException {
      File sourceDir = tempDir.resolve("sourceDir").toFile();
      assertTrue(sourceDir.mkdir());
      File destDir = tempDir.resolve("destDir").toFile();

      boolean result = pathHelper.renameDirectory(sourceDir, destDir);
      assertTrue(result);
      assertFalse(sourceDir.exists());
      assertTrue(destDir.exists());
    }
  }
}
