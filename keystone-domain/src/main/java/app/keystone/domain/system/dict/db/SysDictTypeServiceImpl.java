package app.keystone.domain.system.dict.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 字典类型 服务实现类
 * @author likanug
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictTypeEntity>
    implements SysDictTypeService {

    @Override
    public boolean isDictTypeUnique(String dictType, Long excludeDictId) {
        LambdaQueryWrapper<SysDictTypeEntity> wrapper = new LambdaQueryWrapper<SysDictTypeEntity>()
            .eq(SysDictTypeEntity::getDictType, dictType)
            .ne(excludeDictId != null, SysDictTypeEntity::getDictId, excludeDictId);
        return !this.exists(wrapper);
    }
}
