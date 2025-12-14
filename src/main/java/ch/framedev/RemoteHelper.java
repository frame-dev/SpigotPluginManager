package ch.framedev;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Simple SSH/SFTP helper using JSch for remote plugin folder operations.
 * Usage: create instance, connect(), call operations, disconnect().
 */
public class RemoteHelper {
    private final JSch jsch = new JSch();
    private Session session;
    private ChannelSftp sftp;

    // Connect using password
    public void connect(String host, int port, String username, String password, int timeoutMs) throws JSchException {
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(timeoutMs);
        openSftp();
    }

    // Connect using private key file (optional passphrase)
    @SuppressWarnings("unused")
    public void connectWithKey(String host, int port, String username, String privateKeyPath, String passphrase, int timeoutMs) throws JSchException {
        if (passphrase == null) jsch.addIdentity(privateKeyPath);
        else jsch.addIdentity(privateKeyPath, passphrase);
        session = jsch.getSession(username, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(timeoutMs);
        openSftp();
    }

    private void openSftp() throws JSchException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftp = (ChannelSftp) channel;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConnected() {
        return session != null && session.isConnected() && sftp != null && sftp.isConnected();
    }

    public void disconnect() {
        if (sftp != null) {
            sftp.disconnect();
            sftp = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    // List files in remote directory
    @SuppressWarnings("unchecked")
    public java.util.List<String> listFiles(String remoteDir) throws SftpException {
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(remoteDir);
        java.util.List<String> names = new ArrayList<>();
        for (ChannelSftp.LsEntry e : entries) {
            if (!".".equals(e.getFilename()) && !"..".equals(e.getFilename())) {
                names.add(e.getFilename());
            }
        }
        return names;
    }

    // Upload local file to remote directory (overwrites)
    public void uploadFile(File localFile, String remoteDir) throws SftpException {
        if (!localFile.exists() || !localFile.isFile()) throw new IllegalArgumentException("Local file invalid");
        String remotePath = remoteDir.endsWith("/") ? remoteDir + localFile.getName() : remoteDir + "/" + localFile.getName();
        sftp.put(localFile.getAbsolutePath(), remotePath, ChannelSftp.OVERWRITE);
    }

    // Download remote file to local destination (overwrites)
    public void downloadFile(String remoteFilePath, File localDest) throws Exception {
        try (InputStream in = sftp.get(remoteFilePath); FileOutputStream out = new FileOutputStream(localDest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
    }

    // Rename remote file (used to enable/disable by renaming suffix)
    public void renameRemote(String remoteDir, String oldName, String newName) throws SftpException {
        String oldPath = remoteDir.endsWith("/") ? remoteDir + oldName : remoteDir + "/" + oldName;
        String newPath = remoteDir.endsWith("/") ? remoteDir + newName : remoteDir + "/" + newName;
        sftp.rename(oldPath, newPath);
    }

    // Delete remote file
    public void deleteRemote(String remoteDir, String name) throws SftpException {
        String path = remoteDir.endsWith("/") ? remoteDir + name : remoteDir + "/" + name;
        sftp.rm(path);
    }

    // Convenience operations
    public java.util.List<String> listPlugins(String remoteDir) throws SftpException {
        return listFiles(remoteDir);
    }

    public void installPlugin(File localJar, String remoteDir) throws SftpException {
        uploadFile(localJar, remoteDir);
    }

    public void enablePlugin(String remoteDir, String disabledName, String disabledSuffix) throws SftpException {
        if (!disabledName.endsWith(disabledSuffix))
            throw new IllegalArgumentException("File does not use disabled suffix");
        String restored = disabledName.substring(0, disabledName.length() - disabledSuffix.length());
        renameRemote(remoteDir, disabledName, restored);
    }

    public void disablePlugin(String remoteDir, String jarName, String disabledSuffix) throws SftpException {
        if (!jarName.endsWith(".jar")) throw new IllegalArgumentException("Expected .jar file");
        renameRemote(remoteDir, jarName, jarName + disabledSuffix);
    }

    public void uninstallPlugin(String remoteDir, String name) throws SftpException {
        deleteRemote(remoteDir, name);
    }
}
