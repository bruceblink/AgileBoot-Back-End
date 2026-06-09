package app.keystone.admin.controller.common;

import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.common.FileApplicationService;
import app.keystone.domain.common.dto.DownloadFileDTO;
import app.keystone.domain.common.dto.UploadDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用请求处理
 * @author likanug
 */
@Tag(name = "上传API", description = "上传相关接口")
@RestController
@RequestMapping("/file")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final FileApplicationService fileApplicationService;

    /**
     * 通用下载请求
     * download接口  其实不是很有必要
     * @param fileName 文件名称
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public ResponseEntity<byte[]> fileDownload(String fileName, HttpServletResponse response) {
        try {
            DownloadFileDTO downloadFile = fileApplicationService.download(fileName);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return new ResponseEntity<>(downloadFile.getContent(), downloadFile.getHeaders(), HttpStatus.OK);
        } catch (ApiException e) {
            ResponseDTO<Object> fail = ResponseDTO.fail(e);
            return new ResponseEntity<>(JacksonUtil.to(fail).getBytes(StandardCharsets.UTF_8), null, HttpStatus.OK);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            ResponseDTO<Object> fail = ResponseDTO.fail(
                new ApiException(ErrorCode.Internal.INTERNAL_ERROR, "文件下载失败"));
            return new ResponseEntity<>(JacksonUtil.to(fail).getBytes(StandardCharsets.UTF_8), null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @Operation(summary = "单个上传文件")
    @PostMapping("/upload")
    public ResponseDTO<UploadDTO> uploadFile(MultipartFile file) {
        return ResponseDTO.ok(fileApplicationService.upload(file));
    }

    /**
     * 通用上传请求（多个）
     */
    @Operation(summary = "多个上传文件")
    @PostMapping("/uploads")
    public ResponseDTO<List<UploadDTO>> uploadFiles(List<MultipartFile> files) {
        return ResponseDTO.ok(fileApplicationService.uploadBatch(files));
    }

}
