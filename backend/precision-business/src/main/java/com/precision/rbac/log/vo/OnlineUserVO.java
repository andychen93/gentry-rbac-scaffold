package com.precision.rbac.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnlineUserVO {
    private String tokenId;
    private Long userId;
    private String username;
    private String nickname;
    private String deptName;
    private String loginIp;
    private String location;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
}
