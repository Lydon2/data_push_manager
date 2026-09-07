module.exports = {
  // 自动适配部署路径,无需任何配置
  publicPath: './',
  
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
