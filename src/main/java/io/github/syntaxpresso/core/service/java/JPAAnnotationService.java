package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.service.java.jpa.ColumnAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.EntityAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.FieldAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.IdAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.LobAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.TemporalAnnotationService;
import io.github.syntaxpresso.core.service.java.jpa.TimeZoneStorageAnnotationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class JPAAnnotationService {
  private final ColumnAnnotationService columnAnnotationService;
  private final EntityAnnotationService entityAnnotationService;
  private final IdAnnotationService idAnnotationService;
  private final FieldAnnotationService fieldAnnotationService;
  private final TemporalAnnotationService temporalAnnotationService;
  private final TimeZoneStorageAnnotationService timeZoneStorageAnnotationService;
  private final LobAnnotationService lobAnnotationService;
}
