export default {
  // 留空时本地登录不启用验证码；生产环境通过构建变量配置。
  TENCENT_CAPTCHA: process.env.VUE_APP_TENCENT_CAPTCHA || ""
};
