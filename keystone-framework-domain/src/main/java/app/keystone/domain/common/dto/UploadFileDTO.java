package app.keystone.domain.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@NoArgsConstructor
public class UploadFileDTO {

    public UploadFileDTO(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    private String imgUrl;

}
