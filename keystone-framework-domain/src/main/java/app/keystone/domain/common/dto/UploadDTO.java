package app.keystone.domain.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author likanug
 */
@Data
@Builder
public class UploadDTO {

    private String url;
    private String fileName;
    private String newFileName;
    private String originalFilename;

}
