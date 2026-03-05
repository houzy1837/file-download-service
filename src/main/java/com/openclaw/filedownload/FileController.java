package com.openclaw.filedownload;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class FileController {

    private static final String WORKSPACE_DIR = "/root/.openclaw/workspace";

    @GetMapping("/")
    public String listFiles(@RequestParam(defaultValue = "") String path, Model model) {
        File dir = path.isEmpty() ? new File(WORKSPACE_DIR) : new File(path);
        
        // 安全检查：确保路径在 workspace 内
        try {
            String canonicalPath = dir.getCanonicalPath();
            if (!canonicalPath.startsWith(WORKSPACE_DIR)) {
                model.addAttribute("error", "访问被拒绝：只能访问 workspace 目录");
                model.addAttribute("files", new ArrayList<>());
                return "index";
            }
        } catch (Exception e) {
            model.addAttribute("error", "无效路径");
            model.addAttribute("files", new ArrayList<>());
            return "index";
        }

        File[] files = dir.listFiles();
        List<FileDTO> fileDTOs = new ArrayList<>();
        
        if (files != null) {
            for (File file : files) {
                // 跳过隐藏文件和 git 目录
                if (file.getName().startsWith(".") || file.getName().equals("node_modules")) {
                    continue;
                }
                fileDTOs.add(new FileDTO(
                    file.getName(),
                    file.getAbsolutePath(),
                    file.isDirectory(),
                    file.length(),
                    file.lastModified()
                ));
            }
        }
        
        model.addAttribute("currentPath", dir.getAbsolutePath());
        model.addAttribute("parentPath", dir.getParent());
        model.addAttribute("files", fileDTOs);
        model.addAttribute("workspaceDir", WORKSPACE_DIR);
        return "index";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String file) {
        try {
            Path filePath = Paths.get(file).normalize();
            
            // 安全检查
            String canonicalPath = filePath.toFile().getCanonicalPath();
            if (!canonicalPath.startsWith(WORKSPACE_DIR)) {
                return ResponseEntity.badRequest().build();
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
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 内部类用于模板展示
    public static class FileDTO {
        public String name;
        public String path;
        public boolean directory;
        public long size;
        public long lastModified;

        public FileDTO(String name, String path, boolean directory, long size, long lastModified) {
            this.name = name;
            this.path = path;
            this.directory = directory;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getFormattedSize() {
            if (directory) return "-";
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
