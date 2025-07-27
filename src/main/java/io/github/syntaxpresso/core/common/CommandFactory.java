package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.command.java.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.java.GetMainClassCommand;
import io.github.syntaxpresso.core.service.JavaService;
import io.github.syntaxpresso.core.util.PathHelper;
import io.github.syntaxpresso.core.util.TSHelper;
import org.treesitter.TreeSitterJava;
import picocli.CommandLine.IFactory;

public class CommandFactory implements IFactory {
  private final PathHelper pathHelper = new PathHelper();
  private final TSHelper javaTsHelper = new TSHelper(new TreeSitterJava(), pathHelper);
  private final JavaService javaService = new JavaService(pathHelper, javaTsHelper);

  @Override
  @SuppressWarnings("unchecked")
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == CreateNewFileCommand.class) {
      return (K) new CreateNewFileCommand(javaService);
    }
    if (cls == GetMainClassCommand.class) {
      return (K) new GetMainClassCommand(javaService);
    }
    return cls.getDeclaredConstructor().newInstance();
  }
}
