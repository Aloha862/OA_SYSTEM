package com.example.oa.module.file.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.config.FileProperties;
import com.example.oa.module.file.dto.FileQueryRequest;
import com.example.oa.module.file.entity.OaFile;
import com.example.oa.module.file.mapper.OaFileMapper;
import com.example.oa.module.file.service.OaFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OaFileServiceImpl extends ServiceImpl<OaFileMapper, OaFile> implements OaFileService {

    private final FileProperties fileProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaFile upload(MultipartFile file, String businessType, Long businessId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        try {
            LocalDate now = LocalDate.now();
            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String extension = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                extension = originalName.substring(dot + 1);
            }
            String storedName = IdUtil.fastSimpleUUID() + (StringUtils.hasText(extension) ? "." + extension : "");
            Path directory = Path.of(fileProperties.getUploadPath(), String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName);
            file.transferTo(target);

            String relative = now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + storedName;
            OaFile oaFile = new OaFile();
            oaFile.setOriginalName(originalName);
            oaFile.setFileName(storedName);
            oaFile.setFilePath(target.toAbsolutePath().toString());
            oaFile.setFileUrl(fileProperties.getAccessPrefix() + "/" + relative);
            oaFile.setFileSize(file.getSize());
            oaFile.setFileType(file.getContentType());
            oaFile.setExtension(extension);
            oaFile.setBusinessType(businessType);
            oaFile.setBusinessId(businessId);
            oaFile.setUploaderId(SecurityUtils.currentUserId());
            oaFile.setDownloadCount(0);
            save(oaFile);
            return oaFile;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<OaFile> pageFiles(FileQueryRequest request) {
        Page<OaFile> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<OaFile>()
                        .eq(StringUtils.hasText(request.getBusinessType()), OaFile::getBusinessType, request.getBusinessType())
                        .eq(request.getBusinessId() != null, OaFile::getBusinessId, request.getBusinessId())
                        .eq(request.getUploaderId() != null, OaFile::getUploaderId, request.getUploaderId())
                        .orderByDesc(OaFile::getId));
        return PageResult.of(page);
    }

    @Override
    public java.util.List<OaFile> businessFiles(String businessType, Long businessId) {
        return list(new LambdaQueryWrapper<OaFile>()
                .eq(OaFile::getBusinessType, businessType)
                .eq(OaFile::getBusinessId, businessId)
                .orderByDesc(OaFile::getId));
    }

    @Override
    public Resource download(Long id) {
        OaFile oaFile = getRequired(id);
        oaFile.setDownloadCount((oaFile.getDownloadCount() == null ? 0 : oaFile.getDownloadCount()) + 1);
        updateById(oaFile);
        FileSystemResource resource = new FileSystemResource(oaFile.getFilePath());
        if (!resource.exists()) {
            throw new BusinessException("文件不存在或已被移动");
        }
        return resource;
    }

    @Override
    public void deleteFile(Long id) {
        OaFile file = getRequired(id);
        ensureUploaderOrAdmin(file);
        if (getById(id) == null) {
            throw new BusinessException("文件不存在");
        }
        removeById(id);
    }

    private OaFile getRequired(Long id) {
        OaFile file = getById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        return file;
    }

    private void ensureUploaderOrAdmin(OaFile file) {
        if (!SecurityUtils.isAdmin() && !Objects.equals(file.getUploaderId(), SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "Only the uploader can delete this file");
        }
    }
}
