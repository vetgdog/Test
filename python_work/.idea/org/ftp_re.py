from ftplib import FTP_TLS,FTP
import ssl
from ssl import SSLSocket

class ReusedSslSocket(SSLSocket):
    def unwrap(self):
        pass
class FTP_TLS_IgnoreHost(FTP_TLS):
    def makepasv(self):
        _, port = super().makepasv()
        return self.host, port
    """Explicit FTPS, with shared TLS session"""
    def ntransfercmd(self, cmd, rest=None):
        conn, size = FTP.ntransfercmd(self, cmd, rest)
        if self._prot_p:
            conn = self.context.wrap_socket(conn,
                                            server_hostname=self.host,
                                            session=self.sock.session)  # reuses TLS session
            conn.__class__ = ReusedSslSocket  # we should not close reused ssl socket when file transfers finish
        return conn, size

ftp = FTP_TLS_IgnoreHost('192.168.0.200', timeout=5)
ftp.ssl_version = ssl.PROTOCOL_TLS
ftp.auth()
ftp.login("default", "saisiwuji.323")
ftp.prot_p()
ftp.set_pasv(True)
pwd = ftp.pwd()
print(pwd)
print(ftp.nlst())


# 切换到 /视频图文/ 目录
ftp.cwd("视频图文")
print("切换后目录:", ftp.pwd())

# 列出 /视频图文/ 下的文件和子目录
print("目录内容:", ftp.nlst())  # 简单列表
# 或使用 dir() 获取详细信息
# ftp.retrlines("LIST", lambda line: print(line))

# 关闭连接
ftp.close()