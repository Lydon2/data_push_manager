@echo off
chcp 65001 >nul
echo ================================
echo   数据对接平台前端打包工具
echo ================================
echo.

:input
set /p deploy_path="请输入部署目录名(直接回车=部署到ROOT目录): "

if "%deploy_path%"=="" (
    echo.
    echo [信息] 将部署到 Tomcat ROOT 目录
    set "VUE_APP_PUBLIC_PATH=/"
) else (
    echo.
    echo [信息] 将部署到 Tomcat webapps\%deploy_path% 目录
    set "VUE_APP_PUBLIC_PATH=/%deploy_path%/"
)

echo.
echo [开始] 正在打包...
echo.

call npm run build

if %errorlevel% equ 0 (
    echo.
    echo ================================
    echo   打包成功!
    echo ================================
    echo.
    echo 部署步骤:
    if "%deploy_path%"=="" (
        echo 1. 将 dist 目录下的所有文件复制到: tomcat\webapps\ROOT\
    ) else (
        echo 1. 在 Tomcat 的 webapps 目录下创建文件夹: %deploy_path%
        echo 2. 将 dist 目录下的所有文件复制到: tomcat\webapps\%deploy_path%\
    )
    echo 3. 重启 Tomcat 服务
    echo.
    if "%deploy_path%"=="" (
        echo 访问地址: http://localhost:8080/
    ) else (
        echo 访问地址: http://localhost:8080/%deploy_path%/
    )
    echo ================================
) else (
    echo.
    echo [错误] 打包失败,请检查错误信息
)

echo.
pause
