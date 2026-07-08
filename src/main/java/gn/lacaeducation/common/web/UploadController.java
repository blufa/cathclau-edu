package gn.lacaeducation.common.web;

import gn.lacaeducation.common.MediaUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Generic authenticated file upload to Cloudinary (photos & documents). Returns the secure URL. */
@Tag(name = "Uploads", description = "Upload images and documents to Cloudinary")
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final MediaUploadService uploadService;

    @Operation(summary = "Upload a file (image or document); returns its secure URL")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "folder", required = false) String folder) {
        return Map.of("url", uploadService.upload(file, folder));
    }
}
