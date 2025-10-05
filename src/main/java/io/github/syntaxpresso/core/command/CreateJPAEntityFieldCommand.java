package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFieldTemporal;
import io.github.syntaxpresso.core.command.extra.JavaFieldTimeZoneStorage;
import io.github.syntaxpresso.core.command.extra.JavaFieldType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateJPAEntityFieldCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "create-jpa-entity-field",
    description = "Create JPA Repository for the current JPA Entity.")
public class CreateJPAEntityFieldCommand
    implements Callable<DataTransferObject<CreateNewFileResponse>> {
  private final CreateJPAEntityFieldCommandService createJPAEntityFieldCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(
      names = "--file-path",
      description = "The absolute path to the JPA Entity file",
      required = true)
  private Path filePath;

  @Option(names = "--type", description = "The JPA field type (BASIC, ENUM, ID)", required = false)
  private JavaFieldType type;

  @Option(names = "--field-name", description = "The name of the field to create", required = false)
  private String fieldName;

  @Option(
      names = "--field-length",
      description = "The maximum length for string fields",
      required = false)
  private Integer fieldLength;

  @Option(
      names = "--field-precision",
      description = "The precision for decimal fields",
      required = false)
  private Integer fieldPrecision;

  @Option(names = "--field-scale", description = "The scale for decimal fields", required = false)
  private Integer fieldScale;

  @Option(
      names = "--field-temporal",
      description = "The temporal type for date/time fields",
      required = false)
  private JavaFieldTemporal fieldTemporal;

  @Option(
      names = "--field-time-zone-storage",
      description = "The time zone storage configuration for timestamp fields",
      required = false)
  private JavaFieldTimeZoneStorage fieldTimeZoneStorage;

  @Option(names = "--field-type", description = "The Java type of the field", required = false)
  private String fieldType;

  @Option(
      names = "--field-type-package-name",
      description = "The package name for the field type if it's a custom class",
      required = false)
  private String fieldTypePackageName;

  @Option(
      names = "--language",
      description = "The language related to the command execution.",
      required = true)
  private SupportedLanguage language;

  @Option(
      names = "--ide",
      description = "The IDE the command is being called from.",
      required = true)
  private SupportedIDE ide = SupportedIDE.NONE;

  @Option(
      names = "--field-nullable",
      description = "Whether the field can be null",
      required = false)
  private boolean fieldNullable = true;

  @Option(
      names = "--field-unique",
      description = "Whether the field value must be unique",
      required = false)
  private boolean fieldUnique = false;

  @Option(
      names = "--field-updatable",
      description = "Whether the field can be updated",
      required = false)
  private boolean fieldUpdatable = true;

  @Option(
      names = "--field-large-object",
      description = "Whether the field is a large object (LOB)",
      required = false)
  private boolean fieldLargeObject = false;

  @Override
  public DataTransferObject<CreateNewFileResponse> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {
      DataTransferObject<Void> result =
          this.createJPAEntityFieldCommandService.run(
              this.cwd,
              this.filePath,
              this.language,
              this.ide,
              this.fieldName,
              this.fieldType,
              this.fieldTypePackageName,
              this.fieldLength,
              this.fieldPrecision,
              this.fieldScale,
              this.fieldTemporal,
              this.fieldTimeZoneStorage,
              this.fieldNullable,
              this.fieldUnique,
              this.fieldUpdatable,
              this.fieldLargeObject,
              this.type);

      if (result.getSucceed()) {
        return DataTransferObject.success(
            new CreateNewFileResponse("JPA Entity field created successfully"));
      } else {
        return DataTransferObject.error(result.getErrorReason());
      }
    }
    return DataTransferObject.error("Language not supported.");
  }
}
