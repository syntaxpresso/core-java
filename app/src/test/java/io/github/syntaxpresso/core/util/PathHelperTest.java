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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PathHelper Tests")
class PathHelperTest {

  private PathHelper pathHelper;

  @BeforeEach
  void setUp() {
    pathHelper = new PathHelper();
  }

  @Test
  @DisplayName("convertToFile should convert a String path to a File object")
  void convertToFile_fromString_shouldReturnFile() {
    String filePath = "test.txt";
    File file = pathHelper.convertToFile(filePath);
    assertEquals("test.txt", file.getName());
  }

  @Test
  @DisplayName("getFileSourceCode should return content for a valid file")
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
  @DisplayName("getFileSourceCode should return empty for a non-existent file")
  void getFileSourceCode_forNonExistentFile_shouldReturnEmpty() {
    File nonExistentFile = new File("non_existent_file_123.txt");
    Optional<String> content = pathHelper.getFileSourceCode(nonExistentFile);
    assertFalse(content.isPresent());
  }

  @Test
  @DisplayName("getFileSourceCode should handle String path correctly")
  void getFileSourceCode_forValidStringPath_shouldReturnContent(@TempDir Path tempDir)
      throws IOException {
    Path tempFile = tempDir.resolve("testFromString.txt");
    String expectedContent = "Content from string path";
    Files.writeString(tempFile, expectedContent);

    Optional<String> content = pathHelper.getFileSourceCode(tempFile.toString());

    assertTrue(content.isPresent());
    assertEquals(expectedContent, content.get());
  }

  @Test
  @DisplayName("findFiles should find all files with a given extension")
  void findFiles_shouldReturnMatchingFiles(@TempDir Path tempDir) throws IOException {
    // Create test files
    Files.createFile(tempDir.resolve("test1.java"));
    Files.createFile(tempDir.resolve("test2.java"));
    Files.createFile(tempDir.resolve("test.txt"));

    // Create a subdirectory with a matching file
    Path subDir = tempDir.resolve("subdir");
    Files.createDirectory(subDir);
    Files.createFile(subDir.resolve("test3.java"));

    List<File> javaFiles = pathHelper.findFiles(tempDir.toFile(), "java");

    assertEquals(3, javaFiles.size());
    assertTrue(javaFiles.stream().allMatch(f -> f.getName().endsWith(".java")));
  }

  @Test
  @DisplayName("findFiles should return empty list when no files match")
  void findFiles_whenNoMatches_shouldReturnEmptyList(@TempDir Path tempDir) throws IOException {
    Files.createFile(tempDir.resolve("test.txt"));
    List<File> javaFiles = pathHelper.findFiles(tempDir.toFile(), "java");
    assertTrue(javaFiles.isEmpty());
  }

  @Test
  @DisplayName("findDirectory should find an existing directory")
  void findDirectory_whenDirectoryExists_shouldReturnPath(@TempDir Path tempDir)
      throws IOException {
    Path subDir = tempDir.resolve("my_dir");
    Files.createDirectory(subDir);

    Optional<Path> foundDir = pathHelper.findDirectory(tempDir, "my_dir");

    assertTrue(foundDir.isPresent());
    assertEquals(subDir, foundDir.get());
  }

  @Test
  @DisplayName("findDirectory should return empty for a non-existent directory")
  void findDirectory_whenDirectoryDoesNotExist_shouldReturnEmpty(@TempDir Path tempDir) {
    Optional<Path> foundDir = pathHelper.findDirectory(tempDir, "non_existent_dir");
    assertFalse(foundDir.isPresent());
  }

  @Test
  @DisplayName("findDirectory should return empty if rootDir is null")
  void findDirectory_whenRootDirIsNull_shouldReturnEmpty() {
    Optional<Path> foundDir = pathHelper.findDirectory(null, "any_dir");
    assertFalse(foundDir.isPresent());
  }
}
