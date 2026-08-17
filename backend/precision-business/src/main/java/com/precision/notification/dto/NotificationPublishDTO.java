package com.precision.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发布站内通知入参。
 */
public class NotificationPublishDTO {

    /** 目标用户；不传 = 当前租户内广播 */
    private Long userId;

    /** 类型，默认 SYSTEM；业务方可自定义并用它做筛选 */
    @Size(max = 30, message = "类型长度不能超过30")
    private String type;

    /** 级别 1紧急 2严重 3一般 4提示；≤2 会额外触发短信 */
    @Min(value = 1, message = "级别取值 1-4")
    @Max(value = 4, message = "级别取值 1-4")
    private Integer level;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Size(max = 1000, message = "内容长度不能超过1000")
    private String content;

    @Size(max = 200, message = "业务关联键长度不能超过200")
    private String bizRef;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getBizRef() { return bizRef; }
    public void setBizRef(String bizRef) { this.bizRef = bizRef; }
}
