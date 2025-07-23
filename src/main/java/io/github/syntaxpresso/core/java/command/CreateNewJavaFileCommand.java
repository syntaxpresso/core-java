package io.github.syntaxpresso.core.java.command;

import io.github.syntaxpresso.core.java.command.dtos.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.java.command.enums.JavaFileTemplate;
import io.github.syntaxpresso.core.java.common.DataTransferObject;
import io.github.syntaxpresso.core.java.util.TreeSitterUtils;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create-new-java-file", description = "Create a new Java file")
public class CreateNewJavaFileCommand implements Callable<Integer> {
  private static final Logger log = Logger.getLogger(CreateNewJavaFileCommand.class.getName());

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
  public Integer call() throws Exception {
    log.info(
        "Creating new Java file with package: "
            + packageName
            + ", file: "
            + fileName
            + ", type: "
            + fileType);
    String className = this.fileName.replace(".java", "");
    String template = this.fileType.getSourceContent(this.packageName, className);
    TreeSitterUtils treeSitterUtils = new TreeSitterUtils();
    if (treeSitterUtils.isSourceCodeValid(template)) {
      log.info("Generated source code is valid.");
      CreateNewJavaFileResponse payload = new CreateNewJavaFileResponse(template);
      System.out.println(DataTransferObject.success(payload));
      return 0;
    } else {
      log.severe("Generated source code is invalid.");
      String reason =
          "The generated code for " + this.fileName + " did not pass syntax validation.";
      System.out.println(DataTransferObject.error(reason));
    }
    return 1;
  }
}
