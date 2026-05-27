package com.englishplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RankingResponse {
    private Long userId;
    private String fullName;
    private String nickname;
    private BigDecimal avgScore;
    private int rank;
}
