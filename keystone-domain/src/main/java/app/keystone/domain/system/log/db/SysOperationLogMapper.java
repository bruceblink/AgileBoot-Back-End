package app.keystone.domain.system.log.db;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 操作日志记录 Mapper 接口
 * </p>
 *
 * @author likanug
 * @since 2022-06-08
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLogEntity> {

}
