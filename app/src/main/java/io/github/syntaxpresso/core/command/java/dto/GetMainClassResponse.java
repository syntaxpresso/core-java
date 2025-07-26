package io.github.syntaxpresso.core.command.java.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetMainClassResponse {
  private String filePath;
  private String packageName;
}
