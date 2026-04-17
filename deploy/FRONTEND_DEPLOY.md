# 前端部署指南

## 项目信息

- **项目名称**：gov-web
- **技术栈**：Vue 3.5.30 + Vite 8.0.1
- **UI框架**：Element Plus 2.13.6
- **HTTP客户端**：Axios 1.13.6
- **状态管理**：Pinia 3.0.4
- **路由**：Vue Router 4.6.4
- **开发端口**：3000
- **构建工具**：Vite

## 前端部署方式

前端采用**静态文件 + Nginx反向代理**的方式部署，不需要RPM包。

## 部署流程

### 1. 构建前端

```bash
cd gov-web

# 安装依赖
npm install

# 构建生产版本
npm run build

# 构建输出目录
ls -la dist/
```

**构建输出说明：**
- 输出目录：`dist/`
- 包含优化的分块：
  - `ep-icons.js` - Element Plus图标
  - `ep-data.js` - Element Plus数据组件（表格、分页等）
  - `ep-feedback.js` - Element Plus反馈组件（对话框、消息等）
  - `ep-form.js` - Element Plus表单组件
  - `ep-navigation.js` - Element Plus导航组件
  - `vendor-axios.js` - Axios HTTP客户端
  - `vendor-vue.js` - Vue、Vue Router、Pinia

### 2. 复制到Nginx

```bash
# 方式1：直接复制
sudo cp -r dist/* /usr/share/nginx/html/

# 方式2：备份后复制
sudo mv /usr/share/nginx/html /usr/share/nginx/html.backup
sudo mkdir -p /usr/share/nginx/html
sudo cp -r dist/* /usr/share/nginx/html/
sudo chown -R nginx:nginx /usr/share/nginx/html
```

### 3. 配置Nginx

#### 创建Nginx配置文件

```bash
sudo vi /etc/nginx/conf.d/gov-frontend.conf
```

#### 配置内容

```nginx
# HTTP重定向到HTTPS（可选）
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS服务器配置
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL证书配置（可选）
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # 前端静态文件目录
    root /usr/share/nginx/html;
    index index.html;

    # 前端路由处理（SPA应用）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 缓存静态资源
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 禁用缓存HTML文件
    location ~* \.html$ {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
}
```

#### 简化配置（HTTP only）

如果不需要HTTPS，可以使用简化配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /usr/share/nginx/html;
    index index.html;

    # 前端路由处理
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 缓存静态资源
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 4. 验证Nginx配置

```bash
# 检查配置语法
sudo nginx -t

# 输出应该是：
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 5. 启动Nginx

```bash
# 启动Nginx
sudo systemctl start nginx

# 设置开机自启
sudo systemctl enable nginx

# 查看状态
sudo systemctl status nginx
```

### 6. 验证部署

```bash
# 访问前端
curl http://your-domain.com

# 测试API代理
curl http://your-domain.com/api/actuator/health

# 查看Nginx日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 环境变量配置

### 开发环境

前端开发时使用 `.env` 文件：

```bash
# .env
VITE_APP_NAME=gov-web
VITE_API_BASE_URL=/api
VITE_API_TIMEOUT=10000
VITE_APP_LOG_LEVEL=debug
VITE_APP_SLOW_REQUEST_MS=800
VITE_APP_SLOW_ROUTE_MS=800
VITE_APP_SLOW_INITIAL_ROUTE_MS=1600
VITE_APP_RUNTIME_LOG_BUFFER_SIZE=200
VITE_APP_FRONTEND_MONITOR_ENABLED=true
VITE_APP_FRONTEND_MONITOR_FLUSH_MS=10000
VITE_APP_FRONTEND_MONITOR_BATCH_SIZE=20
VITE_APP_FRONTEND_MONITOR_QUEUE_SIZE=100
VITE_APP_ENABLE_FILE_LOG=true
VITE_APP_LOG_DIR=logs
VITE_APP_LOG_KEEP_DAYS=7
VITE_APP_LOG_MAX_FILE_SIZE_MB=20
VITE_APP_LOG_TOTAL_SIZE_MB=200
VITE_APP_CSRF_COOKIE_NAME=XSRF-TOKEN
VITE_APP_CSRF_HEADER_NAME=X-CSRF-Token
VITE_APP_HIDDEN_MENUS=system:frontend-monitor
```

### 生产环境

生产环境通过Nginx代理，前端无需配置API地址。

**API地址自动为：** `http://your-domain.com/api`

## 开发模式

### 本地开发

```bash
cd gov-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问地址
# http://localhost:3000

# 开发时API代理配置（vite.config.js）
# /api -> http://localhost:8080/api
```

### 代码检查

```bash
# ESLint检查
npm run lint

# 修复ESLint错误
npm run lint -- --fix
```

### 测试

```bash
# 单元测试（单次执行）
npm run test

# 单元测试（监听模式）
npm run test:watch

# 端到端测试
npm run test:e2e

# 烟雾测试（快速验证）
npm run test:smoke
```

### 构建统计

```bash
# 生成构建统计报告
npm run build:stats
```

## 更新部署

### 更新前端代码

```bash
# 1. 构建新版本
cd gov-web
npm run build

# 2. 备份旧版本
sudo mv /usr/share/nginx/html /usr/share/nginx/html.backup-$(date +%Y%m%d-%H%M%S)

# 3. 部署新版本
sudo mkdir -p /usr/share/nginx/html
sudo cp -r dist/* /usr/share/nginx/html/
sudo chown -R nginx:nginx /usr/share/nginx/html

# 4. 重新加载Nginx（不中断服务）
sudo nginx -s reload

# 5. 验证
curl http://your-domain.com
```

### 回滚到上一个版本

```bash
# 查看备份
ls -la /usr/share/nginx/html.backup*

# 恢复备份
sudo rm -rf /usr/share/nginx/html
sudo mv /usr/share/nginx/html.backup-YYYYMMDD-HHMMSS /usr/share/nginx/html

# 重新加载Nginx
sudo nginx -s reload

# 验证
curl http://your-domain.com
```

## Nginx常见操作

### 基本命令

```bash
# 启动
sudo systemctl start nginx

# 停止
sudo systemctl stop nginx

# 重启
sudo systemctl restart nginx

# 重新加载配置（不中断服务）
sudo systemctl reload nginx
# 或
sudo nginx -s reload

# 查看状态
sudo systemctl status nginx

# 查看进程
ps aux | grep nginx
```

### 日志查看

```bash
# 访问日志
sudo tail -f /var/log/nginx/access.log

# 错误日志
sudo tail -f /var/log/nginx/error.log

# 查看特定域名的日志
sudo grep "your-domain.com" /var/log/nginx/access.log
```

### 配置管理

```bash
# 检查配置语法
sudo nginx -t

# 查看当前配置
sudo nginx -T

# 重新加载配置
sudo nginx -s reload

# 优雅关闭
sudo nginx -s quit

# 强制关闭
sudo nginx -s stop
```

## 性能优化

### 启用Gzip压缩

在Nginx配置中添加：

```nginx
gzip on;
gzip_types text/plain text/css text/javascript application/javascript application/json;
gzip_min_length 1000;
gzip_comp_level 6;
```

### 启用缓存

```nginx
# 缓存静态资源30天
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}

# 不缓存HTML
location ~* \.html$ {
    expires -1;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
}
```

### 启用HTTP/2

```nginx
listen 443 ssl http2;
```

## 故障排查

### 前端无法访问

```bash
# 1. 检查Nginx是否运行
sudo systemctl status nginx

# 2. 检查端口是否监听
sudo netstat -tlnp | grep nginx

# 3. 检查防火墙
sudo firewall-cmd --list-all

# 4. 检查Nginx配置
sudo nginx -t

# 5. 查看错误日志
sudo tail -f /var/log/nginx/error.log
```

### API代理不工作

```bash
# 1. 检查后端是否运行
sudo systemctl status gov-backend

# 2. 检查后端端口
sudo netstat -tlnp | grep 8080

# 3. 测试后端连接
curl http://localhost:8080/api/actuator/health

# 4. 查看Nginx错误日志
sudo tail -f /var/log/nginx/error.log

# 5. 查看Nginx访问日志
sudo tail -f /var/log/nginx/access.log
```

### 页面刷新404错误

**原因：** SPA应用路由问题

**解决：** 确保Nginx配置中有以下规则

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

这样所有未匹配的路由都会重定向到 `index.html`，由前端路由处理。

### 静态资源加载失败

```bash
# 1. 检查文件权限
ls -la /usr/share/nginx/html/

# 2. 检查文件是否存在
ls -la /usr/share/nginx/html/dist/

# 3. 修改权限
sudo chown -R nginx:nginx /usr/share/nginx/html
sudo chmod -R 755 /usr/share/nginx/html
```

### 构建失败

```bash
# 1. 清理依赖
rm -rf node_modules package-lock.json

# 2. 重新安装
npm install

# 3. 清理构建缓存
rm -rf dist

# 4. 重新构建
npm run build

# 5. 查看构建日志
npm run build 2>&1 | tee build.log
```

## 安全建议

1. **启用HTTPS** - 使用SSL证书加密通信
2. **配置防火墙** - 只开放必要的端口（80、443）
3. **隐藏Nginx版本** - 在Nginx配置中添加 `server_tokens off;`
4. **启用CORS** - 如果需要跨域请求
5. **CSP头** - 配置内容安全策略
6. **X-Frame-Options** - 防止点击劫持

### 安全Nginx配置示例

```nginx
# 隐藏Nginx版本
server_tokens off;

# 防止点击劫持
add_header X-Frame-Options "SAMEORIGIN" always;

# 防止MIME类型嗅探
add_header X-Content-Type-Options "nosniff" always;

# 启用XSS防护
add_header X-XSS-Protection "1; mode=block" always;

# 内容安全策略
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
```

## 部署检查清单

部署前必须验证：

- [ ] 前端代码已构建（`npm run build`）
- [ ] dist目录已生成
- [ ] Nginx已安装
- [ ] Nginx配置语法正确（`sudo nginx -t`）
- [ ] 后端服务已启动（`sudo systemctl status gov-backend`）
- [ ] 后端API可访问（`curl http://localhost:8080/api/actuator/health`）
- [ ] 防火墙已配置（开放80、443端口）
- [ ] SSL证书已配置（如使用HTTPS）
- [ ] 文件权限正确（`sudo chown -R nginx:nginx /usr/share/nginx/html`）
- [ ] Nginx已启动（`sudo systemctl status nginx`）
- [ ] 前端可访问（`curl http://your-domain.com`）
- [ ] API代理正常（`curl http://your-domain.com/api/actuator/health`）
