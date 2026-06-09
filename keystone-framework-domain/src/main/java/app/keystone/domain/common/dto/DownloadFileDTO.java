package app.keystone.domain.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;

@Data
@AllArgsConstructor
public class DownloadFileDTO {

    private byte[] content;

    private HttpHeaders headers;
}
