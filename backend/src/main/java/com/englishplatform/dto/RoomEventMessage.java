package com.englishplatform.dto;

/**
 * Broadcast room event forwarded to all participants via STOMP topic.
 *
 * type values: JOIN | LEAVE | ROOM_ENDED | PARTICIPANTS_UPDATED
 */
public class RoomEventMessage {

    private String type;
    private Long roomId;
    private Long userId;
    private String userNickname;
    private String userFullName;
    private String userAvatarUrl;

    public RoomEventMessage() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
}
