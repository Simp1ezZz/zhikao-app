package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("llm_model_config")
public class LlmModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String apiKey;
    private String baseUrl;
    private String model;
    private String provider;
    private Boolean isActive;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
