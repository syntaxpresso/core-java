package io.github.syntaxpresso.core.command.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.service.JavaService;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSTree;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "rename", description = "Rename a symbol and all its usages.")
public class RenameCommand implements Callable<Void> {

  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private File cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
  private File filePath;

  // @Option(names = "--line", description = "The line number of the symbol", required = true)
  // private int line;
  //
  // @Option(names = "--column", description = "The column number of the symbol", required = true)
  // private int column;
  //
  // @Option(names = "--new-name", description = "The new name for the symbol", required = true)
  // private String newName;

  @Override
  public Void call() {
    List<File> modifiedFiles = new ArrayList<>();
    File fileCopy = this.filePath;
    Optional<TSTree> treeCopy = this.javaService.getTsHelper().parse(fileCopy);
    if (!fileCopy.exists() || treeCopy.isEmpty()) {
      System.out.println("1");
      return null;
    }
    Optional<TSNode> node = this.javaService.getTsHelper().getNodeAtPosition(fileCopy, 7, 14);
    if (node.isEmpty()) {
      return null;
    }
    String currentName = this.javaService.getTsHelper().getIdentifierText(node.get(), fileCopy);
    if (Strings.isNullOrEmpty(currentName)) {
      return null;
    }

    JavaIdentifierType identifierType = this.javaService.getIdentifierType(node.get());
    if (identifierType.equals(JavaIdentifierType.CLASS_NAME)) {
      Optional<String> packageScopeName = this.javaService.getPackageName();
      if (packageScopeName.isEmpty()) {
        return null;
      }
      boolean nodeRenamed =
          this.javaService.getTsHelper().renameNode(this.filePath, node.get(), "NewName");
      if (!nodeRenamed) {
        return null;
      }
      File renamedFile =
          this.javaService.getPathHelper().renameFile(this.filePath, "NewName" + ".java");
      if (renamedFile == null) {
        return null;
      }
      List<TSNode> usages = this.javaService.findClassUsages(currentName, this.cwd);
      System.out.println(usages);
    }

    //
    // JavaIdentifierType classIdentifierType =
    //     this.javaService.getIdentifierType(this.filePath, 7, 14);
    // JavaIdentifierType fieldIdentifierType =
    //     this.javaService.getIdentifierType(this.filePath, 8, 18);
    // JavaIdentifierType methodIdentifierType =
    //     this.javaService.getIdentifierType(this.filePath, 11, 15);
    // JavaIdentifierType localVariableIdentifierType =
    //     this.javaService.getIdentifierType(this.filePath, 12, 13);
    // JavaIdentifierType formalParameterIdentifierType =
    //     this.javaService.getIdentifierType(this.filePath, 11, 23);
    //
    // System.out.println(classIdentifierType);
    // System.out.println(fieldIdentifierType);
    // System.out.println(methodIdentifierType);
    // System.out.println(localVariableIdentifierType);
    // System.out.println(formalParameterIdentifierType);
    return null;
  }
}
