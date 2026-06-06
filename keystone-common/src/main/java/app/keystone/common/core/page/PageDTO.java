package app.keystone.common.core.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.Data;

/**
 * 分页模型类
 * @author likanug
 */
@Data
public class PageDTO<T> {
    /**
     * 总记录数
     */
    private Long total;

    /**
     * 列表数据
     */
    private List<T> rows;

    public PageDTO(List<T> list) {
        this.rows = new java.util.ArrayList<>(list);
        this.total = (long) list.size();
    }

    public PageDTO(Page<T> page) {
        this.rows = new java.util.ArrayList<>(page.getRecords());
        this.total = page.getTotal();
    }

    public PageDTO(List<T> list, Long count) {
        this.rows = new java.util.ArrayList<>(list);
        this.total = count;
    }

}
