package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("subject_config")
public class SubjectConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
