package io.github.syntaxpresso.core;

import io.github.syntaxpresso.core.common.CommandFactory;
import io.github.syntaxpresso.core.command.GenericCommand;
import io.github.syntaxpresso.core.command.JavaCommand;
import picocli.CommandLine;

@CommandLine.Command(
    subcommands = {
      JavaCommand.class,
      GenericCommand.class,
    })
public class Core {
  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new Core(), new CommandFactory());
    System.exit(commandLine.execute(args));
  }
}
