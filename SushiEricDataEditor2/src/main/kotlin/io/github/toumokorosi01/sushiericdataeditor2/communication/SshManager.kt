package io.github.toumokorosi01.sushiericdataeditor2.communication

import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.editor.session.EditorSession
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.security.Security

/**
 * SSHJライブラリを使用してSSH接続およびSFTP操作を管理するクラス。
 * サーバーへの接続、認証、ファイルの読み書き、および切断処理を担当します。
 */
class SshManager {
    private var client: SSHClient? = null
    private var sftpClient: SFTPClient? = null
    private val logger = LoggerFactory.getLogger(javaClass)
    private var profile: ServerProfile? = null

    // 外部から profile を取得するための「読み取り専用」プロパティ
    val currentProfile: ServerProfile? get() = profile

    /**
     * 現在サーバーと接続中であり、かつ認証が完了しているかどうかを返す。
     */
    val isConnected: Boolean
        get() = client?.isConnected == true && client?.isAuthenticated == true && profile != null

    /**
     * SFTP操作が可能かどうか（接続済みかつSFTPクライアントが有効か）を返す。
     */
    val isSftpActive: Boolean
        get() = isConnected && sftpClient != null

    /**
     * 指定されたサーバープロファイルを使用してSSH接続を確立し、認証を行います。
     *
     * @param profile 接続先ホスト、ポート、ユーザー、鍵、および対象ディレクトリパスを含む[ServerProfile]
     * @return 接続および認証が成功し、かつ指定されたパスがディレクトリとして存在すれば true
     */
    fun connect(profile: ServerProfile): Boolean {
        Security.addProvider(BouncyCastleProvider())
        val sshClient = SSHClient()
        this.client = sshClient

        // ホストキーの検証をスキップ (StrictHostKeyChecking=no 相当)
        sshClient.addHostKeyVerifier(PromiscuousVerifier())

        return try {
            logger.info("${profile.name} への接続を開始します...")

            // 接続試行
            sshClient.connect(profile.host, profile.port)

            // 認証 (公開鍵認証)
            val keyProvider = sshClient.loadKeys(profile.key)
            sshClient.authPublickey(profile.user, keyProvider)

            if (sshClient.isAuthenticated) {
                logger.info("認証成功！")

                sftpClient = sshClient.newSFTPClient()

                try {
                    val attributes = sftpClient?.stat(profile.path)
                    // 指定されたパスがディレクトリであることを確認
                    if (attributes?.type == FileMode.Type.DIRECTORY) {
                        logger.info("ディレクトリを確認しました: ${profile.path}")
                        this.profile = profile
                        true
                    } else {
                        CustomDialog.error(ErrorType.INVALID_DIRECTORY)
                            .exception(null, listOf("指定されたパスはディレクトリではありません:", profile.path))
                            .show()
                        false
                    }
                } catch (e: Exception) {
                    CustomDialog.error(ErrorType.DIRECTORY_NOT_FOUND)
                        .exception(e, listOf("ディレクトリが存在しないか、アクセス権限がありません:", profile.path))
                        .show()
                    false
                }
            } else {
                CustomDialog.error(ErrorType.AUTHENTICATION_FAILED)
                    .exception(null, listOf("秘密鍵又はユーザー名に誤りがあります。"))
                    .show()
                false
            }
        } catch (e: Exception) {
            CustomDialog.error(ErrorType.CONNECTION_FAILED)
                .content(listOf(
                    "サーバーとの通信に失敗しました。",
                    "Hostの誤り、サーバーダウン、またはデバイスがオフラインの可能性があります。"
                ))
                .exception(e, listOf("SSHJ接続エラー（接続フェーズでの例外発生）"))
                .show()
            EditorSession.disconnect()
            false
        }
    }

    /**
     * 現在のSFTPセッションおよびSSHクライアントを切断します。
     * 実行後は各クライアントを null にリセットします。
     */
    fun disconnect() {
        // SFTPを閉じる（例外が発生しても処理を続行）
        runCatching { sftpClient?.close() }

        // SSHクライアントを閉じる
        runCatching {
            client?.disconnect()
            client?.close()
        }

        sftpClient = null
        client = null
        profile = null

        logger.info("SSHセッションを正常に終了しました。")
    }

    /**
     * 指定されたパスのファイル一覧を返す。
     * sftpClient が null の場合は null を返す。
     */
    fun listFilesOrThrow(path: String): List<RemoteResourceInfo> {
        if (!isSftpActive) {
            throw IllegalStateException("SFTP session is not active")
        }

        return sftpClient
            ?.ls(path)
            ?.filterNotNull()
            ?: emptyList()
    }

    fun download(remotePath: String, localPath: String) {
        sftpClient?.get(remotePath, localPath)
    }

    fun upload(localPath: String, remotePath: String) {
        val normalizedRemotePath = remotePath
            .replace("\\", "/")
            .replace(Regex("/+"), "/")

        val parentPath = normalizedRemotePath.substringBeforeLast(
            delimiter = "/",
            missingDelimiterValue = ""
        )

        if (parentPath.isNotBlank()) {
            createDirectories(parentPath)
        }

        sftpClient?.put(localPath, normalizedRemotePath)
    }

    /**
     * 指定されたリモートパスのファイルを物理削除します。
     *
     * 内部で SSHJ の SFTPClient.rm(path) を実行します。
     * 接続がアクティブでない場合、または削除処理中に例外（ファイル不在、権限不足など）が発生した場合は
     * そのまま例外（IOException 等）をスローするため、呼び出し側で適切に catch して判定してください。
     *
     * @param remotePath 削除対象のリモートファイルの絶対パス
     * @throws IllegalStateException SFTPセッションがアクティブでない場合
     * @throws java.io.IOException 削除に失敗した場合（ファイル不在、権限不足、ネットワークエラー等）
     */
    fun remove(remotePath: String) {
        // 💡 そもそも接続が生きていなければ状態不正として例外を投げる
        if (!isSftpActive) {
            throw IllegalStateException("SFTP session is not active")
        }

        // 💡 SSHJのファイル削除メソッドを実行
        sftpClient?.rm(remotePath)
    }

    /**
     * 指定されたリモートパスのファイル名を変更（移動）します。
     *
     * 内部で SSHJ の SFTPClient.rename(oldPath, newPath) を実行します。
     * 接続がアクティブでない場合、またはリネーム処理中に例外（ファイル不在、変更先の重複、権限不足など）が
     * 発生した場合は例外をスローするため、呼び出し側（DataService）で適切に catch して判定してください。
     *
     * @param oldRemotePath 変更前のリモートファイルの絶対パス
     * @param newRemotePath 変更後の新しいリモートファイルの絶対パス
     * @throws IllegalStateException SFTPセッションがアクティブでない場合
     * @throws java.io.IOException リネームに失敗した場合
     */
    fun rename(oldRemotePath: String, newRemotePath: String) {
        if (!isSftpActive) {
            throw IllegalStateException("SFTP session is not active")
        }
        // 💡 SSHJのネイティブリネームメソッドを実行
        sftpClient?.rename(oldRemotePath, newRemotePath)
    }

    fun createDirectories(remoteDirPath: String) {
        if (!isSftpActive) {
            throw IllegalStateException("SFTP session is not active")
        }

        val normalizedPath = remoteDirPath
            .replace("\\", "/")
            .replace(Regex("/+"), "/")
            .trimEnd('/')

        if (normalizedPath.isBlank()) return

        val parts = normalizedPath
            .split("/")
            .filter { it.isNotBlank() }

        var currentPath = if (normalizedPath.startsWith("/")) "/" else ""

        for (part in parts) {
            currentPath = when {
                currentPath == "/" -> "/$part"
                currentPath.isBlank() -> part
                else -> "$currentPath/$part"
            }

            try {
                val attributes = sftpClient?.stat(currentPath)

                if (attributes?.type != FileMode.Type.DIRECTORY) {
                    throw IllegalStateException("指定パスはディレクトリではありません: $currentPath")
                }
            } catch (e: net.schmizz.sshj.sftp.SFTPException) {
                if (e.statusCode == net.schmizz.sshj.sftp.Response.StatusCode.NO_SUCH_FILE) {
                    sftpClient?.mkdir(currentPath)
                } else {
                    throw e
                }
            }
        }
    }
}