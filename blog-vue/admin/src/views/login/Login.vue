<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-title">管理员登录</div>
      <!-- 登录表单 -->
      <el-form
        status-icon
        :model="loginForm"
        :rules="rules"
        ref="ruleForm"
        class="login-form"
      >
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="el-icon-user-solid"
            placeholder="用户名"
            @keyup.enter.native="login"
          />
        </el-form-item>
        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="iconfont el-icon-mymima"
            show-password
            placeholder="密码"
            @keyup.enter.native="login"
          />
        </el-form-item>
      </el-form>
      <!-- 登录按钮 -->
      <el-button type="primary" @click="login">登录</el-button>
    </div>
  </div>
</template>

<script>
import { generaMenu } from "../../assets/js/menu";
import config from "../../config/config";

export default {
  data: function() {
    return {
      config,
      loginForm: {
        username: "",
        password: ""
      },
      rules: {
        username: [
          { required: true, message: "用户名不能为空", trigger: "blur" }
        ],
        password: [{ required: true, message: "密码不能为空", trigger: "blur" }]
      }
    };
  },
  methods: {
    login() {
      this.$refs.ruleForm.validate(valid => {
        if (valid) {
          if (
            this.config.TENCENT_CAPTCHA &&
            typeof window.TencentCaptcha === "function"
          ) {
            const captcha = new window.TencentCaptcha(
              this.config.TENCENT_CAPTCHA,
              res => {
                if (res.ret === 0) {
                  this.submitLogin();
                }
              }
            );
            captcha.show();
          } else {
            this.submitLogin();
          }
        } else {
          return false;
        }
      });
    },
    submitLogin() {
      const param = new URLSearchParams();
      param.append("username", this.loginForm.username);
      param.append("password", this.loginForm.password);
      this.axios.post("/api/login", param).then(({ data }) => {
        if (data.flag) {
          this.$store.commit("login", data.data);
          generaMenu();
          this.$message.success("登录成功");
          this.$router.push({ path: "/" });
        } else {
          this.$message.error(data.message);
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  left: 0;
  background: url("@/assets/img/6.jpg") center center /
    cover no-repeat;
}
.login-card {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  background: #fff;
  padding: 170px 60px 180px;
  width: 350px;
}
.login-title {
  color: #303133;
  font-weight: bold;
  font-size: 1rem;
}
.login-form {
  margin-top: 1.2rem;
}
.login-card button {
  margin-top: 1rem;
  width: 100%;
}

@media (max-width: 768px) {
  .login-container {
    position: fixed;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    box-sizing: border-box;
  }

  .login-container::before {
    position: absolute;
    inset: 0;
    content: "";
    background: rgba(15, 23, 42, 0.34);
    backdrop-filter: blur(3px);
  }

  .login-card {
    position: relative;
    width: min(100%, 380px);
    padding: 32px 24px;
    border-radius: 16px;
    box-shadow: 0 20px 55px rgba(15, 23, 42, 0.24);
  }

  .login-title {
    font-size: 20px;
    text-align: center;
  }

  .login-card button,
  .login-card .el-input__inner {
    min-height: 46px;
    font-size: 16px;
  }
}
</style>
