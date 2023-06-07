package com.subratsss.agorasdkvoicecall.agoraTokenGenerator;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
