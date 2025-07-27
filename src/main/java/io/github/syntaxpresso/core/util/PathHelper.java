package io.github.syntaxpresso.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PathHelper {
  public File convertToFile(String filePath) {
    return new File(filePath);
  }

  public Optional<String> getFileSourceCode(File file) {
    try {
      String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      return Optional.of(content);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public Optional<String> getFileSourceCode(String filePath) {
    File convertedFilePath = this.convertToFile(filePath);
    return this.getFileSourceCode(convertedFilePath);
  }

  public boolean createFile(File file, String sourceCode) throws IOException {
    try {
      Files.writeString(file.toPath(), sourceCode, StandardCharsets.UTF_8);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public List<File> findFilesByExtention(File cwd, String extension) throws IOException {
    Path rootDir = cwd.toPath();
    try (Stream<Path> stream = Files.walk(rootDir)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith("." + extension))
          .map(Path::toFile)
          .collect(Collectors.toList());
    }
  }

  public Optional<File> findDirectoryRecursively(File rootDir, String dirName) {
    if (rootDir == null || !rootDir.isDirectory() || dirName == null) {
      return Optional.empty();
    }
    Path rootPath = rootDir.toPath();
    try (Stream<Path> stream = Files.walk(rootPath)) {
      Optional<Path> foundPathOptional =
          stream
              .filter(Files::isDirectory)
              .filter(path -> path.toString().endsWith(dirName))
              .findFirst();
      if (foundPathOptional.isPresent()) {
        Path foundPath = foundPathOptional.get();
        return Optional.of(foundPath.toFile());
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
