package app.keystone.domain.system.job.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * Scheduled job service implementation.
 * @author likanug
 */
@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJobEntity> implements SysJobService {
}
