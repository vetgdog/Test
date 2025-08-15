from ftplib import FTP_TLS


# 连接 FTP 服务器
ftp = FTP_TLS('192.168.0.200', timeout=5)
ftp.auth()
ftp.login("default", "saisiwuji.323")

# 切换到目标目录
print("当前目录:", ftp.pwd())
ftp.cwd("/视频图文/7. 个人临时文件/3、陆明宇/待审/xfans人物")
print("当前目录:", ftp.pwd())