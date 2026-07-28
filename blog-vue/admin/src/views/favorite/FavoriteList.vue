<template>
  <div>
    <el-card class="main-card">
      <div class="title">收藏网站管理</div>
      <!-- 表格操作 -->
      <div class="operation-container">
        <el-button
          type="primary"
          size="small"
          icon="el-icon-plus"
          @click="openAddFavorite"
        >
          新增
        </el-button>
        <div style="margin-left: auto;">
          <el-select
            v-model="query.categoryId"
            placeholder="请选择分类"
            clearable
            size="small"
            style="width: 200px; margin-right: 10px;"
            @change="getFavoriteList"
          >
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            >
            </el-option>
          </el-select>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-search"
            @click="getFavoriteList"
          >
            查询
          </el-button>
        </div>
      </div>
      
      <!-- 收藏网站表格 -->
      <el-table
        v-loading="loading"
        :data="favoriteList"
        border
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
          align="center"
        />
        <el-table-column
          label="网站图标"
          width="100"
          align="center"
        >
          <template slot-scope="scope">
            <el-image
              v-if="scope.row.icon"
              :src="scope.row.icon"
              style="width: 30px; height: 30px;"
              fit="contain"
            ></el-image>
            <i v-else :class="getCategoryDefaultIcon(scope.row.categoryId)" style="font-size: 20px;"></i>
          </template>
        </el-table-column>
        <el-table-column
          prop="name"
          label="网站名称"
          width="180"
          align="center"
        />
        <el-table-column
          prop="url"
          label="网站地址"
          align="center"
        >
          <template slot-scope="scope">
            <el-link type="primary" :href="scope.row.url" target="_blank">{{ scope.row.url }}</el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="categoryName"
          label="所属分类"
          width="120"
          align="center"
        />
        <el-table-column
          label="标签"
          width="180"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag v-for="tag in scope.row.tags" :key="tag" size="mini" style="margin-right: 5px; margin-bottom: 5px;">
              {{ tag }}
            </el-tag>
            <span v-if="!scope.row.tags || scope.row.tags.length === 0">-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="sort"
          label="排序"
          width="80"
          align="center"
        />
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button
              type="primary"
              size="mini"
              icon="el-icon-edit"
              @click="editFavorite(scope.row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除此收藏网站吗？"
              @confirm="deleteFavorite(scope.row.id)"
            >
              <el-button
                slot="reference"
                type="danger"
                size="mini"
                icon="el-icon-delete"
              >
                删除
              </el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 收藏网站表单对话框 -->
    <el-dialog
      :title="favoriteForm.id ? '编辑收藏网站' : '添加收藏网站'"
      :visible.sync="favoriteDialogVisible"
      width="50%"
    >
      <el-form
        ref="favoriteForm"
        :model="favoriteForm"
        :rules="favoriteRules"
        label-width="100px"
      >
        <el-form-item label="网站名称" prop="name">
          <el-input v-model="favoriteForm.name" placeholder="请输入网站名称"></el-input>
        </el-form-item>
        <el-form-item label="网站地址" prop="url">
          <el-input v-model="favoriteForm.url" placeholder="请输入网站地址 (如: https://example.com)"></el-input>
        </el-form-item>
        <el-form-item label="网站图标" prop="icon">
          <el-input v-model="favoriteForm.icon" placeholder="请输入图标URL">
            <template slot="append">
              <el-popover
                placement="top"
                width="400"
                trigger="hover"
              >
                <div>
                  提示：可以使用网站的favicon图标，通常为 https://网站域名/favicon.ico
                </div>
                <el-button slot="reference" icon="el-icon-question"></el-button>
              </el-popover>
            </template>
          </el-input>
          <div v-if="favoriteForm.icon" class="icon-preview">
            预览：<img :src="favoriteForm.icon" style="max-width: 32px; max-height: 32px; vertical-align: middle;">
          </div>
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="favoriteForm.categoryId" placeholder="请选择分类" style="width: 100%;">
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="网站描述" prop="description">
          <el-input
            v-model="favoriteForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入网站描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-tag
            :key="tag"
            v-for="tag in favoriteForm.tags"
            closable
            :disable-transitions="false"
            @close="handleRemoveTag(tag)"
            style="margin-right: 10px; margin-bottom: 10px;"
          >
            {{ tag }}
          </el-tag>
          <el-input
            class="input-new-tag"
            v-if="inputTagVisible"
            v-model="inputTagValue"
            ref="tagInput"
            size="small"
            @keyup.enter.native="handleInputTagConfirm"
            @blur="handleInputTagConfirm"
          >
          </el-input>
          <el-button v-else class="button-new-tag" size="small" @click="showInputTag">+ 添加标签</el-button>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="favoriteForm.sort" :min="0" :max="999" controls-position="right"></el-input-number>
          <div class="sort-tip">数字越小排序越靠前</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="favoriteDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitFavoriteForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "FavoriteList",
  data() {
    return {
      loading: false,
      categoryList: [],
      favoriteList: [],
      query: {
        categoryId: null
      },
      favoriteDialogVisible: false,
      favoriteForm: {
        id: null,
        name: "",
        url: "",
        description: "",
        icon: "",
        categoryId: null,
        sort: 0,
        tags: []
      },
      favoriteRules: {
        name: [
          { required: true, message: "请输入网站名称", trigger: "blur" },
          { min: 1, max: 100, message: "长度在 1 到 100 个字符", trigger: "blur" }
        ],
        url: [
          { required: true, message: "请输入网站地址", trigger: "blur" },
          { pattern: /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w.-]*)*\/?$/, message: "请输入有效的网站地址", trigger: "blur" }
        ],
        categoryId: [
          { required: true, message: "请选择所属分类", trigger: "change" }
        ],
        description: [
          { max: 500, message: "长度不能超过500个字符", trigger: "blur" }
        ]
      },
      inputTagVisible: false,
      inputTagValue: ""
    };
  },
  created() {
    this.getCategories();
    this.getFavoriteList();
  },
  methods: {
    // 获取分类列表
    getCategories() {
      this.axios.get("/api/favorites/categories").then(({ data }) => {
        if (data.flag) {
          this.categoryList = data.data;
        } else {
          this.$message.error(data.message);
        }
      });
    },
    
    // 获取收藏网站列表
    getFavoriteList() {
      this.loading = true;
      const params = {};
      if (this.query.categoryId) {
        params.categoryId = this.query.categoryId;
      }
      
      this.axios.get("/api/favorites", { params }).then(({ data }) => {
        if (data.flag) {
          this.favoriteList = data.data;
        } else {
          this.$message.error(data.message);
        }
        this.loading = false;
      }).catch(() => {
        this.$message.error("获取收藏网站列表失败");
        this.loading = false;
      });
    },
    
    // 打开添加收藏网站对话框
    openAddFavorite() {
      this.favoriteForm = {
        id: null,
        name: "",
        url: "",
        description: "",
        icon: "",
        categoryId: null,
        sort: 0,
        tags: []
      };
      this.favoriteDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.favoriteForm.clearValidate();
      });
    },
    
    // 编辑收藏网站
    editFavorite(row) {
      this.axios.get(`/api/favorites/${row.id}`).then(({ data }) => {
        if (data.flag) {
          this.favoriteForm = {
            id: data.data.id,
            name: data.data.name,
            url: data.data.url,
            description: data.data.description,
            icon: data.data.icon,
            categoryId: data.data.categoryId,
            sort: data.data.sort || 0,
            tags: data.data.tags || []
          };
          this.favoriteDialogVisible = true;
          this.$nextTick(() => {
            this.$refs.favoriteForm.clearValidate();
          });
        } else {
          this.$message.error(data.message);
        }
      }).catch(() => {
        this.$message.error("获取收藏网站详情失败");
      });
    },
    
    // 提交收藏网站表单
    submitFavoriteForm() {
      this.$refs.favoriteForm.validate(valid => {
        if (valid) {
          const isEdit = this.favoriteForm.id !== null;
          this.axios
            .post("/api/admin/favorites", this.favoriteForm)
            .then(({ data }) => {
              if (data.flag) {
                this.$notify.success({
                  title: "成功",
                  message: isEdit ? "修改成功" : "添加成功"
                });
                this.favoriteDialogVisible = false;
                this.getFavoriteList();
              } else {
                this.$notify.error({
                  title: "失败",
                  message: data.message
                });
              }
            })
            .catch(() => {
              this.$notify.error({
                title: "错误",
                message: "操作失败，请重试"
              });
            });
        } else {
          return false;
        }
      });
    },
    
    // 删除收藏网站
    deleteFavorite(id) {
      this.axios
        .delete(`/api/admin/favorites/${id}`)
        .then(({ data }) => {
          if (data.flag) {
            this.$notify.success({
              title: "成功",
              message: "删除成功"
            });
            this.getFavoriteList();
          } else {
            this.$notify.error({
              title: "失败",
              message: data.message
            });
          }
        })
        .catch(() => {
          this.$notify.error({
            title: "错误",
            message: "删除失败，请重试"
          });
        });
    },
    
    // 根据分类获取默认图标
    getCategoryDefaultIcon(categoryId) {
      const iconMap = {
        1: "mdi-code-tags",
        2: "mdi-palette",
        3: "mdi-school",
        4: "mdi-tools",
        5: "mdi-account-group"
      };
      return iconMap[categoryId] || "mdi-web";
    },
    
    // 标签相关方法
    handleRemoveTag(tag) {
      this.favoriteForm.tags.splice(this.favoriteForm.tags.indexOf(tag), 1);
    },
    
    showInputTag() {
      this.inputTagVisible = true;
      this.$nextTick(() => {
        this.$refs.tagInput.$refs.input.focus();
      });
    },
    
    handleInputTagConfirm() {
      const inputValue = this.inputTagValue.trim();
      if (inputValue && this.favoriteForm.tags.indexOf(inputValue) === -1) {
        this.favoriteForm.tags.push(inputValue);
      }
      this.inputTagVisible = false;
      this.inputTagValue = "";
    }
  }
};
</script>

<style scoped>
.main-card {
  margin: 20px;
}
.title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
}
.operation-container {
  margin-bottom: 20px;
  display: flex;
}
.icon-preview {
  margin-top: 5px;
  font-size: 12px;
  color: #606266;
}
.sort-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.input-new-tag {
  width: 100px;
  margin-right: 10px;
  vertical-align: bottom;
}
.button-new-tag {
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
