const path = require("path");
const TerserPlugin = require('terser-webpack-plugin');

function resolve(dir) {
  return path.join(__dirname, dir);
}

module.exports = {
  devServer: {
    port: 8082,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        pathRewrite: {
          "^/api": ""
        }
      }
    },
    allowedHosts: "all"
  },
  chainWebpack: config => {
    config.resolve.alias.set("@", resolve("src"));
    
    // 禁用 ESLint
    config.module.rules.delete('eslint');
  },
  lintOnSave: false, // 禁用保存时的 lint 检查
  configureWebpack: {
    mode: 'production',
    optimization: {
      minimize: true,
      minimizer: [
        new TerserPlugin({
          terserOptions: {
            compress: {
              drop_console: true,
              drop_debugger: true,
              pure_funcs: ['console.log', 'console.info', 'console.debug', 'console.warn', 'this.log'],
              passes: 2  // 多次优化通道
            },
            mangle: true,
            keep_fnames: false
          },
          extractComments: false,
          parallel: true
        })
      ]
    }
  }
};
