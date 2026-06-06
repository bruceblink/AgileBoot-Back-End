package app.keystone.integrationTest;

import app.keystone.domain.system.config.db.SysConfigMapper;
import app.keystone.domain.system.config.db.SysConfigServiceImpl;
import app.keystone.domain.system.dept.db.SysDeptMapper;
import app.keystone.domain.system.dept.db.SysDeptServiceImpl;
import app.keystone.domain.system.dict.db.SysDictDataMapper;
import app.keystone.domain.system.dict.db.SysDictDataServiceImpl;
import app.keystone.domain.system.dict.db.SysDictTypeMapper;
import app.keystone.domain.system.dict.db.SysDictTypeServiceImpl;
import app.keystone.domain.system.log.db.SysLoginInfoMapper;
import app.keystone.domain.system.log.db.SysOperationLogMapper;
import app.keystone.domain.system.menu.db.SysMenuMapper;
import app.keystone.domain.system.menu.db.SysMenuServiceImpl;
import app.keystone.domain.system.notice.db.SysNoticeMapper;
import app.keystone.domain.system.post.db.SysPostMapper;
import app.keystone.domain.system.post.db.SysPostServiceImpl;
import app.keystone.domain.system.role.db.SysRoleMapper;
import app.keystone.domain.system.role.db.SysRoleMenuMapper;
import app.keystone.domain.system.role.db.SysRoleServiceImpl;
import app.keystone.domain.system.user.db.SysUserMapper;
import app.keystone.domain.system.user.db.SysUserServiceImpl;
import app.keystone.infrastructure.config.ApplicationConfig;
import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * 集成测试配置类
 * @author likanug
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = "app.keystone")
@ComponentScan(
    basePackages = "app.keystone",
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationConfig.class)
)
@Import({
    SysConfigServiceImpl.class,
    SysDeptServiceImpl.class,
    SysDictDataServiceImpl.class,
    SysDictTypeServiceImpl.class,
    SysMenuServiceImpl.class,
    SysPostServiceImpl.class,
    SysRoleServiceImpl.class,
    SysUserServiceImpl.class,
    IntegrationTestApplication.MapperBeanConfig.class
})
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }

    @Configuration
    static class MapperBeanConfig {

        private <T> T mapper(SqlSession sqlSession, Class<T> mapperType) {
            if (!sqlSession.getConfiguration().hasMapper(mapperType)) {
                sqlSession.getConfiguration().addMapper(mapperType);
            }
            return sqlSession.getMapper(mapperType);
        }

        @Bean
        SysConfigMapper sysConfigMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysConfigMapper.class);
        }

        @Bean
        SysDeptMapper sysDeptMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysDeptMapper.class);
        }

        @Bean
        SysDictDataMapper sysDictDataMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysDictDataMapper.class);
        }

        @Bean
        SysDictTypeMapper sysDictTypeMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysDictTypeMapper.class);
        }

        @Bean
        SysMenuMapper sysMenuMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysMenuMapper.class);
        }

        @Bean
        SysPostMapper sysPostMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysPostMapper.class);
        }

        @Bean
        SysRoleMapper sysRoleMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysRoleMapper.class);
        }

        @Bean
        SysRoleMenuMapper sysRoleMenuMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysRoleMenuMapper.class);
        }

        @Bean
        SysUserMapper sysUserMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysUserMapper.class);
        }

        @Bean
        SysNoticeMapper sysNoticeMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysNoticeMapper.class);
        }

        @Bean
        SysLoginInfoMapper sysLoginInfoMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysLoginInfoMapper.class);
        }

        @Bean
        SysOperationLogMapper sysOperationLogMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysOperationLogMapper.class);
        }
    }
}
