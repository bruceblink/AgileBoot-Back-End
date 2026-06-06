package app.keystone.common.constant;


/**
 * 通用常量信息
 *
 * @author likanug
 */
public class Constants {
    private Constants() {
    }

    public static final int KB = 1024;

    public static final int MB = KB * 1024;

    public static final int GB = MB * 1024;

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";


    public static class Token {

        private Token() {
        }

        /**
         * 令牌前缀
         */
        public static final String PREFIX = "Bearer ";

        /**
         * 令牌前缀
         */
        public static final String LOGIN_USER_KEY = "login_user_key";

        /**
         * 登录用户ID
         */
        public static final String LOGIN_USER_ID = "login_user_id";

        /**
         * 登录用户名
         */
        public static final String LOGIN_USERNAME = "login_username";

        /**
         * refresh 会话ID
         */
        public static final String LOGIN_REFRESH_SESSION_ID = "login_refresh_session_id";

    }

    public static class Captcha {

        private Captcha() {
        }

        /**
         * 令牌
         */
        public static final String MATH_TYPE = "math";

        /**
         * 令牌前缀
         */
        public static final String CHAR_TYPE = "char";

    }

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "profile";

    public static class UploadSubDir {

        private UploadSubDir() {
        }

        public static final String IMPORT_PATH = "import";

        public static final String AVATAR_PATH = "avatar";

        public static final String DOWNLOAD_PATH = "download";

        public static final String UPLOAD_PATH = "upload";

    }



}
