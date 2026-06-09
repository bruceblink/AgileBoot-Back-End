package app.keystone.domain.system.dict.db;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 字典数据 服务接口
 * @author likanug
 */
public interface SysDictDataService extends IService<SysDictDataEntity> {

    List<SysDictDataEntity> listByDictType(String dictType);

    void deleteByDictType(String dictType);
}
