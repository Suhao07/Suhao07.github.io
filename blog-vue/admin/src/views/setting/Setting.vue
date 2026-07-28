<template>
  <el-card class="main-card">
    <el-tabs v-model="activeName" @tab-click="handleClick">
      <!-- 修改信息 -->
      <el-tab-pane label="修改信息" name="info">
        <div class="info-container">
          <el-upload
            class="avatar-uploader"
            action="/api/users/avatar"
            :show-file-list="false"
            :on-success="updateAvatar"
          >
            <img v-if="avatar" :src="avatar" class="avatar" />
            <i v-else class="el-icon-plus avatar-uploader-icon" />
          </el-upload>
          <el-form
            label-width="70px"
            :model="infoForm"
            style="width:320px;margin-left:3rem"
          >
            <el-form-item label="昵称">
              <el-input v-model="infoForm.nickname" size="small" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="infoForm.intro" size="small" />
            </el-form-item>
            <el-form-item label="个人网站">
              <el-input v-model="infoForm.webSite" size="small" />
            </el-form-item>
            <el-button
              @click="updateInfo"
              type="primary"
              size="medium"
              style="margin-left:4.375rem"
            >
              修改
            </el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <!-- 修改密码 -->
      <el-tab-pane label="修改密码" name="password">
        <el-form label-width="70px" :model="passwordForm" style="width:320px">
          <el-form-item label="旧密码">
            <el-input
              @keyup.enter.native="updatePassword"
              v-model="passwordForm.oldPassword"
              size="small"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input
              @keyup.enter.native="updatePassword"
              v-model="passwordForm.newPassword"
              size="small"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input
              @keyup.enter.native="updatePassword"
              v-model="passwordForm.confirmPassword"
              size="small"
              show-password
            />
          </el-form-item>
          <el-button
            type="primary"
            size="medium"
            style="margin-left:4.4rem"
            @click="updatePassword"
          >
            修改
          </el-button>
        </el-form>
      </el-tab-pane>
      <!-- 网站公告 -->
      <el-tab-pane label="管理公告" name="notice">
        <!-- 公告列表 -->
        <div class="notice-list-header">
          <span class="notice-list-title">已发布公告</span>
          <div class="notice-buttons">
            <el-button 
              type="info" 
              size="small" 
              @click="restoreDraft" 
              v-if="$store.state.noticeDraft"
            >
              <i class="el-icon-document"></i> 恢复草稿
            </el-button>
            <el-button type="primary" size="small" @click="showAddNoticeDialog">
              <i class="el-icon-plus"></i> 发布公告
            </el-button>
          </div>
        </div>
        
        <el-table :data="noticeList" style="width: 100%; margin-bottom: 20px;" border>
          <el-table-column prop="id" label="ID" width="80"></el-table-column>
          <el-table-column prop="content" label="公告内容" show-overflow-tooltip>
            <template slot-scope="scope">
              <div v-html="formatContent(scope.row.content)"></div>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="180"></el-table-column>
          <el-table-column label="操作" width="150">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="editNotice(scope.row)">编辑</el-button>
              <el-button type="text" size="mini" class="delete-btn" @click="deleteNotice(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 添加/编辑公告弹窗 -->
        <el-dialog 
          :title="isEdit ? '编辑公告' : '发布公告'" 
          :visible.sync="noticeDialogVisible"
          width="50%">
          <el-form :model="noticeForm" ref="noticeForm">
            <el-form-item label="公告内容" prop="content" :rules="[{ required: true, message: '请输入公告内容', trigger: 'blur' }]">
              <el-input
                type="textarea"
                v-model="noticeForm.content"
                :rows="6"
                placeholder="支持Markdown格式"
              ></el-input>
            </el-form-item>
            <div class="markdown-preview">
              <div class="preview-title">Markdown 预览：</div>
              <div class="preview-content" v-html="previewContent"></div>
            </div>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="saveDraft" type="info">保存草稿</el-button>
            <el-button @click="noticeDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="submitNotice">发 布</el-button>
          </span>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script>
import { marked } from 'marked';

export default {
  data: function() {
    return {
      infoForm: {
        nickname: this.$store.state.nickname,
        intro: this.$store.state.intro,
        webSite: this.$store.state.webSite
      },
      passwordForm: {
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      },
      noticeList: [],
      noticeForm: {
        id: null,
        content: "",
        createTime: null
      },
      noticeDialogVisible: false,
      isEdit: false,
      activeName: "info",
      draftContent: null,
      autoSaveInterval: null
    };
  },
  computed: {
    previewContent() {
      return this.noticeForm.content ? marked(this.noticeForm.content) : '';
    },
    avatar() {
      return this.$store.state.avatar;
    }
  },
  created() {
    const savedDraft = this.$store.state.noticeDraft;
    if (savedDraft) {
      this.draftContent = savedDraft;
      this.$confirm('检测到上次未保存的公告内容，是否恢复？', '提示', {
        confirmButtonText: '恢复',
        cancelButtonText: '放弃',
        type: 'warning'
      }).then(() => {
        this.noticeForm.content = this.draftContent;
        this.noticeDialogVisible = true;
      }).catch(() => {
        this.$store.commit('clearNoticeDraft');
      });
    }
  },
  watch: {
    noticeDialogVisible(newVal) {
      if (!newVal) {
        if (this.noticeForm.content.trim()) {
          this.$store.commit('saveNoticeDraft', this.noticeForm.content);
        }
      }
    },
    activeName(newVal) {
      if (newVal !== 'notice' && this.noticeForm.content.trim()) {
        this.$store.commit('saveNoticeDraft', this.noticeForm.content);
      }
    }
  },
  methods: {
    formatContent(content) {
      return content.length > 50 ? marked(content.substring(0, 50) + '...') : marked(content);
    },
    handleClick(tab) {
      if (tab.index == 2) {
        this.getNoticeList();
      }
    },
    getNoticeList() {
      this.axios.get("/api/admin/notices").then(({ data }) => {
        if (data.flag) {
          this.noticeList = data.data;
        } else {
          this.$message.error(data.message);
        }
      });
    },
    showAddNoticeDialog() {
      this.isEdit = false;
      this.noticeForm = {
        id: null,
        content: "",
        createTime: null
      };
      this.noticeDialogVisible = true;
    },
    editNotice(notice) {
      this.isEdit = true;
      this.noticeForm = {
        id: notice.id,
        content: notice.content,
        createTime: notice.createTime
      };
      this.noticeDialogVisible = true;
      this.startAutoSave();
    },
    deleteNotice(id) {
      this.$confirm('确定要删除这条公告吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.axios.delete(`/api/admin/notices/${id}`).then(({ data }) => {
          if (data.flag) {
            this.$message.success(data.message);
            this.getNoticeList();
          } else {
            this.$message.error(data.message);
          }
        });
      }).catch(() => {
        this.$message.info('已取消删除');
      });
    },
    submitNotice() {
      this.$refs.noticeForm.validate(valid => {
        if (valid) {
          const method = this.isEdit ? 'put' : 'post';
          const url = this.isEdit ? `/api/admin/notices/${this.noticeForm.id}` : '/api/admin/notices';
          
          this.axios[method](url, this.noticeForm).then(({ data }) => {
            if (data.flag) {
              this.$message.success(data.message);
              this.noticeDialogVisible = false;
              this.getNoticeList();
              this.$store.commit('clearNoticeDraft');
            } else {
              this.$message.error(data.message);
            }
          });
        }
      });
    },
    updateAvatar(response) {
      if (response.flag) {
        this.$message.success(response.message);
        this.$store.commit("updateAvatar", response.data);
      } else {
        this.$message.error(response.message);
      }
    },
    updateInfo() {
      if (this.infoForm.nickname.trim() == "") {
        this.$message.error("昵称不能为空");
        return false;
      }
      this.axios.put("/api/users/info", this.infoForm).then(({ data }) => {
        if (data.flag) {
          this.$message.success(data.message);
          this.$store.commit("updateUserInfo", this.infoForm);
        } else {
          this.$message.error(data.message);
        }
      });
    },
    updatePassword() {
      if (this.passwordForm.oldPassword.trim() == "") {
        this.$message.error("旧密码不能为空");
        return false;
      }
      if (this.passwordForm.newPassword.trim() == "") {
        this.$message.error("新密码不能为空");
        return false;
      }
      if (this.passwordForm.newPassword.length < 6) {
        this.$message.error("新密码不能少于6位");
        return false;
      }
      if (this.passwordForm.newPassword != this.passwordForm.confirmPassword) {
        this.$message.error("两次密码输入不一致");
        return false;
      }
      this.axios
        .put("/api/admin/users/password", this.passwordForm)
        .then(({ data }) => {
          if (data.flag) {
            this.passwordForm.oldPassword = "";
            this.passwordForm.newPassword = "";
            this.passwordForm.confirmPassword = "";
            this.$message.success(data.message);
          } else {
            this.$message.error(data.message);
          }
        });
    },
    startAutoSave() {
      this.autoSaveInterval = setInterval(() => {
        if (this.noticeForm.content.trim()) {
          this.$store.commit('saveNoticeDraft', this.noticeForm.content);
        }
      }, 30000);
    },
    clearAutoSave() {
      if (this.autoSaveInterval) {
        clearInterval(this.autoSaveInterval);
      }
    },
    restoreDraft() {
      const savedDraft = this.$store.state.noticeDraft;
      if (savedDraft) {
        this.noticeForm = {
          id: null,
          content: savedDraft,
          createTime: null
        };
        this.isEdit = false;
        this.noticeDialogVisible = true;
      }
    },
    saveDraft() {
      if (!this.noticeForm.content.trim()) {
        this.$message.warning('草稿内容不能为空');
        return;
      }
      
      this.$store.commit('saveNoticeDraft', this.noticeForm.content);
      this.$message.success('草稿保存成功');
      this.noticeDialogVisible = false;
    }
  },
  beforeDestroy() {
    this.clearAutoSave();
    if (this.noticeForm.content.trim()) {
      this.$store.commit('saveNoticeDraft', this.noticeForm.content);
    }
  }
};
</script>

<style scoped>
.avatar-container {
  text-align: center;
}
.el-icon-message-solid {
  color: #f56c6c;
  margin-right: 0.3rem;
}
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409eff;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}
.avatar {
  width: 120px;
  height: 120px;
  display: block;
}
.info-container {
  display: flex;
  align-items: center;
  margin-left: 20%;
  margin-top: 5rem;
}
.notice-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.notice-list-title {
  font-size: 16px;
  font-weight: 500;
}
.delete-btn {
  color: #f56c6c;
}
.markdown-preview {
  margin-top: 15px;
  border-top: 1px dashed #ccc;
  padding-top: 15px;
}
.preview-title {
  font-weight: 500;
  margin-bottom: 10px;
  color: #606266;
}
.preview-content {
  padding: 10px;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  background-color: #f9f9f9;
  max-height: 200px;
  overflow-y: auto;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.dialog-footer .el-button {
  margin-left: 0;
}
.notice-buttons {
  display: flex;
  gap: 10px;
}
</style>
