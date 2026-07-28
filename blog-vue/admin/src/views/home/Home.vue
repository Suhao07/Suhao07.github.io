<template>
  <div>
    <!-- 统计数据 -->
    <el-row :gutter="30">
      <el-col :span="6" v-loading="loading">
        <el-card>
          <div class="card-icon-container">
            <i class="iconfont el-icon-myfangwenliang" style="color:#40C9C6" />
          </div>
          <div class="card-desc">
            <div class="card-title">访问量</div>
            <div class="card-count">{{ viewsCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="card-icon-container">
            <i class="el-icon-s-comment" style="color:#36A3F7" />
          </div>
          <div class="card-desc">
            <div class="card-title">留言量</div>
            <div class="card-count">{{ messageCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="card-icon-container">
            <i class="iconfont el-icon-myuser" style="color:#34BFA3" />
          </div>
          <div class="card-desc">
            <div class="card-title">用户量</div>
            <div class="card-count">{{ userCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="card-icon-container">
            <i class="iconfont el-icon-mywenzhang-copy" style="color:#F4516C" />
          </div>
          <div class="card-desc">
            <div class="card-title">文章量</div>
            <div class="card-count">{{ articleCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 一周访问量展示 -->
    <el-card style="margin-top:1.25rem">
      <div class="e-title">一周访问量</div>
      <div style="height:350px">
        <div ref="viewChartRef" style="width:100%;height:100%"></div>
      </div>
    </el-card>
    <el-row :gutter="30" style="margin-top:1.25rem">
      <!-- 文章浏览量排行 -->
      <el-col :span="16">
        <el-card>
          <div class="e-title">文章浏览量排行</div>
          <div style="height:350px">
            <div ref="rankChartRef" style="width:100%;height:100%"></div>
          </div>
        </el-card>
      </el-col>
      <!-- 分类数据统计 -->
      <el-col :span="8">
        <el-card>
          <div class="e-title">文章分类统计</div>
          <div style="height:350px">
            <div ref="categoryChartRef" style="width:100%;height:100%"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  created() {
    this.initData();
  },
  mounted() {
    // 初始化图表
    this.initCharts();
    this.getData();
    
    // 监听窗口大小变化
    window.addEventListener('resize', this.handleResize);
    
    // 添加定时刷新
    this.timer = setInterval(() => {
      this.refreshData();
    }, 300000);
  },
  data() {
    return {
      loading: true,
      viewsCount: 0,
      messageCount: 0,
      userCount: 0,
      articleCount: 0,
      // 图表实例
      viewChart: null,
      rankChart: null,
      categoryChart: null,
      // 图表数据
      viewData: {
        dates: [],
        values: []
      },
      rankData: {
        titles: [],
        values: []
      },
      categoryData: []
    };
  },
  methods: {
    initData() {
      // 重置基础数据
      this.viewsCount = 0;
      this.messageCount = 0;
      this.userCount = 0;
      this.articleCount = 0;
      
      // 重置图表数据
      this.viewData = { dates: [], values: [] };
      this.rankData = { titles: [], values: [] };
      this.categoryData = [];
    },
    
    initCharts() {
      // 初始化访问量图表
      this.viewChart = echarts.init(this.$refs.viewChartRef);
      this.viewChart.setOption({
        tooltip: {
          trigger: "axis",
          axisPointer: { type: "cross" }
        },
        color: ["#3888fa"],
        legend: {
          data: ["访问量"],
          y: 0
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: [],
          axisLine: {
            lineStyle: { color: "#666" }
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            lineStyle: { color: "#048CCE" }
          }
        },
        series: [{
          name: "访问量",
          type: "line",
          data: [],
          smooth: true,
          areaStyle: { opacity: 0.3 },
          itemStyle: {
            color: '#3888fa',
            lineStyle: {
              color: '#3888fa',
              width: 2
            }
          }
        }]
      });
      
      // 初始化文章排行图表
      this.rankChart = echarts.init(this.$refs.rankChartRef);
      this.rankChart.setOption({
        tooltip: {
          trigger: "axis",
          axisPointer: { type: "shadow" }
        },
        color: ["#58AFFF"],
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: [],
          axisLabel: {
            interval: 0,
            rotate: 40,
            fontSize: 12
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: "浏览量",
          type: "bar",
          data: [],
          barWidth: '60%',
          itemStyle: {
            color: '#58AFFF'
          }
        }]
      });
      
      // 初始化分类统计图表
      this.categoryChart = echarts.init(this.$refs.categoryChartRef);
      this.categoryChart.setOption({
        color: [
          "#7EC0EE",
          "#FF9F7F",
          "#FFD700",
          "#C9C9C9",
          "#E066FF",
          "#C0FF3E"
        ],
        legend: {
          orient: 'vertical',
          right: 10,
          y: 'center',
          data: []
        },
        tooltip: {
          trigger: "item",
          formatter: "{a} <br/>{b} : {c} ({d}%)"
        },
        series: [{
          name: "文章分类",
          type: "pie",
          radius: ['50%', '70%'],
          center: ['40%', '50%'],
          data: [],
          label: {
            show: true,
            formatter: '{b}: {c}篇 ({d}%)'
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      });
    },
    
    getData() {
      this.loading = true;
      this.axios.get("/api/admin")
        .then(({ data }) => {
          if (!data || !data.data) {
            throw new Error("数据格式错误");
          }

          const responseData = data.data;
          console.log("获取到的数据:", responseData);

          // 更新基础统计数据
          this.updateBasicStats(responseData);

          // 更新图表数据并重绘
          this.updateChartData(responseData);
        })
        .catch(error => {
          console.error("获取首页数据失败", error);
          this.$notify.error({
            title: "错误",
            message: "获取数据失败，请稍后重试"
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    updateBasicStats(data) {
      this.viewsCount = data.viewsCount || 0;
      this.messageCount = data.messageCount || 0;
      this.userCount = data.userCount || 0;
      this.articleCount = data.articleCount || 0;
    },

    updateChartData(data) {
      // 重置数据
      this.viewData = { dates: [], values: [] };
      this.rankData = { titles: [], values: [] };
      this.categoryData = [];
      
      let legendData = [];
      
      // 处理访问量数据
      if (Array.isArray(data.uniqueViewDTOList)) {
        // 按日期排序
        const sortedData = data.uniqueViewDTOList.sort((a, b) => 
          new Date(a.day) - new Date(b.day)
        );
        
        sortedData.forEach(item => {
          if (item && item.day && item.viewsCount !== undefined) {
            // 格式化日期显示
            const date = new Date(item.day);
            const formattedDate = `${date.getMonth() + 1}-${date.getDate()}`;
            this.viewData.dates.push(formattedDate);
            this.viewData.values.push(Number(item.viewsCount));
          }
        });
      }
      
      // 处理文章排行数据
      if (Array.isArray(data.articleRankDTOList)) {
        data.articleRankDTOList.forEach(item => {
          if (item && item.articleTitle && item.viewsCount !== undefined) {
            this.rankData.titles.push(item.articleTitle);
            this.rankData.values.push(Number(item.viewsCount));
          }
        });
      }
      
      // 处理分类数据
      if (Array.isArray(data.categoryDTOList)) {
        data.categoryDTOList.forEach(item => {
          if (item && item.categoryName && item.articleCount !== undefined) {
            this.categoryData.push({
              value: Number(item.articleCount),
              name: item.categoryName
            });
            legendData.push(item.categoryName);
          }
        });
      }
      
      // 更新图表
      this.updateCharts(legendData);
    },
    
    updateCharts(legendData) {
      // 更新访问量图表
      if (this.viewChart) {
        this.viewChart.setOption({
          xAxis: { data: this.viewData.dates },
          series: [{ data: this.viewData.values }]
        });
      }
      
      // 更新文章排行图表
      if (this.rankChart) {
        this.rankChart.setOption({
          xAxis: { data: this.rankData.titles },
          series: [{ data: this.rankData.values }]
        });
      }
      
      // 更新分类图表
      if (this.categoryChart) {
        this.categoryChart.setOption({
          legend: { data: legendData },
          series: [{ data: this.categoryData }]
        });
      }
    },
    
    refreshData() {
      this.initData();
      this.getData();
    },

    handleResize() {
      // 响应窗口大小变化，重新调整图表大小
      if (this.viewChart) {
        this.viewChart.resize();
      }
      if (this.rankChart) {
        this.rankChart.resize();
      }
      if (this.categoryChart) {
        this.categoryChart.resize();
      }
    }
  },
  beforeDestroy() {
    // 清除定时器和事件监听
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    window.removeEventListener('resize', this.handleResize);
    
    // 销毁图表实例
    if (this.viewChart) {
      this.viewChart.dispose();
      this.viewChart = null;
    }
    if (this.rankChart) {
      this.rankChart.dispose();
      this.rankChart = null;
    }
    if (this.categoryChart) {
      this.categoryChart.dispose();
      this.categoryChart = null;
    }
  }
};
</script>

<style scoped>
.card-icon-container {
  display: inline-block;
  font-size: 3rem;
}
.card-desc {
  font-weight: bold;
  float: right;
}
.card-title {
  margin-top: 0.3rem;
  line-height: 18px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 1rem;
}
.card-count {
  margin-top: 0.75rem;
  color: #666;
  font-size: 1.25rem;
}
.e-title {
  font-size: 13px;
  color: #202a34;
  font-weight: 700;
  margin-bottom: 20px;
}
</style>
