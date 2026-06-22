package com.example.oa.module.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictTypeRequest {

    @NotBlank(message = "字典类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "字典类型名称不能为空")
    private String typeName;

    private String remark;
    private Integer status;
}
