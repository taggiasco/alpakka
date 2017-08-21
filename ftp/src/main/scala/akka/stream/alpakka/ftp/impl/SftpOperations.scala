/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp
package impl

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.{OpenMode, RemoteResourceInfo, SFTPClient}
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.password.PasswordUtils
import net.schmizz.sshj.xfer.FilePermission
import scala.collection.immutable
import scala.util.Try
import scala.collection.JavaConverters._
import java.nio.file.attribute.PosixFilePermission
import java.io.{File, IOException, InputStream, OutputStream}

private[ftp] trait SftpOperations { _: FtpLike[SSHClient, SftpSettings] =>

  type Handler = SFTPClient

  def connect(connectionSettings: SftpSettings)(implicit ssh: SSHClient): Try[Handler] = Try {
    import connectionSettings._

    if (!strictHostKeyChecking)
      ssh.addHostKeyVerifier(new PromiscuousVerifier)
    else
      knownHosts.foreach(path => ssh.loadKnownHosts(new File(path)))

    ssh.connect(host.getHostAddress, port)

    if (credentials.password != "" && sftpIdentity.isEmpty)
      ssh.authPassword(credentials.username, credentials.password)

    sftpIdentity.foreach(setIdentity(_, credentials.username))

    ssh.newSFTPClient()
  }

  def disconnect(handler: Handler)(implicit ssh: SSHClient): Unit = {
    handler.close()
    if (ssh.isConnected) ssh.disconnect()
  }

  def listFiles(basePath: String, handler: Handler): immutable.Seq[FtpFile] = {
    val path = if (!basePath.isEmpty && basePath.head != '/') s"/$basePath" else basePath
    val entries = handler.ls(path).asScala
    entries.map { file =>
      FtpFile(
        file.getName,
        file.getPath,
        file.isDirectory,
        file.getAttributes.getSize,
        file.getAttributes.getMtime * 1000L,
        getPosixFilePermissions(file)
      )
    }.toVector
  }

  private def getPosixFilePermissions(file: RemoteResourceInfo) = {
    import FilePermission._, PosixFilePermission._
    file.getAttributes.getPermissions.asScala.collect {
      case USR_R => OWNER_READ
      case USR_W => OWNER_WRITE
      case USR_X => OWNER_EXECUTE
      case GRP_R => GROUP_READ
      case GRP_W => GROUP_WRITE
      case GRP_X => GROUP_EXECUTE
      case OTH_R => OTHERS_READ
      case OTH_W => OTHERS_WRITE
      case OTH_X => OTHERS_EXECUTE
    }.toSet
  }

  def listFiles(handler: Handler): immutable.Seq[FtpFile] = listFiles(".", handler)

  def retrieveFileInputStream(name: String, handler: Handler): Try[InputStream] = Try {
    val remoteFile = handler.open(name, Set(OpenMode.READ).asJava)
    val is = new remoteFile.RemoteFileInputStream()
    Option(is).getOrElse(throw new IOException(s"$name: No such file or directory"))
  }

  def storeFileOutputStream(name: String, handler: Handler, append: Boolean): Try[OutputStream] = Try {
    import OpenMode._
    val openModes = Set(WRITE, CREAT) ++ (if (append) Set(APPEND) else Set())
    val remoteFile = handler.open(name, openModes.asJava)
    val os = new remoteFile.RemoteFileOutputStream()
    Option(os).getOrElse(throw new IOException(s"Could not write to $name"))
  }

  private[this] def setIdentity(identity: SftpIdentity, username: String)(implicit ssh: SSHClient) = {
    def bats(array: Array[Byte]): String = new String(array, "UTF-8")

    def initKey(f: OpenSSHKeyFile => Unit) = {
      val key = new OpenSSHKeyFile
      f(key)
      ssh.authPublickey(username, key)
    }

    val passphrase =
      identity.privateKeyFilePassphrase.map(pass => PasswordUtils.createOneOff(bats(pass).toCharArray)).orNull

    identity match {
      case id: RawKeySftpIdentity =>
        initKey(_.init(bats(id.privateKey), id.publicKey.map(bats).orNull, passphrase))
      case id: KeyFileSftpIdentity =>
        initKey(_.init(new File(id.privateKey), passphrase))
    }
  }
}
