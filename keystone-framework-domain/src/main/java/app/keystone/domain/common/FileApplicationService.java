package app.keystone.domain.common;

import app.keystone.common.constant.Constants.UploadSubDir;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.file.FileUploadUtils;
import app.keystone.domain.common.dto.DownloadFileDTO;
import app.keystone.domain.common.dto.UploadDTO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileApplicationService {

    public DownloadFileDTO download(String fileName) {
        if (!FileUploadUtils.isAllowDownload(fileName)) {
            throw new ApiException(Business.COMMON_FILE_NOT_ALLOWED_TO_DOWNLOAD, fileName);
        }

        try {
            String filePath = FileUploadUtils.getFileAbsolutePath(UploadSubDir.DOWNLOAD_PATH, fileName);
            Path path = Paths.get(filePath);
            return new DownloadFileDTO(Files.readAllBytes(path), FileUploadUtils.getDownloadHeader(fileName));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Internal.INTERNAL_ERROR, "文件下载失败");
        }
    }

    public UploadDTO upload(MultipartFile file) {
        if (file == null) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_IS_EMPTY);
        }

        String fileName = FileUploadUtils.upload(UploadSubDir.UPLOAD_PATH, file);
        return buildUploadDTO(file, fileName);
    }

    public List<UploadDTO> uploadBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_IS_EMPTY);
        }

        List<UploadDTO> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null) {
                uploads.add(upload(file));
            }
        }
        return uploads;
    }

    private UploadDTO buildUploadDTO(MultipartFile file, String fileName) {
        String url = ServletHolderUtil.getContextUrl() + fileName;
        return UploadDTO.builder()
            .url(url)
            .fileName(fileName)
            .newFileName(FilenameUtils.getName(fileName))
            .originalFilename(file.getOriginalFilename())
            .build();
    }
}
