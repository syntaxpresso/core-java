package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.java.CreateNewJavaFileCommand;
import io.github.syntaxpresso.core.command.java.GetMainClassCommand;
import picocli.CommandLine.Command;

@Command(
    name = "java",
    description = "Parent command for all Java-specific operations.",
    subcommands = {CreateNewJavaFileCommand.class, GetMainClassCommand.class})
public class JavaCommand {}
