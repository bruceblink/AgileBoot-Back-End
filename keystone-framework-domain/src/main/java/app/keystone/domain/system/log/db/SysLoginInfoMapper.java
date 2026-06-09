package app.keystone.domain.system.log.db;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统访问记录 Mapper 接口
 * </p>
 *
 * @author likanug
 * @since 2022-06-06
 */
@Mapper
public interface SysLoginInfoMapper extends BaseMapper<SysLoginInfoEntity> {

}
