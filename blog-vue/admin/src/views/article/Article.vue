<template>
  <el-card class="main-card">
    <div class="title">{{ this.$route.name }}</div>
    <!-- 文章标题 -->
    <div class="article-title-container">
      <el-input
        v-model="article.articleTitle"
        size="medium"
        placeholder="输入文章标题"
      />
      <el-button
        type="danger"
        size="medium"
        class="save-btn action-btn"
        @click="saveArticleDraft"
        v-if="article.isDraft != 0"
      >
        保存草稿
      </el-button>
      <el-button
        type="danger"
        size="medium"
        class="publish-btn action-btn"
        @click="addOrEdit = true"
      >
        发布文章
      </el-button>
    </div>
    <!-- 文章内容 - 使用ByteMD编辑器 -->
    <div class="editor-config-panel">
      <div class="config-row">
        <div class="config-item">
          <span>Mode:</span>
          <label><input type="radio" v-model="mode" value="auto"> auto</label>
          <label><input type="radio" v-model="mode" value="split"> split</label>
          <label><input type="radio" v-model="mode" value="tab"> tab</label>
        </div>
        
        <div class="config-item">
          <span>Locale:</span>
          <select v-model="currentLocale">
            <option value="ar">العربية (Arabic)</option>
            <option value="ca">Català (Catalan)</option>
            <option value="de">Deutsch (German)</option>
            <option value="en">English</option>
            <option value="es">Español (Spanish)</option>
            <option value="fr">Français (French)</option>
            <option value="id">Bahasa Indonesia (Indonesian)</option>
            <option value="nb_NO">Norsk Bokmål (Norwegian)</option>
            <option value="pl">Polski (Polish)</option>
            <option value="pt_BR">Português Brasileiro (Brazilian Portuguese)</option>
            <option value="ru">Русский (Russian)</option>
            <option value="tr">Türkçe (Turkish)</option>
            <option value="zh_Hans">简体中文 (Simplified Chinese)</option>
          </select>
        </div>
        
        <div class="config-item">
          <span>Max length:</span>
          <input type="number" v-model.number="maxLength" min="0">
        </div>
      </div>
      
      <div class="config-row">
        <div class="config-item">
          <span>Plugins:</span>
          <label><input type="checkbox" v-model="enableBreaks"> breaks</label>
          <label><input type="checkbox" v-model="enableFrontmatter"> frontmatter</label>
          <label><input type="checkbox" v-model="enableGemoji"> gemoji</label>
          <label><input type="checkbox" v-model="enableGfm"> gfm</label>
          <label><input type="checkbox" v-model="enableHighlight"> highlight</label>
          <label><input type="checkbox" v-model="enableMath"> math</label>
          <label><input type="checkbox" v-model="enableMediumZoom"> medium-zoom</label>
          <label><input type="checkbox" v-model="enableMermaid"> mermaid</label>
        </div>
      </div>
    </div>

    <div v-if="isMobile" class="mobile-editor-hint">
      手机编辑模式：使用“编辑 / 预览”标签切换视图，离开页面时会自动保存。
    </div>
    
    <Editor 
      :key="editorKey"
      :value="article.articleContent" 
      :plugins="plugins"
      :mode="mode"
      :locale="locales[currentLocale]"
      :max-length="maxLength"
      :placeholder="placeholder"
      :uploadImages="uploadImages"
      @change="handleContentChange"
    />
    <!-- 添加文章对话框 -->
    <el-dialog
      :visible.sync="addOrEdit"
      :width="isMobile ? '100%' : '40%'"
      :fullscreen="isMobile"
      :top="isMobile ? '0' : '10vh'"
      :custom-class="isMobile ? 'mobile-publish-dialog' : ''"
    >
      <div class="dialog-title-container" slot="title">
        上传文章
      </div>
      <!-- 文章数据 -->
      <el-form
        :label-position="isMobile ? 'top' : 'right'"
        label-width="80px"
        size="medium"
        :model="article"
      >
        <el-form-item label="文章分类">
          <el-select
            v-model="article.categoryId"
            class="publish-field"
            placeholder="请选择分类"
          >
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <!-- 添加摘要输入框 -->
        <el-form-item label="文章摘要">
          <el-input
            type="textarea"
            v-model="article.summary"
            :rows="4"
            placeholder="请输入文章摘要(不超过200字)，留空将自动提取文章内容前200字"
            maxlength="200"
            show-word-limit
          ></el-input>
        </el-form-item>
        
        <el-form-item label="文章标签">
          <el-select
            v-model="article.tagIdList"
            class="publish-field"
            multiple
            placeholder="请选择标签"
          >
            <el-option
              v-for="item in tagList"
              :key="item.id"
              :label="item.tagName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上传封面">
          <el-upload
            class="upload-cover"
            drag
            action="/api/admin/articles/images"
            multiple
            :on-success="uploadCover"
          >
            <i class="el-icon-upload" v-if="article.articleCover == ''" />
            <div class="el-upload__text" v-if="article.articleCover == ''">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <img
              v-else
              :src="article.articleCover"
              class="cover-preview"
              alt="文章封面预览"
            />
          </el-upload>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch
            v-model="article.isTop"
            active-color="#13ce66"
            inactive-color="#F4F4F5"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="addOrEdit = false">取 消</el-button>
        <el-button type="danger" @click="saveOrUpdateArticle">
          发 表
        </el-button>
      </div>
    </el-dialog>
  </el-card>
</template>

<script>
// 导入ByteMD组件
import { Editor, Viewer } from '@bytemd/vue'
import 'bytemd/dist/index.min.css'

// 导入插件
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import mermaid from '@bytemd/plugin-mermaid'  // ByteMD的mermaid插件
import gemoji from '@bytemd/plugin-gemoji'
import math from '@bytemd/plugin-math'
import mediumZoom from '@bytemd/plugin-medium-zoom'
import frontmatter from '@bytemd/plugin-frontmatter'
// import breaks from '@bytemd/plugin-breaks'

// 导入必要的样式
import 'highlight.js/styles/github.css'
import 'katex/dist/katex.css'
import 'github-markdown-css/github-markdown.css'
// 添加 mermaid 样式
// import 'mermaid/dist/mermaid.min.css'

// 导入原生 mermaid 库
import mermaidLib from 'mermaid'

// 导入中文语言包
import zh_Hans from '../../assets/locales/zh_Hans.json';

// 导入所有语言包
import ar from '../../assets/locales/ar.json';
import ca from '../../assets/locales/ca.json';
import de from '../../assets/locales/de.json';
import en from '../../assets/locales/en.json';
import es from '../../assets/locales/es.json';
import fr from '../../assets/locales/fr.json';
import id from '../../assets/locales/id.json';
import nb_NO from '../../assets/locales/nb_NO.json';
import pl from '../../assets/locales/pl.json';
import pt_BR from '../../assets/locales/pt_BR.json';
import ru from '../../assets/locales/ru.json';
import tr from '../../assets/locales/tr.json';

export default {
  components: {
    Editor,
    Viewer
  },
  created() {
    const path = this.$route.path;
    const arr = path.split("/");
    const articleId = arr[2];
    if (articleId) {
      this.axios.get("/api/admin/articles/" + articleId).then(({ data }) => {
        this.article = data.data;
      });
    }
    this.listArticleOptions();
    
    // 设置全局配置，告诉 highlight.js 忽略 mermaid 代码块
    window.hljs = window.hljs || {};
    window.hljs.configure = window.hljs.configure || function(){};
    window.hljs.configure({
      languages: [],
      noHighlightRe: /^mermaid$/
    });
    
    // 保存this引用
    const self = this;
    
    // 创建自定义图片上传插件
    const uploadImagePlugin = {
      actions: [
        {
          title: '上传图片',
          icon: '<svg viewBox="0 0 16 16"><path d="M10 3.5a1.5 1.5 0 1 1 3 0 1.5 1.5 0 0 1-3 0zm-8 8a2.5 2.5 0 0 1 5 0 2.5 2.5 0 0 1-5 0zm10.5-2.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"/></svg>',
          handler: {
            type: 'action',
            click(ctx) {
              // 创建文件输入元素
              const input = document.createElement('input');
              input.type = 'file';
              input.accept = 'image/*';
              input.multiple = true;
              
              // 处理文件选择
              input.onchange = async () => {
                if (!input.files || input.files.length === 0) return;
                
                // 显示加载提示
                self.$message({
                  message: '图片上传中...',
                  type: 'info',
                  duration: 0
                });
                
                try {
                  const files = Array.from(input.files);
                  const urls = await self.uploadImages(files);
                  
                  // 关闭提示
                  self.$message.closeAll();
                  
                  if (urls && urls.length) {
                    // 将图片URL作为markdown插入编辑器
                    const imgMarkdown = urls.map(url => `![图片](${url})`).join('\n');
                    ctx.appendBlock(imgMarkdown);
                    self.$message.success('图片上传成功');
                  }
                } catch (error) {
                  self.$message.closeAll();
                  self.$message.error('图片上传失败: ' + (error.message || '未知错误'));
                }
              };
              
              // 触发文件选择对话框
              input.click();
            }
          }
        }
      ],
      // 处理粘贴和拖放操作 - 使用DOM API而不是CodeMirror API
      editorEffect({ editor }) {
        // 获取编辑器DOM元素
        const editorEl = editor.getWrapperElement();
        
        // 处理粘贴事件
        editorEl.addEventListener('paste', async (e) => {
          const items = Array.from(e.clipboardData.items);
          const images = items.filter(item => item.type.startsWith('image/'));
          
          if (images.length === 0) return;
          
          e.preventDefault();
          
          // 显示加载提示
          self.$message({
            message: '图片上传中...',
            type: 'info',
            duration: 0
          });
          
          try {
            const files = images.map(item => item.getAsFile());
            const urls = await self.uploadImages(files);
            
            // 关闭提示
            self.$message.closeAll();
            
            if (urls && urls.length) {
              const imgMarkdown = urls.map(url => `![图片](${url})`).join('\n');
              // 在光标位置插入文本
              const cursor = editor.getCursor();
              editor.replaceRange(imgMarkdown, cursor);
              self.$message.success('图片上传成功');
            }
          } catch (error) {
            self.$message.closeAll();
            self.$message.error('图片上传失败: ' + (error.message || '未知错误'));
          }
        });
        
        // 处理拖放事件
        editorEl.addEventListener('dragover', (e) => {
          e.preventDefault();
          editorEl.classList.add('bytemd-editor-drag');
        });
        
        editorEl.addEventListener('dragleave', () => {
          editorEl.classList.remove('bytemd-editor-drag');
        });
        
        editorEl.addEventListener('drop', async (e) => {
          e.preventDefault();
          editorEl.classList.remove('bytemd-editor-drag');
          
          const items = Array.from(e.dataTransfer.items);
          const images = items.filter(item => item.type.startsWith('image/'));
          
          if (images.length === 0) return;
          
          // 显示加载提示
          self.$message({
            message: '图片上传中...',
            type: 'info',
            duration: 0
          });
          
          try {
            const files = images.map(item => item.getAsFile());
            const urls = await self.uploadImages(files);
            
            // 关闭提示
            self.$message.closeAll();
            
            if (urls && urls.length) {
              const imgMarkdown = urls.map(url => `![图片](${url})`).join('\n');
              // 在光标位置插入文本
              const cursor = editor.getCursor();
              editor.replaceRange(imgMarkdown, cursor);
              self.$message.success('图片上传成功');
            }
          } catch (error) {
            self.$message.closeAll();
            self.$message.error('图片上传失败: ' + (error.message || '未知错误'));
          }
        });
      }
    };
    
    // 构建插件列表
    this.plugins = [
      uploadImagePlugin, // 添加自定义上传插件
      {
        viewerEffect({ markdownBody }) {
          // 查找所有 mermaid 代码块
          const mermaidBlocks = markdownBody.querySelectorAll('pre code.language-mermaid');
          // 移除这些代码块上的 hljs 相关类
          mermaidBlocks.forEach(block => {
            block.classList.remove('hljs');
            block.parentElement.classList.add('no-highlight');
          });
        }
      },
      highlight({
        ignore: ['mermaid']
      }),
      mermaid({
        theme: 'default'
      }),
      gfm(),
      frontmatter(),
      gemoji(),
      math(),
      mediumZoom()
    ].concat(this.plugins || []);
  },
  destroyed() {
    //文章自动保存功能
    this.autoSaveArticle();
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
    document.addEventListener('visibilitychange', this.handleVisibilityChange);

    // 初始化 mermaid - 使用重命名后的库
    mermaidLib.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      fontFamily: 'sans-serif',
      themeVariables: {
        primaryColor: '#7C84B8',
        primaryTextColor: '#333',
        primaryBorderColor: '#7C84B8',
        lineColor: '#333',
        secondaryColor: '#EDF2F7',
        tertiaryColor: '#f0f5ff'
      },
      flowchart: {
        useMaxWidth: false,
        htmlLabels: true,
        curve: 'basis'
      }
    });
    
    // 使用后处理钩子来处理渲染
    this.setupBytemdHook();

    // 打印语言包内容，确认是否正确
    console.log('中文语言包加载状态:', this.locale ? '成功' : '失败');
  },
  beforeDestroy() {
    // 移除事件监听器
    window.removeEventListener('resize', this.handleResize);
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);
  },
  data: function() {
    return {
      addOrEdit: false,
      autoSave: true,
      categoryList: [],
      tagList: [],
      article: {
        id: null,
        articleTitle: this.$moment(new Date()).format("YYYY-MM-DD"),
        articleContent: "",
        articleCover: "",
        categoryId: null,
        tagIdList: [],
        isTop: 0,
        isDraft: null,
        summary: ""
      },
      
      // ByteMD 编辑器配置
      mode: window.innerWidth <= 768 ? 'tab' : 'auto',
      isMobile: window.innerWidth <= 768,
      maxLength: 100000,
      placeholder: '请输入文章内容...',
      editorKey: 'editor-1',
      
      // 插件配置
      enableBreaks: false,
      enableFrontmatter: true,
      enableGemoji: true,
      enableGfm: true,
      enableHighlight: true,
      enableMath: true,
      enableMediumZoom: true,
      enableMermaid: true,
      plugins: [],
      debug: true, // 设置为true开启调试
      refreshKey: 0, // 用于强制重新渲染编辑器
      currentLocale: 'zh_Hans',
      locales: {
        ar,
        ca,
        de,
        en,
        es,
        fr,
        id,
        nb_NO,
        pl,
        pt_BR,
        ru,
        tr,
        zh_Hans
      },
      
      // 在这里添加编辑器配置
      editorConfig: {
        uploadImages: this.uploadImages,
      },
    };
  },
  computed: {
    // 计算属性，根据当前选择返回正确的语言包
    locale() {
      return this.locales[this.currentLocale];
    }
  },
  methods: {
    handleResize() {
      const isMobile = window.innerWidth <= 768;
      if (isMobile !== this.isMobile) {
        this.isMobile = isMobile;
        this.mode = isMobile ? 'tab' : 'auto';
        this.refreshEditor();
      }
    },
    handleVisibilityChange() {
      if (document.visibilityState === 'hidden') {
        this.autoSaveArticle();
      }
    },
    listArticleOptions() {
      this.axios.get("/api/admin/articles/options").then(({ data }) => {
        this.categoryList = data.data.categoryDTOList;
        this.tagList = data.data.tagDTOList;
      });
    },
    // 新方法：设置 ByteMD 后处理钩子
    setupBytemdHook() {
      // 确保我们可以访问 DOM 
      this.$nextTick(() => {
        // 找到 ByteMD 预览容器
        const observer = new MutationObserver((mutations) => {
          // 监测到 DOM 变化时，尝试渲染 mermaid
          this.processMermaidBlocks();
        });
        
        // 监听预览区域的变化
        const previewContainer = document.querySelector('.bytemd-preview');
        if (previewContainer) {
          observer.observe(previewContainer, { 
            childList: true, 
            subtree: true, 
            attributes: false 
          });
        }
      });
    },
    
    // 新方法：处理所有 Mermaid 代码块
    processMermaidBlocks() {
      setTimeout(() => {
        try {
          const mermaidBlocks = document.querySelectorAll('.bytemd-preview pre code.language-mermaid');
          
          if (mermaidBlocks.length > 0) {
            mermaidBlocks.forEach((block, index) => {
              if (block.parentElement.getAttribute('data-processed') === 'true') {
                return;
              }
              
              const mermaidCode = block.textContent;
              const chartDiv = document.createElement('div');
              chartDiv.className = 'mermaid-chart';
              chartDiv.style.textAlign = 'center';
              chartDiv.style.margin = '16px 0';
              chartDiv.id = `mermaid-chart-${Date.now()}-${index}`;
              
              block.parentElement.parentElement.insertBefore(chartDiv, block.parentElement);
              block.parentElement.style.display = 'none';
              block.parentElement.setAttribute('data-processed', 'true');
              
              // 使用重命名后的mermaid库
              mermaidLib.render(
                chartDiv.id,
                mermaidCode,
                (svgCode) => {
                  chartDiv.innerHTML = svgCode;
                }
              );
            });
          }
        } catch (error) {
          console.error('Mermaid 处理错误:', error);
        }
      }, 500);
    },
    
    // 修改编辑器内容变化处理
    handleContentChange(value) {
      this.article.articleContent = value;
      
      // 内容变化后处理 mermaid 代码块
      this.$nextTick(() => {
        this.processMermaidBlocks();
      });
    },
    
    // 添加上传图片方法
    uploadImages(files) {
      return Promise.all(
        files.map(file => {
          return new Promise((resolve, reject) => {
            // 验证文件类型和大小
            if (!file.type.startsWith('image/')) {
              reject(new Error('只能上传图片文件'));
              return;
            }
            
            // 手机照片通常较大，限制与服务端配置保持在安全范围内
            if (file.size > 15 * 1024 * 1024) {
              reject(new Error('图片大小不能超过15MB'));
              return;
            }
            
            const formData = new FormData();
            formData.append('file', file);
            
            this.axios.post('/api/admin/articles/images', formData)
              .then(({ data }) => {
                if (data.code === 200 || data.flag) {
                  resolve(data.data); // 返回图片URL
                } else {
                  reject(new Error(data.message || '上传失败'));
                }
              })
              .catch(error => {
                reject(error);
              });
          });
        })
      );
    },
    uploadCover(response) {
      this.article.articleCover = response.data;
    },
    generateSummary() {
      if (!this.article.summary) {
        let content = this.article.articleContent;
        if (content) {
          content = content.replace(/#+\s/g, '');
          content = content.replace(/\*\*|__/g, '');
          content = content.replace(/\*|_/g, '');
          content = content.replace(/!\[.*?\]\(.*?\)/g, '');
          content = content.replace(/\[.*?\]\(.*?\)/g, '');
          content = content.replace(/`{1,3}[\s\S]*?`{1,3}/g, '');
          content = content.replace(/>\s(.*?)\n/g, '$1 ');
          content = content.replace(/\n/g, ' ');

          if (content.length > 200) {
            this.article.summary = content.substring(0, 200) + '...';
          } else {
            this.article.summary = content;
          }
        }
      }
    },
    saveArticleDraft() {
      if (this.article.articleTitle.trim() == "") {
        this.$message.error("文章标题不能为空");
        return false;
      }
      if (this.article.articleContent.trim() == "") {
        this.$message.error("文章内容不能为空");
        return false;
      }
      
      this.generateSummary();
      
      this.article.isDraft = 1;
      this.axios.post("/api/admin/articles", this.article).then(({ data }) => {
        if (data.flag) {
          this.$notify.success({
            title: "成功",
            message: "保存草稿成功"
          });
        } else {
          this.$notify.error({
            title: "失败",
            message: "保存草稿失败"
          });
        }
      });
      this.autoSave = false;
    },
    saveOrUpdateArticle() {
      if (this.article.articleTitle.trim() == "") {
        this.$message.error("文章标题不能为空");
        return false;
      }
      if (this.article.articleContent.trim() == "") {
        this.$message.error("文章内容不能为空");
        return false;
      }
      if (!this.article.categoryId) {
        this.$message.error("文章分类不能为空");
        return false;
      }
      if (this.article.tagIdList.length == 0) {
        this.$message.error("文章标签不能为空");
        return false;
      }
      if (this.article.articleCover.trim() == "") {
        this.$message.error("文章封面不能为空");
        return false;
      }
      
      this.generateSummary();
      
      this.article.isDraft = 0;
      this.axios.post("/api/admin/articles", this.article).then(({ data }) => {
        if (data.flag) {
          this.$notify.success({
            title: "成功",
            message: data.message
          });
        } else {
          this.$notify.error({
            title: "失败",
            message: data.message
          });
        }
        this.addOrEdit = false;
      });
      this.autoSave = false;
    },
    autoSaveArticle() {
      if (
        this.autoSave &&
        this.article.articleTitle.trim() != "" &&
        this.article.articleContent.trim() != ""
      ) {
        this.generateSummary();
        
        this.article.isDraft =
          this.article.isDraft == 0 ? this.article.isDraft : 1;
        this.axios
          .post("/api/admin/articles", this.article)
          .then(({ data }) => {
            if (data.flag) {
              this.$notify.success({
                title: "成功",
                message: "自动保存成功"
              });
            } else {
              this.$notify.error({
                title: "失败",
                message: "自动保存失败"
              });
            }
          });
      }
    },
    getEnabledPlugins() {
      const plugins = [];
      
      if (this.enableGfm) plugins.push(gfm());
      if (this.enableHighlight) plugins.push(highlight());
      if (this.enableMermaid) plugins.push(mermaid());
      if (this.enableFrontmatter) plugins.push(frontmatter());
      if (this.enableGemoji) plugins.push(gemoji());
      if (this.enableMath) plugins.push(math());
      if (this.enableMediumZoom) plugins.push(mediumZoom());
      if (this.enableBreaks) plugins.push(breaks());
      
      return plugins;
    },
    // 切换语言方法
    changeLocale(locale) {
      this.currentLocale = locale;
    },
    // 强制刷新编辑器组件
    forceRefresh() {
      this.refreshKey += 1;
    },
    refreshEditor() {
      this.editorKey = 'editor-' + Date.now();
    }
  },
  // 监听语言变化，强制刷新编辑器
  watch: {
    currentLocale() {
      this.forceRefresh();
      this.refreshEditor();
    }
  }
};
</script>

<style>
/* 基础样式调整 */
.article-title-container {
  display: flex;
  align-items: center;
  margin-bottom: 1.25rem;
  margin-top: 2.25rem;
}

/* 保存按钮样式 */
.save-btn {
  margin-left: 0.75rem;
  background: #fff;
  color: #f56c6c;
}

.publish-btn {
  margin-left: 10px;
}

.mobile-editor-hint {
  display: none;
}

.publish-field {
  width: 100%;
}

.cover-preview {
  display: block;
  width: 100%;
  max-width: 360px;
  aspect-ratio: 2 / 1;
  object-fit: cover;
}

/* ByteMD 编辑器样式优化 */
.bytemd {
  height: 600px !important;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.bytemd-toolbar {
  border-bottom: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}

.bytemd-status {
  border-top: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}

/* 拆分视图样式 */
.bytemd-split {
  border-right: 1px solid #e4e7ed;
}

.bytemd-preview {
  background-color: #ffffff !important;
  color: #333333 !important;
}

/* 工具栏按钮悬停效果 */
.bytemd-toolbar-icon:hover {
  background-color: rgba(73, 177, 245, 0.1);
}

/* 配置面板样式 */
.bytemd-editor-config {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.bytemd-editor-config-item {
  display: flex;
  align-items: center;
}

.bytemd-editor-config-item label {
  margin-right: 5px;
}

/* 添加 Mermaid 相关样式 */
.markdown-body .mermaid {
  text-align: center;
  margin: 16px 0;
  overflow: visible;
}

/* 确保预览区域能正确显示 Mermaid 图表 */
.bytemd-preview .mermaid svg {
  max-width: 100%;
}

/* 调整编辑器预览区域的背景色，使图表更清晰 */
.bytemd-preview {
  background-color: white;
}

/* 修改 Mermaid 样式以确保正确渲染在预览区域 */
.bytemd-preview .mermaid {
  text-align: center;
  margin: 16px 0;
  display: block;
  overflow: visible;
  font-size: 16px;
  background-color: white;
}

/* 确保 SVG 正确显示 */
.bytemd-preview .mermaid svg {
  max-width: 100%;
  height: auto !important;
  display: inline-block;
}

/* 修复 mermaid 图表在编辑器中的定位 */
.bytemd-split .bytemd-preview .mermaid {
  position: relative;
  width: 100%;
}

/* 确保图表有足够的空间 */
.bytemd-preview-container {
  overflow: auto;
  padding: 10px 20px;
}

/* 防止预览内容溢出 */
.bytemd-preview {
  overflow: visible;
  background-color: white;
  padding: 0 10px;
}

/* 为流程图文本添加样式 */
.mermaid text {
  font-family: 'Arial', sans-serif;
  font-size: 14px;
}

/* 调整节点和连线样式 */
.mermaid .node rect, 
.mermaid .node circle, 
.mermaid .node ellipse, 
.mermaid .node polygon {
  fill: #f0f5ff;
  stroke: #6c87c7;
  stroke-width: 1px;
}

.mermaid .edgePath .path {
  stroke: #333;
  stroke-width: 1.5px;
}

/* 重置之前的 mermaid 样式 */
.markdown-body .mermaid,
.bytemd-preview .mermaid,
.bytemd-split .bytemd-preview .mermaid {
  /* 重置所有 mermaid 相关样式 */
  all: revert;
}

/* 新的 mermaid 图表样式 */
.mermaid-chart {
  background-color: white;
  border-radius: 4px;
  padding: 16px;
  margin: 16px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.mermaid-chart svg {
  max-width: 100%;
  height: auto !important;
}

/* 确保编辑器和预览区域样式正确 */
.bytemd-preview {
  padding: 0 16px;
  overflow: auto !important;
  background-color: white;
}

/* 移除预览区域的溢出隐藏，确保图表可见 */
.bytemd-preview-container {
  overflow: visible !important;
}

/* 编辑器配置面板 */
.editor-config-panel {
  margin-bottom: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f5f7fa;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.config-row:last-child {
  margin-bottom: 0;
}

.config-item {
  display: flex;
  align-items: center;
  margin-right: 20px;
  margin-bottom: 5px;
}

.config-item span {
  font-weight: bold;
  margin-right: 8px;
}

.config-item label {
  margin-right: 8px;
  display: flex;
  align-items: center;
}

.config-item input[type="radio"],
.config-item input[type="checkbox"] {
  margin-right: 3px;
}

.config-item input[type="number"],
.config-item select {
  height: 28px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
}

/* ByteMD 编辑器样式 */
.bytemd {
  height: 600px !important;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.bytemd-toolbar {
  border-bottom: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}

.bytemd-status {
  border-top: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}

/* 确保 mermaid 图表正确显示 */
.bytemd-preview .language-mermaid svg {
  background-color: #ffffff !important;
}

.bytemd-preview .language-mermaid svg {
  display: block;
  margin: 0 auto;
  max-width: 100%;
}

.bytemd-preview pre code.language-mermaid {
  min-height: 100px;
}

/* 为RTL语言添加支持 */
.bytemd[dir="rtl"] {
  direction: rtl;
  text-align: right;
}

.bytemd[dir="rtl"] .bytemd-toolbar {
  direction: rtl;
}

/* 设置白色主题 */
.bytemd {
  height: 600px !important;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

/* 编辑器整体背景 */
.bytemd-preview {
  background-color: #ffffff !important;
  color: #333333 !important;
}

/* 预览区域背景 */
.markdown-body {
  background-color: #ffffff !important;
  color: #333333 !important;
}

/* 代码块背景 */
.markdown-body pre {
  background-color: #f6f8fa !important;
  border: 1px solid #e1e4e8 !important;
}

.markdown-body code {
  background-color: #f6f8fa !important;
  color: #24292e !important;
}

/* 标题颜色 */
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  color: #49B1F5 !important;
  border-bottom-color: #eaecef !important;
}

/* 链接颜色 */
.markdown-body a {
  color: #0366d6 !important;
}

/* 表格样式 */
.markdown-body table {
  border-color: #dfe2e5 !important;
}

.markdown-body table tr {
  background-color: #ffffff !important;
  border-top-color: #dfe2e5 !important;
}

.markdown-body table tr:nth-child(2n) {
  background-color: #f6f8fa !important;
}

.markdown-body table th,
.markdown-body table td {
  border-color: #dfe2e5 !important;
}

/* 引用样式 */
.markdown-body blockquote {
  color: #6a737d !important;
  border-left-color: #dfe2e5 !important;
}

/* Mermaid 图表样式 */
.bytemd-preview .language-mermaid svg {
  background-color: #ffffff !important;
}

/* 语法高亮覆盖 */
.hljs {
  background-color: #f6f8fa !important;
  color: #24292e !important;
}

/* 水平线样式 */
.markdown-body hr {
  background-color: #e1e4e8 !important;
}

/* 编辑器工具栏样式 */
.bytemd-toolbar {
  background-color: #f5f7fa !important;
  border-bottom: 1px solid #e4e7ed !important;
}

/* 编辑器状态栏样式 */
.bytemd-status {
  background-color: #f5f7fa !important;
  border-top: 1px solid #e4e7ed !important;
  color: #606266 !important;
}

/* 滚动条样式 */
.bytemd-preview::-webkit-scrollbar,
.bytemd-editor::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.bytemd-preview::-webkit-scrollbar-thumb,
.bytemd-editor::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.bytemd-preview::-webkit-scrollbar-track,
.bytemd-editor::-webkit-scrollbar-track {
  background: #f5f7fa;
}

/* 拖拽上传相关样式 */
.bytemd-editor textarea {
  transition: background-color 0.3s;
}

.bytemd-editor.drag-over textarea {
  background-color: rgba(232, 243, 255, 0.5);
}

/* 上传消息样式 */
.upload-message {
  background-color: #49B1F5 !important;
  color: white !important;
}

/* 预览中的图片样式 */
.markdown-body img {
  max-width: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  margin: 1em auto;
  display: block;
  transition: transform 0.3s ease;
}

.markdown-body img:hover {
  transform: scale(1.01);
}

/* 拖放区域高亮 */
.bytemd-editor-drag {
  background-color: rgba(73, 177, 245, 0.1);
  border: 2px dashed #49B1F5;
}

/* 上传提示样式 */
.el-message--info {
  background-color: #f0f5ff;
  border-color: #d0e1ff;
}

/* 上传成功提示样式 */
.el-message--success {
  background-color: #f0fff0;
  border-color: #d0ffd0;
}

@media (max-width: 768px) {
  .main-card .el-card__body {
    padding: 12px;
  }

  .main-card .title {
    position: static;
    margin-bottom: 12px;
  }

  .article-title-container {
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 0;
    margin-bottom: 12px;
  }

  .article-title-container > .el-input {
    flex: 0 0 100%;
  }

  .article-title-container .action-btn {
    flex: 1 1 calc(50% - 5px);
    min-height: 44px;
    margin: 0;
  }

  .editor-config-panel {
    display: none;
  }

  .mobile-editor-hint {
    display: block;
    margin-bottom: 10px;
    padding: 9px 11px;
    border-radius: 8px;
    background: #ecf7ff;
    color: #3f6683;
    font-size: 12px;
    line-height: 1.5;
  }

  .bytemd {
    height: calc(100vh - 252px) !important;
    height: calc(100dvh - 252px) !important;
    min-height: 460px;
    border-radius: 8px;
  }

  .bytemd-toolbar {
    overflow-x: auto;
    overflow-y: hidden;
    -webkit-overflow-scrolling: touch;
  }

  .bytemd-toolbar-left {
    flex-wrap: nowrap !important;
    min-width: max-content;
  }

  .bytemd-toolbar-icon {
    min-width: 40px;
    min-height: 40px;
    touch-action: manipulation;
  }

  .CodeMirror,
  .CodeMirror textarea {
    font-size: 16px !important;
  }

  .mobile-publish-dialog .el-dialog__header {
    position: sticky;
    z-index: 2;
    top: 0;
    padding: 16px 16px 12px;
    background: #fff;
    border-bottom: 1px solid #ebeef5;
  }

  .mobile-publish-dialog .el-dialog__body {
    padding: 16px;
  }

  .mobile-publish-dialog .el-dialog__footer {
    position: sticky;
    bottom: 0;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    background: #fff;
    border-top: 1px solid #ebeef5;
  }

  .mobile-publish-dialog .el-form-item__label {
    padding-bottom: 6px;
    line-height: 1.4;
  }

  .mobile-publish-dialog .el-upload,
  .mobile-publish-dialog .el-upload-dragger {
    width: 100%;
  }

  .mobile-publish-dialog .cover-preview {
    max-width: none;
  }

  .mobile-publish-dialog .el-dialog__footer .el-button {
    min-width: 96px;
    min-height: 44px;
  }
}
</style>
