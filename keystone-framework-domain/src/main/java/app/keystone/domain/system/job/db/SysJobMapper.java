package app.keystone.domain.system.job.db;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Scheduled job mapper.
 * @author likanug
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJobEntity> {
}
