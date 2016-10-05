package fr.ortolang.diffusion.ftp;

import java.util.Date;

import org.apache.ftpserver.impl.FtpIoSession;

public class FtpSession {

    private Date loginTime;
    private Date lastAccessTime;
    private String username;
    private String clientAdress;
    private long readBytes;
    private double readBytesThroughtput;
    private long writtenBytes;
    private double writtenBytesThroughtput;

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientAdress() {
        return clientAdress;
    }

    public void setClientAdress(String clientAdress) {
        this.clientAdress = clientAdress;
    }

    public long getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    public double getReadBytesThroughtput() {
        return readBytesThroughtput;
    }

    public void setReadBytesThroughtput(double readBytesThroughtput) {
        this.readBytesThroughtput = readBytesThroughtput;
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }

    public void setWrittenBytes(long writtenBytes) {
        this.writtenBytes = writtenBytes;
    }

    public double getWrittenBytesThroughtput() {
        return writtenBytesThroughtput;
    }

    public void setWrittenBytesThroughtput(double writtenBytesThroughtput) {
        this.writtenBytesThroughtput = writtenBytesThroughtput;
    }

    public static FtpSession fromFtpIoSession(FtpIoSession session) {
        FtpSession ftps = new FtpSession();
        ftps.loginTime = session.getLoginTime();
        ftps.lastAccessTime = session.getLastAccessTime();
        ftps.username = session.getUser().getName();
        ftps.clientAdress = session.getRemoteAddress().toString();
        ftps.readBytes = session.getReadBytes();
        ftps.readBytesThroughtput = session.getReadBytesThroughput();
        ftps.writtenBytes = session.getWrittenBytes();
        ftps.writtenBytesThroughtput = session.getWrittenBytesThroughput();
        return ftps;
    }
}
