package app.keystone.domain.system.dict.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 字典数据 服务实现类
 * @author likanug
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictDataEntity>
    implements SysDictDataService {

    @Override
    public List<SysDictDataEntity> listByDictType(String dictType) {
        LambdaQueryWrapper<SysDictDataEntity> wrapper = new LambdaQueryWrapper<SysDictDataEntity>()
            .eq(SysDictDataEntity::getDictType, dictType)
            .orderByAsc(SysDictDataEntity::getDictSort);
        return this.list(wrapper);
    }

    @Override
    public void deleteByDictType(String dictType) {
        LambdaUpdateWrapper<SysDictDataEntity> wrapper = new LambdaUpdateWrapper<SysDictDataEntity>()
            .eq(SysDictDataEntity::getDictType, dictType);
        this.remove(wrapper);
    }
}
