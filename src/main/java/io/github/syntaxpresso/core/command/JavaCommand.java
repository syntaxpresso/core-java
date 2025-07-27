package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.java.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.java.GetMainClassCommand;
import picocli.CommandLine.Command;

@Command(
    name = "java",
    description = "Parent command for all Java-specific operations.",
    subcommands = {CreateNewFileCommand.class, GetMainClassCommand.class})
public class JavaCommand {}
