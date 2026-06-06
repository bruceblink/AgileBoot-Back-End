package app.keystone.infrastructure.mybatisplus;

import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.base.BaseEntity;
import app.keystone.infrastructure.security.SecretValueDecryptor;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig.Builder;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.builder.Entity;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.fill.Property;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Data;

/**
 * @author likanug
 */
@Data
@lombok.Builder
public class CodeGenerator {

    private String author;
    private String module;
    private String tableName;
    private String databaseUrl;
    private String username;
    private String password;
    private String parentPackage;
    private Boolean isExtendsFromBaseEntity;

    /**
     * 避免覆盖掉原有生成的类  生成的类 放在orm子模块下的/target/generated-code目录底下
     * 有需要更新的实体自己在手动覆盖  或者 挪动过去
     */
    public static void main(String[] args) {
        String databaseUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL is required");
        }
        String username = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "root");
        String password = resolveEncryptedPassword(
            System.getenv("SPRING_DATASOURCE_PASSWORD_FILE"),
            System.getenv("KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE")
        );

        CodeGenerator generator = CodeGenerator.builder()
            .databaseUrl(databaseUrl)
            .username(username)
            .password(password)
            .author("valarchie")
            //生成的类 放在orm子模块下的/target/generated-code目录底下
            .module("/keystone-infrastructure/target/generated-code")
            .parentPackage("app.keystone")
            .tableName("sys_menu")
            // 决定是否继承基类
            .isExtendsFromBaseEntity(true)
            .build();

        generator.generateCode();
    }

    private static String resolveEncryptedPassword(String passwordFile, String encryptKeyFile) {
        if (passwordFile == null || passwordFile.isBlank()) {
            throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD_FILE is required");
        }
        if (encryptKeyFile == null || encryptKeyFile.isBlank()) {
            throw new IllegalStateException("KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE is required");
        }
        try {
            String encryptedPassword = Files.readString(Path.of(passwordFile.trim())).trim();
            String encryptKey = Files.readString(Path.of(encryptKeyFile.trim())).trim();
            if (!SecretValueDecryptor.isSecretV1(encryptedPassword)) {
                throw new IllegalStateException(
                    "Datasource password must use secret:v1:aes-256-gcm format: " + passwordFile);
            }
            return SecretValueDecryptor.decryptSecretV1(encryptedPassword, encryptKey);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt datasource password for code generator", ex);
        }
    }

    public void generateCode() {
        FastAutoGenerator generator = FastAutoGenerator.create(
            new Builder(databaseUrl, username, password)
//            .schema("mybatis-plus")
                // all these three options
                .dbQuery(new MySqlQuery())
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler()));

        globalConfig(generator);
        packageConfig(generator);
//        templateConfig(generator);
        injectionConfig(generator);
        strategyConfig(generator);
        // 默认的是Velocity引擎模板
        generator.templateEngine(new VelocityTemplateEngine());
        generator.execute();
    }


    /**
     * 为了避免  覆盖掉service中的方法
     * @param generator 生成器
     */
    private void globalConfig(FastAutoGenerator generator) {
        generator.globalConfig(
            builder -> builder
                .outputDir(System.getProperty("user.dir") + module + "/src/main/java")
                // use date type under package of java utils
                .dateType(DateType.ONLY_DATE)
                // 配置生成文件中的author
                .author(author)
//                    .enableKotlin()
                // generate swagger annotations.
                .enableSwagger()
                // 注释日期的格式
                .commentDate("yyyy-MM-dd"));
    }


    private void packageConfig(FastAutoGenerator generator) {
        generator.packageConfig(builder -> builder
            // parent package name
            .parent(parentPackage)
            .moduleName("orm")
            .entity("entity")
            .service("service")
            .serviceImpl("service.impl")
            .mapper("mapper")
            .xml("mapper.xml")
            .controller("controller"));
    }

    private void templateConfig(FastAutoGenerator generator) {
        //  customization code template. disable if you don't have specific requirement.
        generator.templateConfig(builder -> builder
            .disable(TemplateType.ENTITY)
            .entity("/templates/entity.java")
            .service("/templates/service.java")
            .serviceImpl("/templates/serviceImpl.java")
            .mapper("/templates/mapper.java")
            .xml("/templates/mapper.xml")
            .controller("/templates/controller.java"));
    }

    private void injectionConfig(FastAutoGenerator generator) {
        //  customization code template. disable if you don't have specific requirement.
        generator.injectionConfig(builder -> {
            // Customization
            builder.beforeOutputFile((tableInfo, objectMap) -> System.out.println("tableInfo: " +
                    tableInfo.getEntityName() + " objectMap: " + objectMap.size()));
        });
    }


    private void strategyConfig(FastAutoGenerator generator) {
        //  customization code template. disable if you don't have specific requirement.
        generator.strategyConfig(builder -> {
            builder
                // Global Configuration
                .enableCapitalMode()
                // does not generate view
                .enableSkipView()
                .disableSqlFilter()
                // filter which tables need to be generated
//                    .likeTable(new LikeTable("USER"))
//                    .addInclude("t_simple")
//                    .addTablePrefix("t_", "c_")
//                    .addFieldSuffix("_flag")
                .addInclude(tableName);

            entityConfig(builder);
            controllerConfig(builder);
            serviceConfig(builder);
            mapperConfig(builder);
        });
    }


    private void entityConfig(StrategyConfig.Builder builder) {
        Entity.Builder entityBuilder = builder.entityBuilder();

        entityBuilder
//                    .superClass(BaseEntity.class)
//                    .disableSerialVersionUID()
//                    .enableChainModel()
            .enableLombok()
            // boolean field
//                    .enableRemoveIsPrefix()
            .enableTableFieldAnnotation()
            // operate entity like JPA.
            .enableActiveRecord()
//                    .versionColumnName("version")
//                    .versionPropertyName("version")
            // deleted的字段设置成tinyint  长度为1
            .logicDeleteColumnName("deleted")
//                    .logicDeletePropertyName("deleteFlag")
            .naming(NamingStrategy.underline_to_camel)
            .columnNaming(NamingStrategy.underline_to_camel)
            // 如果不需要BaseEntity  请注释掉以下两行
//            .superClass(BaseEntity.class)
//            .addSuperEntityColumns("creator_id", "create_time", "creator_name", "updater_id", "update_time", "updater_name", "deleted")
//                    .addIgnoreColumns("age")
            // 两种配置方式 都可以
            .addTableFills(new Column("create_time", FieldFill.INSERT))
            .addTableFills(new Column("creator_id", FieldFill.INSERT))
            .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE))
            .addTableFills(new Property("updaterId", FieldFill.INSERT_UPDATE))
            // ID strategy AUTO, NONE, INPUT, ASSIGN_ID, ASSIGN_UUID;
            .idType(IdType.AUTO)
            .formatFileName("%sEntity");

        if (isExtendsFromBaseEntity) {
            entityBuilder
                .superClass(BaseEntity.class)
                .addSuperEntityColumns("creator_id", "create_time", "creator_name", "updater_id", "update_time",
                    "updater_name", "deleted");
        }

        entityBuilder.build();
    }


    private void controllerConfig(StrategyConfig.Builder builder) {
        builder.controllerBuilder()
            .superClass(BaseController.class)
            .enableHyphenStyle()
            .enableRestStyle()
            .formatFileName("%sController")
            .build();
    }

    private void serviceConfig(StrategyConfig.Builder builder) {
        builder.serviceBuilder()
//                    .superServiceClass(BaseService.class)
//                    .superServiceImplClass(BaseServiceImpl.class)
            .formatServiceFileName("%sService")
            .formatServiceImplFileName("%sServiceImpl")
            .build();
    }

    private void mapperConfig(StrategyConfig.Builder builder) {
        builder.mapperBuilder()
//                    .superClass(BaseMapper.class)
//                    .enableMapperAnnotation()
//                    .enableBaseResultMap()
//                    .enableBaseColumnList()
//                    .cache(MyMapperCache.class)
            .formatMapperFileName("%sMapper")
            .formatXmlFileName("%sMapper")
            .build();
    }


}
