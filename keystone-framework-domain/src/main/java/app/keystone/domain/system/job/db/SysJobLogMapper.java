package app.keystone.domain.system.job.db;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Scheduled job execution log mapper.
 *
 * @author likanug
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogEntity> {
}
