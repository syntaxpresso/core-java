package io.github.syntaxpresso.core.command.java;

import com.google.common.io.Files;
import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.JavaService;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "create-new-file", description = "Create a new Java file")
public class CreateNewJavaFileCommand implements Callable<Void> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private File cwd;

  @Option(
      names = "--package-name",
      description = "The package name for the new file",
      required = true)
  private String packageName;

  @Option(names = "--file-name", description = "The name of the new file", required = true)
  private String fileName;

  @Option(
      names = "--file-type",
      description = "The type of the new file (CLASS, INTERFACE, RECORD, ENUM, ANNOTATION)",
      required = true)
  private JavaFileTemplate fileType;

  @Option(
      names = "--source-directory-type",
      description =
          "Defines if the file should be created in the main or in the test directory (MAIN, TEST)",
      required = false)
  private SourceDirectoryType sourceDirectoryType = SourceDirectoryType.MAIN;

  @Override
  public Void call() throws Exception {
    String className = fileName.trim();
    className = Files.getNameWithoutExtension(className);
    String template = this.fileType.getSourceContent(this.packageName, className);
    boolean isSourceCodeValid = this.javaService.getTsHelper().isSourceCodeValid(template);
    if (isSourceCodeValid) {
      Optional<File> filePath =
          this.javaService.findFilePath(this.cwd, this.packageName, sourceDirectoryType);
      if (filePath.isEmpty()) {
        System.out.println(DataTransferObject.error("Unable to find file path."));
        return null;
      }
      File fileToCreate = filePath.get().toPath().resolve(className.concat(".java")).toFile();
      Boolean fileCreated = this.javaService.getPathHelper().createFile(fileToCreate, template);
      if (fileCreated) {
        CreateNewJavaFileResponse response =
            CreateNewJavaFileResponse.builder().filePath(fileToCreate.getAbsolutePath()).build();
        System.out.println(DataTransferObject.success(response));
        return null;
      }
      System.out.println(DataTransferObject.error("Unable to create file."));
    } else {
      System.out.println(
          DataTransferObject.error(
              "The generated code for " + this.fileName + " did not pass syntax validation."));
    }
    return null;
  }
}
