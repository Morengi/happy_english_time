package com.englishplatform.dto.request;

import com.englishplatform.entity.WordSourceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WordRequest {
    @NotBlank
    private String englishWord;
    @NotBlank
    private String russianTranslation;
    private WordSourceType sourceType = WordSourceType.PERSONAL;
    private Long lessonId;
    private Long groupId;
}
