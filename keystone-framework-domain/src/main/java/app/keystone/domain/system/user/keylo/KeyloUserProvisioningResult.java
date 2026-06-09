package app.keystone.domain.system.user.keylo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyloUserProvisioningResult {

    private String keyloSubject;

    private String keyloUserId;

    public boolean isEmpty() {
        return (keyloSubject == null || keyloSubject.isBlank())
            && (keyloUserId == null || keyloUserId.isBlank());
    }
}
