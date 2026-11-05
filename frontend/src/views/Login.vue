<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-logo">🛍️</div>
        <h1>智能电商导购平台</h1>
        <p>Smart E-commerce Shopping Guide Platform</p>
        <div class="brand-slogan">好物甄选 · 智慧导购 · 一站购齐</div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-area">
        <h2>欢迎登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              :prefix-icon="User"
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
              >登 录</el-button
            >
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock } from "@element-plus/icons-vue";
import { loginApi } from "../api";

const router = useRouter();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
});

const rules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await loginApi(form);
      // 校验通过，后端返回 JWT 令牌
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("username", res.data.username);
      ElMessage.success(res.msg || "登录成功");
      router.push("/");
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6a3c 0%, #ff926e 45%, #ffd9c6 100%);
}

.login-card {
  display: flex;
  width: 780px;
  min-height: 420px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(180, 60, 10, 0.35);
}

.login-brand {
  flex: 1;
  background: linear-gradient(160deg, #ff6a3c 0%, #f5541f 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 30px;
}

.brand-logo {
  font-size: 64px;
  margin-bottom: 16px;
}

.login-brand h1 {
  font-size: 26px;
  letter-spacing: 2px;
}

.login-brand p {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.85;
}

.brand-slogan {
  margin-top: 40px;
  font-size: 14px;
  letter-spacing: 4px;
  opacity: 0.9;
}

.login-form-area {
  flex: 1;
  background: #fff;
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-form-area h2 {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 6px;
}

.login-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  text-align: center;
}
</style>
