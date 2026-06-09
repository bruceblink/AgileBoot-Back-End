package app.keystone.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.constant.Constants.UploadSubDir;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.domain.common.dto.DownloadFileDTO;
import app.keystone.domain.common.dto.UploadDTO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

class FileApplicationServiceTest {

    @TempDir
    private Path tempDir;

    private FileApplicationService fileApplicationService;

    @BeforeEach
    void setUp() {
        KeystoneConfig keystoneConfig = new KeystoneConfig();
        keystoneConfig.setFileBaseDir(tempDir.toString());
        keystoneConfig.init();
        fileApplicationService = new FileApplicationService();
    }

    @Test
    void uploadShouldRejectNullFile() {
        ApiException exception = assertThrows(ApiException.class, () -> fileApplicationService.upload(null));

        assertEquals(Business.UPLOAD_FILE_IS_EMPTY, exception.getErrorCode());
    }

    @Test
    void uploadShouldSaveFileAndReturnUploadDTO() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg",
            "content".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class)) {
            servletHolderUtilMocked.when(ServletHolderUtil::getContextUrl).thenReturn("http://localhost:18080");

            UploadDTO uploadDTO = fileApplicationService.upload(file);

            assertTrue(uploadDTO.getUrl().startsWith("http://localhost:18080/profile/upload/"));
            assertTrue(uploadDTO.getFileName().startsWith("/profile/upload/"));
            assertTrue(uploadDTO.getNewFileName().endsWith(".jpg"));
            assertEquals("avatar.jpg", uploadDTO.getOriginalFilename());
        }
    }

    @Test
    void uploadBatchShouldSkipNullEntries() {
        MockMultipartFile file = new MockMultipartFile("file", "report.txt", "text/plain",
            "hello".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class)) {
            servletHolderUtilMocked.when(ServletHolderUtil::getContextUrl).thenReturn("http://localhost:18080");

            List<UploadDTO> uploads = fileApplicationService.uploadBatch(List.of(file));

            assertEquals(1, uploads.size());
            assertEquals("report.txt", uploads.get(0).getOriginalFilename());
        }
    }

    @Test
    void uploadBatchShouldRejectEmptyList() {
        ApiException exception = assertThrows(ApiException.class, () -> fileApplicationService.uploadBatch(List.of()));

        assertEquals(Business.UPLOAD_FILE_IS_EMPTY, exception.getErrorCode());
    }

    @Test
    void downloadShouldRejectIllegalFileName() {
        ApiException exception = assertThrows(ApiException.class, () -> fileApplicationService.download("../secret.txt"));

        assertEquals(Business.COMMON_FILE_NOT_ALLOWED_TO_DOWNLOAD, exception.getErrorCode());
    }

    @Test
    void downloadShouldReturnFileContentAndHeaders() throws Exception {
        Path downloadDir = tempDir.resolve("profile").resolve(UploadSubDir.DOWNLOAD_PATH);
        Files.createDirectories(downloadDir);
        Files.writeString(downloadDir.resolve("readme.txt"), "download-body", StandardCharsets.UTF_8);

        DownloadFileDTO downloadFile = fileApplicationService.download("readme.txt");

        assertEquals("download-body", new String(downloadFile.getContent(), StandardCharsets.UTF_8));
        assertFalse(downloadFile.getHeaders().isEmpty());
        assertTrue(downloadFile.getHeaders().containsKey("Content-Disposition"));
    }
}
