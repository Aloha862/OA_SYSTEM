package com.example.oa.module.file.controller;

import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.file.dto.FileQueryRequest;
import com.example.oa.module.file.entity.OaFile;
import com.example.oa.module.file.service.OaFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class OaFileController {

    private final OaFileService fileService;

    @PostMapping("/upload")
    public Result<OaFile> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String businessType,
                                 @RequestParam(required = false) Long businessId) {
        return Result.success(fileService.upload(file, businessType, businessId));
    }

    @GetMapping("/{id}")
    public Result<OaFile> detail(@PathVariable Long id) {
        return Result.success(fileService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<OaFile>> page(FileQueryRequest request) {
        return Result.success(fileService.pageFiles(request));
    }

    @GetMapping("/business")
    public Result<List<OaFile>> business(@RequestParam String businessType, @RequestParam Long businessId) {
        return Result.success(fileService.businessFiles(businessType, businessId));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        OaFile file = fileService.getById(id);
        Resource resource = fileService.download(id);
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success(null);
    }
}
