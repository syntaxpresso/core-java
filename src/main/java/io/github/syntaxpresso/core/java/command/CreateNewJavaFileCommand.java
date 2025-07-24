package io.github.syntaxpresso.core.java.command;

import io.github.syntaxpresso.core.java.command.dtos.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.java.command.enums.JavaFileTemplate;
import io.github.syntaxpresso.core.java.common.DataTransferObject;
import io.github.syntaxpresso.core.java.util.TreeSitterUtils;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create-new-java-file", description = "Create a new Java file")
public class CreateNewJavaFileCommand implements Callable<Void> {
  private final TreeSitterUtils treeSitterUtils = new TreeSitterUtils();

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

  @Override
  public Void call() throws Exception {
    String className = this.fileName.replace(".java", "");
    String template = this.fileType.getSourceContent(this.packageName, className);
    Boolean isSourceCodeValid = this.treeSitterUtils.isSourceCodeValid(template);
    if (isSourceCodeValid) {
      CreateNewJavaFileResponse payload = new CreateNewJavaFileResponse(template);
      System.out.println(DataTransferObject.success(payload));
    } else {
      String reason =
          "The generated code for " + this.fileName + " did not pass syntax validation.";
      System.out.println(DataTransferObject.error(reason));
    }
    return null;
  }
}
