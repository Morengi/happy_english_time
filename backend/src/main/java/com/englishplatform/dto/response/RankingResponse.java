package com.englishplatform.dto.response;

import java.math.BigDecimal;

public class RankingResponse {
    private Long userId;
    private String fullName;
    private String nickname;
    private BigDecimal avgScore;
    private int rank;

    public RankingResponse(Long userId, String fullName, String nickname, BigDecimal avgScore, int rank) {
        this.userId = userId; this.fullName = fullName; this.nickname = nickname;
        this.avgScore = avgScore; this.rank = rank;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal v) { this.avgScore = v; }
    public int getRank() { return rank; }
    public void setRank(int v) { this.rank = v; }
}
