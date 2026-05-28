package com.englishplatform.dto;

/**
 * WebRTC signaling message forwarded peer-to-peer via STOMP.
 *
 * type values: OFFER | ANSWER | ICE_CANDIDATE | MUTE_MIC | MUTE_CAM | MUTE_AUDIO
 */
public class SignalMessage {

    private String type;
    private Long fromUserId;
    private String fromNickname;
    private String toUserNickname;
    private Long roomId;
    /** JSON-encoded SDP string or ICE candidate object. */
    private String payload;

    public SignalMessage() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public String getFromNickname() { return fromNickname; }
    public void setFromNickname(String fromNickname) { this.fromNickname = fromNickname; }

    public String getToUserNickname() { return toUserNickname; }
    public void setToUserNickname(String toUserNickname) { this.toUserNickname = toUserNickname; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
