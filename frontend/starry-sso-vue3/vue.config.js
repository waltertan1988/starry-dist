module.exports = {
    devServer: {
        port: '7080',
        // open: true,
        proxy: {
            // 配置跨域代理，当访问http://[localhost:port]/api/auth时，将会转发到https://[process.env.VUE_APP_API_AUTH_SERVER_PATH]的根路径/
            '/api/auth': {
                target: process.env.VUE_APP_API_AUTH_SERVER_PATH,
                changeOrigin: true,
                pathRewrite: {'^/api/auth': '/'}
            }
        }
    }
}
