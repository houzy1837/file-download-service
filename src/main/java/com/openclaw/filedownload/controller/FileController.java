package com.openclaw.filedownload.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class FileController {
    
    @Value("${app.workspace-dir}")
    private String workspaceDir;
    
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listFiles(@RequestParam(defaultValue = "") String path) {
        try {
            File dir = path.isEmpty() ? new File(workspaceDir) : new File(path);
            
            // 安全检查：确保路径在 workspace 内
            String canonicalPath = dir.getCanonicalPath();
            if (!canonicalPath.startsWith(workspaceDir)) {
                return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "访问被拒绝：只能访问 workspace 目录")
                );
            }
            
            File[] files = dir.listFiles();
            List<Map<String, Object>> fileDTOs = new ArrayList<>();
            
            if (files != null) {
                for (File file : files) {
                    // 跳过隐藏文件和 git 目录
                    if (file.getName().startsWith(".") || file.getName().equals("node_modules")) {
                        continue;
                    }
                    
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("name", file.getName());
                    fileMap.put("path", file.getAbsolutePath());
                    fileMap.put("directory", file.isDirectory());
                    fileMap.put("size", file.isDirectory() ? 0 : file.length());
                    fileMap.put("formattedSize", formatFileSize(file.length(), file.isDirectory()));
                    fileMap.put("lastModified", file.lastModified());
                    fileDTOs.add(fileMap);
                }
            }
            
            // 按目录优先，然后按名称排序
            fileDTOs.sort((a, b) -> {
                boolean dirA = (Boolean) a.get("directory");
                boolean dirB = (Boolean) b.get("directory");
                if (dirA && !dirB) return -1;
                if (!dirA && dirB) return 1;
                return ((String) a.get("name")).compareTo((String) b.get("name"));
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentPath", dir.getAbsolutePath());
            response.put("parentPath", dir.getParent());
            response.put("workspaceDir", workspaceDir);
            response.put("files", fileDTOs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "message", "读取目录失败：" + e.getMessage())
            );
        }
    }
    
    @GetMapping("/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> downloadFile(@RequestParam String file) {
        try {
            Path filePath = Paths.get(file).normalize();
            
            // 安全检查
            String canonicalPath = filePath.toFile().getCanonicalPath();
            if (!canonicalPath.startsWith(workspaceDir)) {
                return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "访问被拒绝")
                );
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = "application/octet-stream";
            String fileName = resource.getFilename();
            
            if (fileName != null) {
                if (fileName.endsWith(".pdf")) contentType = "application/pdf";
                else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) 
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                    contentType = "image/" + fileName.substring(fileName.lastIndexOf(".") + 1);
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
                
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "message", "无效的文件路径")
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "message", "下载失败：" + e.getMessage())
            );
        }
    }
    
    private String formatFileSize(long size, boolean isDirectory) {
        if (isDirectory) return "-";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}
