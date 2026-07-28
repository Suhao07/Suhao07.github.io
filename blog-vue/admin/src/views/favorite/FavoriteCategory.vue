<template>
  <div>
    <el-card class="main-card">
      <div class="title">分类管理</div>
      <!-- 表格操作 -->
      <div class="operation-container">
        <el-button
          type="primary"
          size="small"
          icon="el-icon-plus"
          @click="openAddCategory"
        >
          新增
        </el-button>
      </div>
      <!-- 分类表格 -->
      <el-table
        v-loading="loading"
        :data="categoryList"
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
          prop="categoryName"
          label="分类名称"
          align="center"
        />
        <el-table-column
          label="分类图标"
          width="120"
          align="center"
        >
          <template slot-scope="scope">
            <i :class="scope.row.categoryIcon"></i>
            <span class="icon-text">{{ scope.row.categoryIcon }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="sort"
          label="排序"
          width="100"
          align="center"
        />
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
          align="center"
        >
          <template slot-scope="scope">
            <i class="el-icon-time"></i>
            <span style="margin-left: 5px">{{
              $moment(scope.row.createTime).format("YYYY-MM-DD HH:mm")
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button
              type="primary"
              size="mini"
              icon="el-icon-edit"
              @click="editCategory(scope.row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除此分类吗？"
              @confirm="deleteCategory(scope.row.id)"
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

    <!-- 分类表单对话框 -->
    <el-dialog
      :title="categoryForm.id ? '编辑分类' : '添加分类'"
      :visible.sync="categoryDialogVisible"
      width="40%"
    >
      <el-form
        ref="categoryForm"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="分类图标" prop="categoryIcon">
          <el-input v-model="categoryForm.categoryIcon" placeholder="请输入图标类名 (如: mdi-code-tags)">
            <template slot="prepend">
              <i :class="categoryForm.categoryIcon || 'mdi-help'"></i>
            </template>
          </el-input>
          <div class="icon-tip">
            推荐使用 Material Design Icons (mdi-*)，
            <a href="https://materialdesignicons.com/" target="_blank">图标库</a>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="999" controls-position="right"></el-input-number>
          <div class="sort-tip">数字越小排序越靠前</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="categoryDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitCategoryForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "FavoriteCategory",
  data() {
    return {
      loading: false,
      categoryList: [],
      categoryDialogVisible: false,
      categoryForm: {
        id: null,
        categoryName: "",
        categoryIcon: "",
        sort: 0
      },
      categoryRules: {
        categoryName: [
          { required: true, message: "请输入分类名称", trigger: "blur" },
          { min: 1, max: 50, message: "长度在 1 到 50 个字符", trigger: "blur" }
        ],
        categoryIcon: [
          { required: false, message: "请输入图标类名", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.listCategories();
  },
  methods: {
    // 获取分类列表
    listCategories() {
      this.loading = true;
      this.axios.get("/api/favorites/categories").then(({ data }) => {
        if (data.flag) {
          this.categoryList = data.data;
        } else {
          this.$message.error(data.message);
        }
        this.loading = false;
      }).catch(() => {
        this.$message.error("获取分类列表失败");
        this.loading = false;
      });
    },
    
    // 打开添加分类对话框
    openAddCategory() {
      this.categoryForm = {
        id: null,
        categoryName: "",
        categoryIcon: "mdi-bookmark",
        sort: 0
      };
      this.categoryDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.categoryForm.clearValidate();
      });
    },
    
    // 编辑分类
    editCategory(row) {
      this.categoryForm = JSON.parse(JSON.stringify(row));
      this.categoryDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.categoryForm.clearValidate();
      });
    },
    
    // 提交分类表单
    submitCategoryForm() {
      this.$refs.categoryForm.validate(valid => {
        if (valid) {
          const isEdit = this.categoryForm.id !== null;
          this.axios
            .post("/api/admin/favorites/categories", this.categoryForm)
            .then(({ data }) => {
              if (data.flag) {
                this.$notify.success({
                  title: "成功",
                  message: isEdit ? "修改成功" : "添加成功"
                });
                this.categoryDialogVisible = false;
                this.listCategories();
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
    
    // 删除分类
    deleteCategory(id) {
      this.axios
        .delete(`/api/admin/favorites/categories/${id}`)
        .then(({ data }) => {
          if (data.flag) {
            this.$notify.success({
              title: "成功",
              message: "删除成功"
            });
            this.listCategories();
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
}
.icon-text {
  margin-left: 5px;
  color: #606266;
  font-size: 13px;
}
.icon-tip, .sort-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
[class^="mdi-"], [class*=" mdi-"] {
  font-size: 16px;
}
</style>
