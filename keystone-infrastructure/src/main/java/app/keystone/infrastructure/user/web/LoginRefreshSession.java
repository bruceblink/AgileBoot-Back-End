package app.keystone.infrastructure.user.web;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端保存的 refresh 会话。
 */
@Data
@NoArgsConstructor
public class LoginRefreshSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String refreshSessionId;

    private String refreshTokenHash;

    private String accountId;

    private Long userId;

    private String username;

    private String currentTokenId;

    private SystemLoginUser loginUser;

    private long issuedAt;

    private long expiresAt;

    private boolean revoked;

    public boolean isExpired(long currentTimeMillis) {
        return expiresAt <= currentTimeMillis;
    }
}
