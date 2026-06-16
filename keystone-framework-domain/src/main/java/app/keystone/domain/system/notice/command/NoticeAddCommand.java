package app.keystone.domain.system.notice.command;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class NoticeAddCommand {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题不能超过50个字符")
    protected String noticeTitle;

    @NotNull(message = "公告类型不能为空")
    @Min(value = 1, message = "公告类型值无效")
    @Max(value = 2, message = "公告类型值无效")
    protected Integer noticeType;

    /**
     * 想要支持富文本的话, 避免Xss过滤的话， 请加上@JsonDeserialize(using = StringDeserializer.class) 注解
     */
    @NotBlank(message = "公告内容不能为空")
    @JsonDeserialize(using = StringDeserializer.class)
    protected String noticeContent;

    @NotNull(message = "公告状态不能为空")
    @Min(value = 0, message = "公告状态值无效")
    @Max(value = 1, message = "公告状态值无效")
    protected Integer status;

}
