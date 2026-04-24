package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("error_type_config")
public class ErrorTypeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sortOrder;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
