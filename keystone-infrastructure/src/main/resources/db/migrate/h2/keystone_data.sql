/*
Navicat MySQL Data Transfer

Source Server         : Local-Mysql
Source Server Version : 80029
Source Host           : localhost:33066
Source Database       : keystone

Target Server Type    : MYSQL
Target Server Version : 80029
File Encoding         : 65001

Date: 2022-10-07 16:05:40
*/

-- SET FOREIGN_KEY_CHECKS=0;


-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('1', '主框架页-默认皮肤样式名称', 'sys.index.skinName', '["skin-blue","skin-green","skin-purple","skin-red","skin-yellow"]', 'skin-blue', '1', null, null,  '2022-08-28 22:12:19', '2022-05-21 08:30:55', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow', '0');
INSERT INTO `sys_config` VALUES ('2', '用户管理-账号初始密码', 'sys.user.initPassword', '', '1234567', '1', null, null,  '2022-08-28 21:54:19', '2022-05-21 08:30:55', '初始化密码 123456', '0');
INSERT INTO `sys_config` VALUES ('3', '主框架页-侧边栏主题', 'sys.index.sideTheme', '["theme-dark","theme-light"]', 'theme-dark', '1', null,  null,  '2022-08-28 22:12:15', '2022-08-20 08:30:55', '深色主题theme-dark，浅色主题theme-light', '0');
INSERT INTO `sys_config` VALUES ('4', '账号自助-验证码开关', 'sys.account.captchaOnOff', '["true","false"]', 'false', '0', null,  null,  '2022-08-28 22:03:37', '2022-05-21 08:30:55', '是否开启验证码功能（true开启，false关闭）', '0');
INSERT INTO `sys_config` VALUES ('5', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', '["true","false"]', 'true', '0', null,  '1',  '2022-10-05 22:18:57', '2022-05-21 08:30:55', '是否开启注册用户功能（true开启，false关闭）', '0');


-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, remark, creator_id, updater_id, create_time, update_time, deleted)
VALUES (1, '系统是否', 'common.yesOrNo', 1, '系统是否列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (2, '通用状态', 'common.status', 1, '通用状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (3, '用户性别', 'sysUser.sex', 1, '用户性别列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (4, '用户状态', 'sysUser.status', 1, '用户状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (5, '菜单显示状态', 'sysMenu.isVisible', 1, '菜单显示状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (6, '通知类型', 'sysNotice.noticeType', 1, '通知类型列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (7, '通知状态', 'sysNotice.status', 1, '通知状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (8, '操作业务类型', 'sysOperationLog.businessType', 1, '操作业务类型列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (9, '操作状态', 'sysOperationLog.status', 1, '操作状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (10, '操作者类型', 'sysOperationLog.operatorType', 1, '操作者类型列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (11, '登录状态', 'sysLoginLog.status', 1, '登录状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (12, '任务状态', 'sysJob.status', 1, '任务状态列表', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO sys_dict_data (dict_code, dict_type, dict_label, dict_value, dict_sort, is_default, list_class, status, remark, creator_id, updater_id, create_time, update_time, deleted)
VALUES (1, 'common.yesOrNo', '是', '1', 1, 0, '', 1, '是', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (2, 'common.yesOrNo', '否', '0', 2, 0, 'danger', 1, '否', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (3, 'common.status', '正常', '1', 1, 0, '', 1, '正常状态', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (4, 'common.status', '停用', '0', 2, 0, 'danger', 1, '停用状态', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (5, 'sysUser.sex', '男', '1', 1, 0, '', 1, '性别男', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (6, 'sysUser.sex', '女', '2', 2, 0, '', 1, '性别女', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (7, 'sysUser.sex', '未知', '0', 3, 0, '', 1, '性别未知', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (8, 'sysUser.status', '正常', '1', 1, 0, '', 1, '用户正常', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (9, 'sysUser.status', '禁用', '2', 2, 0, 'danger', 1, '用户禁用', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (10, 'sysUser.status', '冻结', '3', 3, 0, 'warning', 1, '用户冻结', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (11, 'sysMenu.isVisible', '显示', '1', 1, 0, '', 1, '显示菜单', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (12, 'sysMenu.isVisible', '隐藏', '0', 2, 0, 'danger', 1, '隐藏菜单', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (13, 'sysNotice.noticeType', '通知', '1', 1, 0, 'warning', 1, '通知', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (14, 'sysNotice.noticeType', '公告', '2', 2, 0, 'success', 1, '公告', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (15, 'sysNotice.status', '正常', '1', 1, 0, '', 1, '正常状态', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (16, 'sysNotice.status', '关闭', '0', 2, 0, 'danger', 1, '关闭状态', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (17, 'sysOperationLog.businessType', '其他操作', '0', 1, 0, 'info', 1, '其他操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (18, 'sysOperationLog.businessType', '添加', '1', 2, 0, '', 1, '添加操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (19, 'sysOperationLog.businessType', '修改', '2', 3, 0, '', 1, '修改操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (20, 'sysOperationLog.businessType', '删除', '3', 4, 0, 'danger', 1, '删除操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (21, 'sysOperationLog.businessType', '授权', '4', 5, 0, '', 1, '授权操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (22, 'sysOperationLog.businessType', '导出', '5', 6, 0, 'warning', 1, '导出操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (23, 'sysOperationLog.businessType', '导入', '6', 7, 0, 'warning', 1, '导入操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (24, 'sysOperationLog.businessType', '强退', '7', 8, 0, 'danger', 1, '强退操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (25, 'sysOperationLog.businessType', '清空', '8', 9, 0, 'danger', 1, '清空操作', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (26, 'sysOperationLog.status', '成功', '1', 1, 0, '', 1, '操作成功', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (27, 'sysOperationLog.status', '失败', '0', 2, 0, 'danger', 1, '操作失败', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (28, 'sysOperationLog.operatorType', '其他', '1', 1, 0, '', 1, '其他操作者', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (29, 'sysOperationLog.operatorType', 'Web用户', '2', 2, 0, '', 1, 'Web用户', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (30, 'sysOperationLog.operatorType', '手机端用户', '3', 3, 0, '', 1, '手机端用户', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (31, 'sysLoginLog.status', '登录成功', '1', 1, 0, 'success', 1, '登录成功', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (32, 'sysLoginLog.status', '退出成功', '2', 2, 0, 'info', 1, '退出成功', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (33, 'sysLoginLog.status', '注册', '3', 3, 0, '', 1, '注册', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (34, 'sysLoginLog.status', '登录失败', '0', 4, 0, 'danger', 1, '登录失败', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (35, 'sysJob.status', '正常', '1', 1, 0, '', 1, '任务正常', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       (36, 'sysJob.status', '暂停', '0', 2, 0, 'danger', 1, '任务暂停', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);


-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES ('1', '0', '0', 'Keystone科技', '0', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null,  '2022-05-21 08:30:54', null,  null, '0');
INSERT INTO `sys_dept` VALUES ('2', '1', '0,1', '深圳总公司', '1', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('3', '1', '0,1', '长沙分公司', '2', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('4', '2', '0,1,2', '研发部门', '1', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('5', '2', '0,1,2', '市场部门', '2', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('6', '2', '0,1,2', '测试部门', '3', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('7', '2', '0,1,2', '财务部门', '4', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('8', '2', '0,1,2', '运维部门', '5', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('9', '3', '0,1,3', '市场部门', '1', null, 'valarchie', '15888888888', 'valarchie@163.com', '1', null, '2022-05-21 08:30:54', null, null, '0');
INSERT INTO `sys_dept` VALUES ('10', '3', '0,1,3', '财务部门', '2', null, 'valarchie', '15888888888', 'valarchie@163.com', '0', null,  '2022-05-21 08:30:54', null,  null, '0');



-- ----------------------------
-- Records of sys_login_info
-- ----------------------------

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO sys_menu VALUES (1, '系统管理', 2, '', 0, '/system', 0, '', '{"title":"系统管理","icon":"ep:management","showParent":1,"rank":1}', 1, '系统管理目录', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:08:50', 0);
INSERT INTO sys_menu VALUES (2, '系统监控', 2, '', 0, '/monitor', 0, '', '{"title":"系统监控","icon":"ep:monitor","showParent":1,"rank":3}', 1, '系统监控目录', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:09:15', 0);
INSERT INTO sys_menu VALUES (3, '系统工具', 2, '', 0, '/tool', 0, '', '{"title":"系统工具","icon":"ep:tools","showParent":1,"rank":2}', 1, '系统工具目录', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:09:03', 0);
INSERT INTO sys_menu VALUES (4, 'Keystone官网', 3, 'KeystoneguanwangIframeRouter', 0, '/KeystoneguanwangIframeLink', 0, '', '{"title":"Keystone官网","icon":"ep:link","showParent":1,"frameSrc":"https://element-plus.org/zh-CN/","rank":8}', 1, 'Agileboot官网地址', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:09:40', 0);
INSERT INTO sys_menu VALUES (5, '用户管理', 1, 'SystemUser', 1, '/system/user/index', 0, 'system:user:list', '{"title":"用户管理","icon":"ep:user-filled","showParent":1}', 1, '用户管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:16:13', 0);
INSERT INTO sys_menu VALUES (6, '角色管理', 1, 'SystemRole', 1, '/system/role/index', 0, 'system:role:list', '{"title":"角色管理","icon":"ep:user","showParent":1}', 1, '角色管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:16:23', 0);
INSERT INTO sys_menu VALUES (7, '菜单管理', 1, 'MenuManagement', 1, '/system/menu/index', 0, 'system:menu:list', '{"title":"菜单管理","icon":"ep:menu","showParent":1}', 1, '菜单管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:15:41', 0);
INSERT INTO sys_menu VALUES (8, '部门管理', 1, 'Department', 1, '/system/dept/index', 0, 'system:dept:list', '{"title":"部门管理","icon":"fa-solid:code-branch","showParent":1}', 1, '部门管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:15:35', 0);
INSERT INTO sys_menu VALUES (9, '岗位管理', 1, 'Post', 1, '/system/post/index', 0, 'system:post:list', '{"title":"岗位管理","icon":"ep:postcard","showParent":1}', 1, '岗位管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:15:11', 0);
INSERT INTO sys_menu VALUES (10, '参数设置', 1, 'Config', 1, '/system/config/index', 0, 'system:config:list', '{"title":"参数设置","icon":"ep:setting","showParent":1}', 1, '参数设置菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:15:03', 0);
INSERT INTO sys_menu VALUES (11, '通知公告', 1, 'SystemNotice', 1, '/system/notice/index', 0, 'system:notice:list', '{"title":"通知公告","icon":"ep:notification","showParent":1}', 1, '通知公告菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:14:56', 0);
INSERT INTO sys_menu VALUES (12, '日志管理', 1, 'LogManagement', 1, '/system/logd', 0, '', '{"title":"日志管理","icon":"ep:document","showParent":1}', 1, '日志管理菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:14:47', 0);
INSERT INTO sys_menu VALUES (13, '在线用户', 1, 'OnlineUser', 2, '/system/monitor/onlineUser/index', 0, 'monitor:online:list', '{"title":"在线用户","icon":"fa-solid:users","showParent":1}', 1, '在线用户菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:13:13', 0);
INSERT INTO sys_menu VALUES (14, '数据监控', 1, 'DataMonitor', 2, '/system/monitor/druid/index', 0, 'monitor:druid:list', '{"title":"数据监控","icon":"fa:database","showParent":1,"frameSrc":"/druid/login.html","isFrameSrcInternal":1}', 1, '数据监控菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:13:25', 0);
INSERT INTO sys_menu VALUES (15, '服务监控', 1, 'ServerInfo', 2, '/system/monitor/server/index', 0, 'monitor:server:list', '{"title":"服务监控","icon":"fa:server","showParent":1}', 1, '服务监控菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:13:34', 0);
INSERT INTO sys_menu VALUES (16, '缓存监控', 1, 'CacheInfo', 2, '/system/monitor/cache/index', 0, 'monitor:cache:list', '{"title":"缓存监控","icon":"ep:reading","showParent":1}', 1, '缓存监控菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:12:59', 0);
INSERT INTO sys_menu VALUES (17, '系统接口', 1, 'SystemAPI', 3, '/tool/swagger/index', 0, 'tool:swagger:list', '{"title":"系统接口","icon":"ep:document-remove","showParent":1,"frameSrc":"/swagger-ui/index.html","isFrameSrcInternal":1}', 1, '系统接口菜单', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:14:01', 0);
INSERT INTO sys_menu VALUES (18, '操作日志', 1, 'OperationLog', 12, '/system/log/operationLog/index', 0, 'monitor:operlog:list', '{"title":"操作日志"}', 1, '操作日志菜单', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (19, '登录日志', 1, 'LoginLog', 12, '/system/log/loginLog/index', 0, 'monitor:logininfor:list', '{"title":"登录日志"}', 1, '登录日志菜单', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (20, '用户查询', 0, ' ', 5, '', 1, 'system:user:query', '{"title":"用户查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (21, '用户新增', 0, ' ', 5, '', 1, 'system:user:add', '{"title":"用户新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (22, '用户修改', 0, ' ', 5, '', 1, 'system:user:edit', '{"title":"用户修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (23, '用户删除', 0, ' ', 5, '', 1, 'system:user:remove', '{"title":"用户删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (24, '用户导出', 0, ' ', 5, '', 1, 'system:user:export', '{"title":"用户导出"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (25, '用户导入', 0, ' ', 5, '', 1, 'system:user:import', '{"title":"用户导入"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (26, '重置密码', 0, ' ', 5, '', 1, 'system:user:resetPwd', '{"title":"重置密码"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (27, '角色查询', 0, ' ', 6, '', 1, 'system:role:query', '{"title":"角色查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (28, '角色新增', 0, ' ', 6, '', 1, 'system:role:add', '{"title":"角色新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (29, '角色修改', 0, ' ', 6, '', 1, 'system:role:edit', '{"title":"角色修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (30, '角色删除', 0, ' ', 6, '', 1, 'system:role:remove', '{"title":"角色删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (31, '角色导出', 0, ' ', 6, '', 1, 'system:role:export', '{"title":"角色导出"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (32, '菜单查询', 0, ' ', 7, '', 1, 'system:menu:query', '{"title":"菜单查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (33, '菜单新增', 0, ' ', 7, '', 1, 'system:menu:add', '{"title":"菜单新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (34, '菜单修改', 0, ' ', 7, '', 1, 'system:menu:edit', '{"title":"菜单修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (35, '菜单删除', 0, ' ', 7, '', 1, 'system:menu:remove', '{"title":"菜单删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (36, '部门查询', 0, ' ', 8, '', 1, 'system:dept:query', '{"title":"部门查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (37, '部门新增', 0, ' ', 8, '', 1, 'system:dept:add', '{"title":"部门新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (38, '部门修改', 0, ' ', 8, '', 1, 'system:dept:edit', '{"title":"部门修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (39, '部门删除', 0, ' ', 8, '', 1, 'system:dept:remove', '{"title":"部门删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (40, '岗位查询', 0, ' ', 9, '', 1, 'system:post:query', '{"title":"岗位查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (41, '岗位新增', 0, ' ', 9, '', 1, 'system:post:add', '{"title":"岗位新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (42, '岗位修改', 0, ' ', 9, '', 1, 'system:post:edit', '{"title":"岗位修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (43, '岗位删除', 0, ' ', 9, '', 1, 'system:post:remove', '{"title":"岗位删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (44, '岗位导出', 0, ' ', 9, '', 1, 'system:post:export', '{"title":"岗位导出"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (45, '参数查询', 0, ' ', 10, '', 1, 'system:config:query', '{"title":"参数查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO sys_menu VALUES (46, '参数新增', 0, ' ', 10, '', 1, 'system:config:add', '{"title":"参数新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (47, '参数修改', 0, ' ', 10, '', 1, 'system:config:edit', '{"title":"参数修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (48, '参数删除', 0, ' ', 10, '', 1, 'system:config:remove', '{"title":"参数删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (49, '参数导出', 0, ' ', 10, '', 1, 'system:config:export', '{"title":"参数导出"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (50, '公告查询', 0, ' ', 11, '', 1, 'system:notice:query', '{"title":"公告查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (51, '公告新增', 0, ' ', 11, '', 1, 'system:notice:add', '{"title":"公告新增"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (52, '公告修改', 0, ' ', 11, '', 1, 'system:notice:edit', '{"title":"公告修改"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (53, '公告删除', 0, ' ', 11, '', 1, 'system:notice:remove', '{"title":"公告删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (54, '操作查询', 0, ' ', 18, '', 1, 'monitor:operlog:query', '{"title":"操作查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (55, '操作删除', 0, ' ', 18, '', 1, 'monitor:operlog:remove', '{"title":"操作删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (56, '日志导出', 0, ' ', 18, '', 1, 'monitor:operlog:export', '{"title":"日志导出"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (57, '登录查询', 0, ' ', 19, '', 1, 'monitor:logininfor:query', '{"title":"登录查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (58, '登录删除', 0, ' ', 19, '', 1, 'monitor:logininfor:remove', '{"title":"登录删除"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (59, '日志导出', 0, ' ', 19, '', 1, 'monitor:logininfor:export', '{"title":"日志导出","rank":22}', 1, '', 0, '2022-05-21 08:30:54', 1, '2023-07-22 17:02:28', 0);
INSERT INTO  sys_menu  VALUES (60, '在线查询', 0, ' ', 13, '', 1, 'monitor:online:query', '{"title":"在线查询"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (61, '批量强退', 0, ' ', 13, '', 1, 'monitor:online:batchLogout', '{"title":"批量强退"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (62, '单条强退', 0, ' ', 13, '', 1, 'monitor:online:forceLogout', '{"title":"单条强退"}', 1, '', 0, '2022-05-21 08:30:54', null, null, 0);
INSERT INTO  sys_menu  VALUES (63, 'Keystone Github地址', 4, 'https://github.com/bruceblink/Keystone', 0, '/external', 0, '', '{"title":"Keystone Github地址","icon":"fa-solid:external-link-alt","showParent":1,"rank":9}', 1, 'Keystone github地址', 0, '2022-05-21 08:30:54', 1, '2023-08-14 23:12:13', 0);
INSERT INTO  sys_menu  VALUES (64, '首页', 2, '', 0, '/global', 0, '121212', '{"title":"首页","showParent":1,"rank":3}', 1, '', 1, '2023-07-24 22:36:03', 1, '2023-07-24 22:38:37', 1);
INSERT INTO  sys_menu  VALUES (65, '个人中心', 1, 'PersonalCenter', 2053, '/system/user/profile', 0, '434sdf', '{"title":"个人中心","showParent":1,"rank":3}', 1, '', 1, '2023-07-24 22:36:55', null, null, 1);


-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES ('1', '温馨提醒：2018-07-01 Keystone新版本发布啊', '2', '新版本内容~~~~~~~~~~', '1', '1',  '2022-05-21 08:30:55', '1', '2022-08-29 20:12:37', '管理员', '0');
INSERT INTO `sys_notice` VALUES ('2', '维护通知：2018-07-01 Keystone系统凌晨维护', '1', '维护内容', '1', '1',  '2022-05-21 08:30:55', null,  null, '管理员', '0');


-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------



-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES ('1', 'ceo', '董事长', '1', '1', '', null,  '2022-05-21 08:30:54', null,  null, '0');
INSERT INTO `sys_post` VALUES ('2', 'se', '项目经理', '2', '1', '', null,  '2022-05-21 08:30:54', null,  null, '0');
INSERT INTO `sys_post` VALUES ('3', 'hr', '人力资源', '3', '1', '', null,  '2022-05-21 08:30:54', null,  null, '0');
INSERT INTO `sys_post` VALUES ('4', 'user', '普通员工', '5', '0', '', null,  '2022-05-21 08:30:54', null,  null, '0');


-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('1', '超级管理员', 'admin', '1', '1', '', '1', null,  '2022-05-21 08:30:54', null,  null, '超级管理员', '0');
INSERT INTO `sys_role` VALUES ('2', '普通角色', 'common', '3', '2', '', '1', null,  '2022-05-21 08:30:54', null,  null, '普通角色', '0');
INSERT INTO `sys_role` VALUES ('3', '闲置角色', 'unused', '4', '2', '', '0', null,  '2022-05-21 08:30:54', null,  null, '未使用的角色', '0');



-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES ('2', '1');
INSERT INTO `sys_role_menu` VALUES ('2', '2');
INSERT INTO `sys_role_menu` VALUES ('2', '3');
INSERT INTO `sys_role_menu` VALUES ('2', '4');
INSERT INTO `sys_role_menu` VALUES ('2', '5');
INSERT INTO `sys_role_menu` VALUES ('2', '6');
INSERT INTO `sys_role_menu` VALUES ('2', '7');
INSERT INTO `sys_role_menu` VALUES ('2', '8');
INSERT INTO `sys_role_menu` VALUES ('2', '9');
INSERT INTO `sys_role_menu` VALUES ('2', '10');
INSERT INTO `sys_role_menu` VALUES ('2', '11');
INSERT INTO `sys_role_menu` VALUES ('2', '12');
INSERT INTO `sys_role_menu` VALUES ('2', '13');
INSERT INTO `sys_role_menu` VALUES ('2', '14');
INSERT INTO `sys_role_menu` VALUES ('2', '15');
INSERT INTO `sys_role_menu` VALUES ('2', '16');
INSERT INTO `sys_role_menu` VALUES ('2', '17');
INSERT INTO `sys_role_menu` VALUES ('2', '18');
INSERT INTO `sys_role_menu` VALUES ('2', '19');
INSERT INTO `sys_role_menu` VALUES ('2', '20');
INSERT INTO `sys_role_menu` VALUES ('2', '21');
INSERT INTO `sys_role_menu` VALUES ('2', '22');
INSERT INTO `sys_role_menu` VALUES ('2', '23');
INSERT INTO `sys_role_menu` VALUES ('2', '24');
INSERT INTO `sys_role_menu` VALUES ('2', '25');
INSERT INTO `sys_role_menu` VALUES ('2', '26');
INSERT INTO `sys_role_menu` VALUES ('2', '27');
INSERT INTO `sys_role_menu` VALUES ('2', '28');
INSERT INTO `sys_role_menu` VALUES ('2', '29');
INSERT INTO `sys_role_menu` VALUES ('2', '30');
INSERT INTO `sys_role_menu` VALUES ('2', '31');
INSERT INTO `sys_role_menu` VALUES ('2', '32');
INSERT INTO `sys_role_menu` VALUES ('2', '33');
INSERT INTO `sys_role_menu` VALUES ('2', '34');
INSERT INTO `sys_role_menu` VALUES ('2', '35');
INSERT INTO `sys_role_menu` VALUES ('2', '36');
INSERT INTO `sys_role_menu` VALUES ('2', '37');
INSERT INTO `sys_role_menu` VALUES ('2', '38');
INSERT INTO `sys_role_menu` VALUES ('2', '39');
INSERT INTO `sys_role_menu` VALUES ('2', '40');
INSERT INTO `sys_role_menu` VALUES ('2', '41');
INSERT INTO `sys_role_menu` VALUES ('2', '42');
INSERT INTO `sys_role_menu` VALUES ('2', '43');
INSERT INTO `sys_role_menu` VALUES ('2', '44');
INSERT INTO `sys_role_menu` VALUES ('2', '45');
INSERT INTO `sys_role_menu` VALUES ('2', '46');
INSERT INTO `sys_role_menu` VALUES ('2', '47');
INSERT INTO `sys_role_menu` VALUES ('2', '48');
INSERT INTO `sys_role_menu` VALUES ('2', '49');
INSERT INTO `sys_role_menu` VALUES ('2', '50');
INSERT INTO `sys_role_menu` VALUES ('2', '51');
INSERT INTO `sys_role_menu` VALUES ('2', '52');
INSERT INTO `sys_role_menu` VALUES ('2', '53');
INSERT INTO `sys_role_menu` VALUES ('2', '54');
INSERT INTO `sys_role_menu` VALUES ('2', '55');
INSERT INTO `sys_role_menu` VALUES ('2', '56');
INSERT INTO `sys_role_menu` VALUES ('2', '57');
INSERT INTO `sys_role_menu` VALUES ('2', '58');
INSERT INTO `sys_role_menu` VALUES ('2', '59');
INSERT INTO `sys_role_menu` VALUES ('2', '60');
INSERT INTO `sys_role_menu` VALUES ('2', '61');
-- roleId = 2的权限 特地少一个 方便测试
INSERT INTO `sys_role_menu` VALUES ('3', '1');




-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('1', '1', '1', '4', 'admin', 'valarchie1', '0', 'keystone@163.com', '15888888889', '0', '', '$2a$10$rb1wRoEIkLbIknREEN1LH.FGs4g0oOS5t6l5LQ793nRaFO.SPHDHy', '1', '127.0.0.1', '2022-10-06 17:00:06', 1, null, null,  '2022-05-21 08:30:54', '1',  '2022-10-06 17:00:06', '管理员', '0');
INSERT INTO `sys_user` VALUES ('2', '2', '2', '5', 'ag1', 'valarchie2', '0', 'keystone1@qq.com', '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '1', '127.0.0.1', '2022-05-21 08:30:54', 0, null, null,  '2022-05-21 08:30:54', null,  null, '测试呔1', '0');
INSERT INTO `sys_user` VALUES ('3', '2', '0', '5', 'ag2', 'valarchie3', '0', 'keystone2@qq.com', '15666666667', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '1', '127.0.0.1', '2022-05-21 08:30:54', 0, null, null,  '2022-05-21 08:30:54', null,  null, '测试呔2', '0');
