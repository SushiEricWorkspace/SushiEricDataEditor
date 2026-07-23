package io.github.toumokorosi01.sushiericdataeditor2.communication

import org.slf4j.Logger

enum class SshFailureCode {
    DNS_RESOLUTION_FAILED,
    HOST_UNREACHABLE,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    HOST_KEY_NOT_APPROVED,
    HOST_KEY_CHANGED,
    HOST_KEY_STORE_FAILED,
    PASSWORD_AUTHENTICATION_FAILED,
    PASSWORD_AUTHENTICATION_DISABLED,
    PUBLIC_KEY_AUTHENTICATION_FAILED,
    PRIVATE_KEY_NOT_FOUND,
    PRIVATE_KEY_PERMISSION_FAILED,
    PRIVATE_KEY_FORMAT_UNSUPPORTED,
    PRIVATE_KEY_PASSPHRASE_REQUIRED,
    REMOTE_DIRECTORY_NOT_FOUND,
    REMOTE_PATH_NOT_DIRECTORY,
    REMOTE_DIRECTORY_NOT_READABLE,
    REMOTE_DIRECTORY_NOT_WRITABLE,
    AUTHORIZED_KEYS_UPDATE_FAILED,
    KEY_GENERATION_FAILED,
    SFTP_UNAVAILABLE,
    PROFILE_SAVE_FAILED,
    UNEXPECTED
}

data class SshFailure(
    val code: SshFailureCode,
    val detail: String? = null,
    val publicKeyRegistered: Boolean = false,
    val generatedPrivateKeyPath: String? = null,
    val generatedKeyFormat: String? = null
) {
    val title: String
        get() = when (code) {
            SshFailureCode.HOST_KEY_CHANGED -> "ホスト鍵が変更されています"
            SshFailureCode.HOST_KEY_NOT_APPROVED -> "ホスト鍵が承認されませんでした"
            SshFailureCode.PASSWORD_AUTHENTICATION_FAILED,
            SshFailureCode.PASSWORD_AUTHENTICATION_DISABLED,
            SshFailureCode.PUBLIC_KEY_AUTHENTICATION_FAILED -> "SSH認証に失敗しました"
            SshFailureCode.PRIVATE_KEY_NOT_FOUND,
            SshFailureCode.PRIVATE_KEY_PERMISSION_FAILED,
            SshFailureCode.PRIVATE_KEY_FORMAT_UNSUPPORTED,
            SshFailureCode.PRIVATE_KEY_PASSPHRASE_REQUIRED -> "秘密鍵を使用できません"
            SshFailureCode.REMOTE_DIRECTORY_NOT_FOUND,
            SshFailureCode.REMOTE_PATH_NOT_DIRECTORY,
            SshFailureCode.REMOTE_DIRECTORY_NOT_READABLE,
            SshFailureCode.REMOTE_DIRECTORY_NOT_WRITABLE -> "接続先ディレクトリを使用できません"
            else -> "SSH処理に失敗しました"
        }

    fun userMessage(): String {
        val baseMessage = when (code) {
            SshFailureCode.DNS_RESOLUTION_FAILED ->
                "ホスト名をIPアドレスへ変換できませんでした。ホスト名を確認してください。"

            SshFailureCode.HOST_UNREACHABLE ->
                "接続先ホストへ到達できません。ネットワーク、SSHポート、ファイアウォールを確認してください。"

            SshFailureCode.CONNECTION_REFUSED ->
                "接続先がSSH接続を拒否しました。SSHサービスが起動しているか、ポート番号が正しいか確認してください。"

            SshFailureCode.CONNECTION_TIMEOUT ->
                "SSH接続がタイムアウトしました。接続先の状態とネットワークを確認してください。"

            SshFailureCode.HOST_KEY_NOT_APPROVED ->
                "初回接続時のホスト鍵が承認されなかったため、接続を中止しました。"

            SshFailureCode.HOST_KEY_CHANGED ->
                "保存済みのホスト鍵と、現在サーバーが提示したホスト鍵が一致しません。中間者攻撃またはサーバー再構築の可能性があるため接続を拒否しました。"

            SshFailureCode.HOST_KEY_STORE_FAILED ->
                "承認したホスト鍵をアプリのホスト鍵保存ファイルへ安全に保存できませんでした。"

            SshFailureCode.PASSWORD_AUTHENTICATION_FAILED ->
                "接続先OSのユーザー名またはパスワードが正しくありません。"

            SshFailureCode.PASSWORD_AUTHENTICATION_DISABLED ->
                "接続先ではSSHのパスワード認証が無効になっている可能性があります。初回公開鍵登録時だけパスワード認証を有効にしてください。"

            SshFailureCode.PUBLIC_KEY_AUTHENTICATION_FAILED ->
                "秘密鍵による公開鍵認証に失敗しました。ユーザー名、秘密鍵、接続先のauthorized_keysを確認してください。"

            SshFailureCode.PRIVATE_KEY_NOT_FOUND ->
                "指定された秘密鍵ファイルが見つかりません。秘密鍵のパスを確認してください。"

            SshFailureCode.PRIVATE_KEY_PERMISSION_FAILED ->
                "秘密鍵を現在のOSユーザーだけが利用できる権限へ設定できませんでした。安全のため鍵生成を中止しました。"

            SshFailureCode.PRIVATE_KEY_FORMAT_UNSUPPORTED ->
                "秘密鍵の形式をSSHライブラリが読み込めません。PKCS#8、PEM、OpenSSH形式などの対応鍵を選択してください。"

            SshFailureCode.PRIVATE_KEY_PASSPHRASE_REQUIRED ->
                "この秘密鍵はパスフレーズを必要とします。現在のGUI実装ではパスフレーズ付き秘密鍵を保存せずに再利用する処理は未対応です。"

            SshFailureCode.REMOTE_DIRECTORY_NOT_FOUND ->
                "指定された接続先ルートディレクトリが存在しません。"

            SshFailureCode.REMOTE_PATH_NOT_DIRECTORY ->
                "指定された接続先パスはディレクトリではありません。"

            SshFailureCode.REMOTE_DIRECTORY_NOT_READABLE ->
                "指定された接続先ルートディレクトリを読み取る権限がありません。"

            SshFailureCode.REMOTE_DIRECTORY_NOT_WRITABLE ->
                "指定された接続先ルートディレクトリへ書き込む権限がありません。"

            SshFailureCode.AUTHORIZED_KEYS_UPDATE_FAILED ->
                "接続先OSの公開鍵認証ファイルへ公開鍵を安全に登録できませんでした。"

            SshFailureCode.KEY_GENERATION_FAILED ->
                "SSH鍵ペアを生成できませんでした。アプリデータディレクトリの権限を確認してください。"

            SshFailureCode.SFTP_UNAVAILABLE ->
                "SSH認証には成功しましたが、SFTPセッションを開始できませんでした。"

            SshFailureCode.PROFILE_SAVE_FAILED ->
                "SSH接続の確認には成功しましたが、接続プロファイルを設定ファイルへ保存できませんでした。"

            SshFailureCode.UNEXPECTED ->
                "予期しないSSHエラーが発生しました。秘密情報を除いたエラー種別をログで確認してください。"
        }

        return buildString {
            append(baseMessage)
            detail?.takeIf { it.isNotBlank() }?.let {
                append("\n\n詳細: ")
                append(it)
            }
            if (publicKeyRegistered) {
                if (code == SshFailureCode.PROFILE_SAVE_FAILED) {
                    append("\n\n公開鍵登録と秘密鍵認証には成功しています。authorized_keysから自動削除は行っていません。")
                } else {
                    append("\n\n公開鍵の登録には成功しましたが、生成した秘密鍵での接続確認に失敗しました。authorized_keysから自動削除は行っていません。")
                }
            }
        }
    }
}

sealed interface SshResult<out T> {
    data class Success<T>(val value: T) : SshResult<T>
    data class Failure(val failure: SshFailure) : SshResult<Nothing>
}

object SecretRedactor {
    fun redact(text: String?, secrets: Iterable<CharSequence>): String {
        var result = text.orEmpty()
        secrets
            .map(CharSequence::toString)
            .filter { it.isNotEmpty() }
            .forEach { secret -> result = result.replace(secret, "***") }
        return result
    }

    fun safeThrowableName(throwable: Throwable): String {
        return throwable::class.qualifiedName ?: "Throwable"
    }
}

object SafeSshLogger {
    fun warn(
        logger: Logger,
        event: String,
        failureCode: SshFailureCode? = null,
        throwable: Throwable? = null
    ) {
        logger.warn(
            "SSH event={}, code={}, type={}",
            event,
            failureCode?.name ?: "NONE",
            throwable?.let(SecretRedactor::safeThrowableName) ?: "NONE"
        )
    }
}
