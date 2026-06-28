package app.keystone.domain.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {

    private String token;

    private String refreshToken;

    private Long expiresIn;

    private Long refreshExpiresIn;

    private CurrentLoginUserDTO currentUser;

}
