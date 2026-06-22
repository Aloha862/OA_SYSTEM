package com.example.oa.module.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class DictData extends BaseEntity {

    private String typeCode;
    private String dictLabel;
    private String dictValue;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
