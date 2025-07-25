package io.github.syntaxpresso.core;

import io.github.syntaxpresso.core.command.GenericCommand;
import io.github.syntaxpresso.core.command.JavaCommand;
import picocli.CommandLine;

@CommandLine.Command(subcommands = {JavaCommand.class, GenericCommand.class})
public class Core {
  public static void main(String[] args) {
    System.exit(new CommandLine(new Core()).execute(args));
  }
}
