package com.example.oa.module.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictDataRequest {

    @NotBlank(message = "字典类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    private String dictValue;

    private Integer sortOrder;
    private Integer status;
    private String remark;
}
