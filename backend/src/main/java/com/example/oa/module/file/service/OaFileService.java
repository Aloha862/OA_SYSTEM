package com.example.oa.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.file.dto.FileQueryRequest;
import com.example.oa.module.file.entity.OaFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OaFileService extends IService<OaFile> {

    OaFile upload(MultipartFile file, String businessType, Long businessId);

    PageResult<OaFile> pageFiles(FileQueryRequest request);

    List<OaFile> businessFiles(String businessType, Long businessId);

    Resource download(Long id);

    void deleteFile(Long id);
}
