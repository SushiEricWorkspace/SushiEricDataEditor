package io.github.toumokorosi01.sushiericdataeditor2.editor.service

import io.github.toumokorosi01.common.Dir
import io.github.toumokorosi01.common.data.core.DataType
import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.ore.data.OreData
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import io.github.toumokorosi01.sushiericdataeditor2.communication.RemoteResource
import io.github.toumokorosi01.sushiericdataeditor2.communication.SshManager
import io.github.toumokorosi01.sushiericdataeditor2.config.FilePath
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.DeleteResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.LoadResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.RenameResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.SaveResult
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.Response
import net.schmizz.sshj.sftp.SFTPException
import org.slf4j.LoggerFactory

/**
 * エディタ内で使用するデータ操作処理をまとめるサービスクラス。
 *
 * このクラスは、SSH/SFTP接続を利用したリモートデータの読み込み・保存・削除・リネームや、
 * ローカル自動保存バックアップの作成・復元・削除などを担当します。
 *
 * データ種別ごとの操作は[items]、[ores]、[mobs]から行います。
 * これにより、呼び出し側は毎回[DataType]を渡さずに、
 * `dataService.items.load(fileName)`のように対象データ種別を明示できます。
 *
 * @property ssh リモートサーバーとのSSH/SFTP通信を管理する[SshManager]。
 */
class EditorDataService(private val ssh: SshManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * アイテムデータ用の操作アクセサ。
     */
    val items: DataAccess<ItemData> = DataAccess(DataType.Item)

    /**
     * 鉱石データ用の操作アクセサ。
     */
    val ores: DataAccess<OreData> = DataAccess(DataType.Ore)

    /**
     * モブデータ用の操作アクセサ。
     */
    val mobs: DataAccess<MobData> = DataAccess(DataType.Mob)

    /**
     * 現在選択されている接続プロファイル名を取得します。
     *
     * 接続プロファイルが選択されていない場合は`null`を返します。
     *
     * @return 現在の接続プロファイル名。未選択の場合は`null`。
     */
    val currentProfileName: String?
        get() = ssh.currentProfile?.name

    /**
     * 最初のサーバー選択画面へ戻ります。
     *
     * 現在の編集画面やデータ選択画面から離脱し、
     * サーバー接続先を選択する初期画面へ遷移するために使用します。
     */
    fun forceBackToSelect() {
        Utility.navigateToServerSelect()
    }

    /**
     * 特定のデータ種別に対する操作をまとめたアクセサ。
     *
     * このクラスは[DataType]を内部に保持することで、
     * 呼び出し側が読み込み・保存・削除・リネームなどのたびに
     * データ種別を引数として渡さなくてもよいようにします。
     *
     * 例えば、アイテムデータの場合は次のように呼び出せます。
     *
     * ```kotlin
     * dataService.items.load(fileName)
     * dataService.items.save(itemData)
     * dataService.items.delete(fileName)
     * ```
     *
     * @param T このアクセサが扱う管理データ型。[ManagedData]を実装している必要があります。
     * @property dataType このアクセサが扱うデータ種別。
     */
    inner class DataAccess<T : ManagedData<T, *>> internal constructor(
        private val dataType: DataType<T>
    ) {
        /** このデータタイプのの表示名 */
        val displayName: String
            get() = dataType.displayName

        /** 新規インスタンスの生成 */
        fun createDefault(id: String): T {
            return dataType.createDefault(id)
        }

        /**
         * このデータ種別に対応するリモートディレクトリ配下から、
         * YAML設定ファイル（.yml）の一覧を取得します。
         *
         * @return 取得されたリモートリソース一覧と、処理の成否フラグのペア。
         */
        fun listYmlResources(): Pair<List<RemoteResource>, Boolean> {
            return listYmlResourcesInternal(dataType)
        }

        /**
         * このデータ種別に対応するリモート設定ファイルをサーバーから読み込みます。
         *
         * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @return 読み込まれたデータと、処理結果を表す[LoadResult]のペア。
         */
        fun load(fileName: String): Pair<T?, LoadResult> {
            return loadInternal(
                fileName = fileName,
                dataType = dataType
            )
        }

        /**
         * 指定された[data]を、このデータ種別に対応するリモート設定ファイルとして保存します。
         *
         * @param fileName 保存先ファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @param data サーバーに保存する最新データ。
         * @return 保存処理の結果ステータスを表す[SaveResult]。
         */
        fun save(fileName: String, data: T): SaveResult {
            return saveInternal(
                fileName = fileName,
                data = data,
                dataType = dataType
            )
        }

        /**
         * 指定された[data]を、[ManagedData.id]をファイル名としてリモートサーバーへ保存します。
         *
         * @param data サーバーに保存する最新データ。
         * @return 保存処理の結果ステータスを表す[SaveResult]。
         */
        fun save(data: T): SaveResult {
            return save(
                fileName = data.id,
                data = data
            )
        }

        /**
         * 指定された[data]を、サーバー保存と同じ形式でローカルバックアップへ自動保存します。
         *
         * 主に、編集中データ用の`editing`、元データ用の`original`への保存に使用します。
         *
         * @param fileName 保存するファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @param subDirName 保存先のサブディレクトリ名。例: `editing`、`original`。
         * @param data ローカルバックアップとして保存するデータ。
         * @return 保存に成功した場合は`true`、失敗した場合は`false`。
         */
        fun saveToLocalBackup(
            fileName: String,
            subDirName: String,
            data: T
        ): Boolean {
            return saveToLocalBackupInternal(
                fileName = fileName,
                subDirName = subDirName,
                data = data,
                dataType = dataType
            )
        }

        /**
         * 指定された[data]を、[ManagedData.id]をファイル名としてローカルバックアップへ自動保存します。
         *
         * @param subDirName 保存先のサブディレクトリ名。例: `editing`、`original`。
         * @param data ローカルバックアップとして保存するデータ。
         * @return 保存に成功した場合は`true`、失敗した場合は`false`。
         */
        fun saveToLocalBackup(
            subDirName: String,
            data: T
        ): Boolean {
            return saveToLocalBackup(
                fileName = data.id,
                subDirName = subDirName,
                data = data
            )
        }

        /**
         * このデータ種別のローカルバックアップから、
         * 「編集中のキャッシュ」と「当時のオリジナル」をペアで同時に読み込みます。
         *
         * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @return 復元された[T]のペア。firstが編集中データ、secondがオリジナルデータ。
         *         復元できない場合は`null`。
         */
        fun loadBackupPair(fileName: String): Pair<T, T>? {
            return loadBackupPairInternal(
                fileName = fileName,
                dataType = dataType
            )
        }

        /**
         * このデータ種別のローカルバックアップファイルを削除します。
         *
         * 削除対象は`editing`と`original`の両方です。
         *
         * @param fileName 削除するバックアップファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         */
        fun deleteLocalBackup(fileName: String) {
            deleteLocalBackupInternal(
                fileName = fileName,
                dataType = dataType
            )
        }

        /**
         * このデータ種別に対応するリモート設定ファイルをサーバーから削除します。
         *
         * リモート削除が成功した場合、対応するローカルバックアップも削除します。
         *
         * @param fileName 削除するファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @return 削除処理の最終結果ステータスを表す[DeleteResult]。
         */
        fun delete(fileName: String): DeleteResult {
            return deleteInternal(
                fileName = fileName,
                dataType = dataType
            )
        }

        /**
         * このデータ種別に対応するリモート設定ファイルの名称を変更し、
         * ローカルに保持されている自動保存バックアップも新しい名称へ追従させます。
         *
         * @param oldName 変更前のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @param newName 変更後のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
         * @return リネーム処理の最終結果ステータスを表す[RenameResult]。
         */
        fun rename(oldName: String, newName: String): RenameResult {
            return renameInternal(
                oldName = oldName,
                newName = newName,
                dataType = dataType
            )
        }
    }

    /**
     * 指定されたデータ種別・バックアップ種別・ファイル名から、
     * ローカル自動保存バックアップファイルの保存先を解決します。
     *
     * このメソッドはファイルパスを組み立てるだけで、
     * ファイルや親ディレクトリの作成は行いません。
     *
     * バックアップファイルは、現在の接続プロファイル名ごとに分けて保存されます。
     * 接続プロファイルが存在しない場合は、`default`ディレクトリ配下のパスを返します。
     *
     * 生成されるパスの形式は次のようになります。
     *
     * ```text
     * AUTOSAVE_DIR/{profileName}/{categoryDirName}/{subDirName}/{fileName}.yml
     * ```
     *
     * @param categoryDirName データ種別ごとのカテゴリディレクトリ名。例: `items`、`ores`、`mobs`。
     * @param subDirName バックアップ種別を表すサブディレクトリ名。例: `editing`、`original`。
     * @param fileName バックアップ対象のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return ローカル自動保存バックアップファイルのパスを表す[java.io.File]。
     */
    private fun resolveLocalBackupFile(
        categoryDirName: String,
        subDirName: String,
        fileName: String
    ): java.io.File {
        val profileName = ssh.currentProfile?.name ?: "default"

        return FilePath.AUTOSAVE_DIR.toFile()
            .resolve(profileName)
            .resolve(categoryDirName)
            .resolve(subDirName)
            .resolve("$fileName.yml")
    }

    /**
     * 指定された[dataType]に対応するリモートディレクトリ配下から、
     * YAML設定ファイル（.yml）の一覧を取得します。
     *
     * ### 戻り値（返り値）の条件
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :--- | :--- |
     * | **正常に取得完了** | `List<RemoteResource>` to `true` | 一覧取得とフィルタリングに成功した場合 |
     * | **プロファイル未選択** | `emptyList()` to `false` | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **対象ディレクトリが不在** | `emptyList()` to `true` | リモート側に対象ディレクトリが存在しない場合 |
     * | **その他取得失敗** | `emptyList()` to `false` | 通信失敗、権限不足、予期しない例外が発生した場合 |
     *
     * @param dataType 一覧取得対象のデータ種別。
     * @return 取得されたリモートリソース一覧と、処理の成否フラグのペア。
     */
    private fun listYmlResourcesInternal(
        dataType: DataType<*>
    ): Pair<List<RemoteResource>, Boolean> {
        val relPath = dataType.dir.getRawPath()
        val profile = ssh.currentProfile ?: return emptyList<RemoteResource>() to false

        val targetFullPath = "${profile.path}/${Dir.BASE_ROOT}/$relPath"
            .replace(Regex("/+"), "/")

        return try {
            val resources = ssh.listFilesOrThrow(targetFullPath)

            val ymlList = resources
                .filter { entry ->
                    entry.attributes.type == FileMode.Type.REGULAR &&
                            entry.name.endsWith(".yml", ignoreCase = true)
                }
                .map { entry ->
                    RemoteResource(
                        name = entry.name,
                        remotePath = "$targetFullPath/${entry.name}".replace(Regex("/+"), "/")
                    )
                }

            ymlList to true
        } catch (e: SFTPException) {
            if (e.statusCode == Response.StatusCode.NO_SUCH_FILE) {
                emptyList<RemoteResource>() to true
            } else {
                logger.error("yml一覧取得失敗: $targetFullPath", e)
                emptyList<RemoteResource>() to false
            }
        } catch (e: Exception) {
            logger.error("yml一覧取得失敗: $targetFullPath", e)
            emptyList<RemoteResource>() to false
        }
    }

    /**
     * 指定された[dataType]に対応するリモート設定ファイルをサーバーからダウンロードし、
     * 管理データ型[T]として読み込みます。
     *
     * 内部ではOSの一時ディレクトリにテンポラリファイル（.yml）を作成し、
     * ダウンロードと読み込みが完了した後に、成功・失敗に関わらずその一時ファイルを削除します。
     *
     * ### 戻り値（返り値）の条件
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :--- | :--- |
     * | **正常に読み込み完了** | `T` to [LoadResult.SUCCESS] | リモートからの取得と解析に成功した場合 |
     * | **SFTP非アクティブ** | `null` to [LoadResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない場合 |
     * | **プロファイル未選択** | `null` to [LoadResult.PROFILE_NOT_SELECTED] | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **ファイルが存在しない** | `null` to [LoadResult.FILE_NOT_FOUND] | リモート側に指定ファイルが存在しない場合 |
     * | **YAML構造が不正** | `null` to [LoadResult.INVALID_YAML] | ファイル取得後、読み込み結果が`null`だった場合 |
     * | **その他読み込み失敗** | `null` to [LoadResult.FAILED] | 通信失敗、権限不足、予期しない例外が発生した場合 |
     *
     * @param T 読み込み対象の管理データ型。[ManagedData]を実装している必要があります。
     * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param dataType 読み込み対象のデータ種別。
     * @return 読み込まれたデータと、処理結果を表す[LoadResult]のペア。
     */
    private fun <T : ManagedData<T, *>> loadInternal(
        fileName: String,
        dataType: DataType<T>
    ): Pair<T?, LoadResult> {
        if (!ssh.isSftpActive) return null to LoadResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return null to LoadResult.PROFILE_NOT_SELECTED

        val fullPath = Utility.getFullRemotePath(profile, dataType.pathOf(fileName))
        var tempFile: java.io.File? = null

        return try {
            tempFile = kotlin.io.path.createTempFile("remote_load_", ".yml").toFile()

            ssh.download(fullPath, tempFile.absolutePath)

            val data = dataType.manager.load(tempFile, fileName)

            if (data == null) {
                null to LoadResult.INVALID_YAML
            } else {
                data to LoadResult.SUCCESS
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""

            if (msg.contains("No such file", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
                logger.warn("リモートファイルが存在しません: $fullPath")
                null to LoadResult.FILE_NOT_FOUND
            } else {
                logger.error("リモートからのダウンロードまたは解析中に致命的な例外が発生: $fullPath", e)
                null to LoadResult.FAILED
            }
        } finally {
            tempFile?.delete()
        }
    }

    /**
     * 指定された[data]を一時ファイルに書き出した後、
     * [dataType]に対応するリモート設定ファイルへアップロードして保存します。
     *
     * ### 戻り値（返り値）の条件
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :---: | :--- |
     * | **正常に保存完了** | [SaveResult.SUCCESS] | ローカル書き出しとサーバーアップロードに成功した場合 |
     * | **SFTP非アクティブ** | [SaveResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない、またはプロファイルが存在しない場合 |
     * | **書き出し・保存失敗** | [SaveResult.FAILED] | YAML生成失敗、通信失敗、権限不足などの例外が発生した場合 |
     *
     * @param T 保存対象の管理データ型。[ManagedData]を実装している必要があります。
     * @param fileName 保存先ファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param data サーバーに保存する最新データ。
     * @param dataType 保存対象のデータ種別。
     * @return 保存処理の結果ステータスを表す[SaveResult]。
     */
    private fun <T : ManagedData<T, *>> saveInternal(
        fileName: String,
        data: T,
        dataType: DataType<T>
    ): SaveResult {
        if (!ssh.isSftpActive) return SaveResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return SaveResult.SFTP_INACTIVE

        val fullPath = Utility.getFullRemotePath(profile, dataType.pathOf(fileName))
        var tempFile: java.io.File? = null

        return try {
            tempFile = kotlin.io.path.createTempFile("remote_save_", ".yml").toFile()

            dataType.manager.save(tempFile, data)

            ssh.upload(tempFile.absolutePath, fullPath)

            SaveResult.SUCCESS
        } catch (e: Exception) {
            logger.error("リモート保存失敗: $fullPath", e)
            SaveResult.FAILED
        } finally {
            tempFile?.delete()
        }
    }

    /**
     * 指定された[data]を、サーバー保存と同じ形式でローカルバックアップへ自動保存します。
     *
     * @param T 保存対象の管理データ型。[ManagedData]を実装している必要があります。
     * @param fileName 保存するファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param subDirName 保存先のサブディレクトリ名。例: `editing`、`original`。
     * @param data ローカルバックアップとして保存するデータ。
     * @param dataType 保存対象のデータ種別。
     * @return 保存に成功した場合は`true`、失敗した場合は`false`。
     */
    private fun <T : ManagedData<T, *>> saveToLocalBackupInternal(
        fileName: String,
        subDirName: String,
        data: T,
        dataType: DataType<T>
    ): Boolean {
        val categoryDirName = dataType.categoryDirName

        return try {
            val backupFile = resolveLocalBackupFile(categoryDirName, subDirName, fileName)
            backupFile.parentFile?.mkdirs()

            dataType.manager.save(backupFile, data)

            logger.debug("【自動保存成功】ローカルキャッシュ[$categoryDirName/$subDirName]を更新しました: ${backupFile.name}")
            true
        } catch (e: Exception) {
            logger.error(
                "ローカルへの自動保存中に例外が発生しました: categoryDirName=$categoryDirName, subDirName=$subDirName, fileName=$fileName",
                e
            )
            false
        }
    }

    /**
     * ローカルバックアップから、指定された[dataType]の「編集中のキャッシュ」と
     * 「当時のオリジナル」をペアで同時に読み込みます。
     *
     * @param T 読み込み対象の管理データ型。[ManagedData]を実装している必要があります。
     * @param fileName データID。拡張子（.ymlなど）は除いた名前を指定します。
     * @param dataType 読み込み対象のデータ種別。
     * @return 復元された[T]のペア。firstが編集中データ、secondがオリジナルデータ。
     *         復元できない場合は`null`。
     */
    private fun <T : ManagedData<T, *>> loadBackupPairInternal(
        fileName: String,
        dataType: DataType<T>
    ): Pair<T, T>? {
        val categoryDirName = dataType.categoryDirName

        return try {
            val editingFile = resolveLocalBackupFile(categoryDirName, "editing", fileName)
            val originalFile = resolveLocalBackupFile(categoryDirName, "original", fileName)

            if (!editingFile.exists() || !originalFile.exists()) return null

            val editingData = dataType.manager.load(editingFile, null) ?: return null
            val originalData = dataType.manager.load(originalFile, null) ?: return null

            logger.info("【自動保存からの復元】新旧ペアのローカルキャッシュを復元しました: $categoryDirName/$fileName")
            Pair(editingData, originalData)
        } catch (e: Exception) {
            logger.error("ローカルキャッシュ（ペア）の読み込みに失敗しました: categoryDirName=$categoryDirName, fileName=$fileName", e)
            null
        }
    }

    /**
     * 指定された[dataType]に対応するローカルバックアップファイルを削除します。
     *
     * 削除対象は、現在のプロファイルに対応する自動保存ディレクトリ内の
     * `editing`と`original`です。
     *
     * @param fileName 削除するバックアップファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param dataType 削除対象のデータ種別。
     */
    private fun deleteLocalBackupInternal(
        fileName: String,
        dataType: DataType<*>
    ) {
        val categoryDirName = dataType.categoryDirName

        try {
            val editingFile = resolveLocalBackupFile(categoryDirName, "editing", fileName)
            val originalFile = resolveLocalBackupFile(categoryDirName, "original", fileName)

            if (editingFile.exists()) editingFile.delete()
            if (originalFile.exists()) originalFile.delete()

            logger.info("【バックアップ削除】用済みのためローカルキャッシュを削除しました: $categoryDirName/$fileName")
        } catch (e: Exception) {
            logger.error("ローカルバックアップの削除に失敗しました: categoryDirName=$categoryDirName, fileName=$fileName", e)
        }
    }

    /**
     * 指定された[dataType]に対応するリモート設定ファイルをサーバーから削除します。
     *
     * リモート削除が成功した場合、同じデータ種別のローカルバックアップも削除します。
     *
     * @param fileName 削除するファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param dataType 削除対象のデータ種別。
     * @return 削除処理の最終結果ステータスを表す[DeleteResult]。
     */
    private fun deleteInternal(
        fileName: String,
        dataType: DataType<*>
    ): DeleteResult {
        if (!ssh.isSftpActive) return DeleteResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return DeleteResult.PROFILE_NOT_SELECTED

        val fullPath = Utility.getFullRemotePath(profile, dataType.pathOf(fileName))

        return try {
            ssh.remove(fullPath)
            deleteLocalBackupInternal(fileName, dataType)

            DeleteResult.SUCCESS
        } catch (e: Exception) {
            val msg = e.message ?: ""

            if (msg.contains("No such file", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
                logger.warn("削除対象のリモートファイルが存在しません: $fullPath")
                DeleteResult.FILE_NOT_FOUND
            } else {
                logger.error("リモートファイルの削除中に致命的な例外が発生: $fullPath", e)
                DeleteResult.FAILED
            }
        }
    }

    /**
     * 指定された[dataType]に対応するリモート設定ファイルの名称を変更し、
     * ローカルに保持されている自動保存バックアップも新しい名称へ追従させます。
     *
     * リモートリネームが成功した場合、`editing`と`original`に存在するローカルバックアップも改名します。
     * ローカルバックアップの読み込みに成功した場合は、内部IDも[newName]へ更新して保存し直します。
     * 読み込みに失敗した場合は、ファイル名だけをリネームします。
     *
     * @param T リネーム対象の管理データ型。[ManagedData]を実装している必要があります。
     * @param oldName 変更前のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param newName 変更後のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param dataType リネーム対象のデータ種別。
     * @return リネーム処理の最終結果ステータスを表す[RenameResult]。
     */
    private fun <T : ManagedData<T, *>> renameInternal(
        oldName: String,
        newName: String,
        dataType: DataType<T>
    ): RenameResult {
        if (!ssh.isSftpActive) return RenameResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return RenameResult.PROFILE_NOT_SELECTED

        val oldFullPath = Utility.getFullRemotePath(profile, dataType.pathOf(oldName))
        val newFullPath = Utility.getFullRemotePath(profile, dataType.pathOf(newName))

        return try {
            ssh.rename(oldFullPath, newFullPath)

            listOf("editing", "original").forEach { subDir ->
                val oldLocalFile = resolveLocalBackupFile(dataType.categoryDirName, subDir, oldName)

                if (oldLocalFile.exists()) {
                    val newLocalFile = resolveLocalBackupFile(dataType.categoryDirName, subDir, newName)
                    val localData = dataType.manager.load(oldLocalFile, null)

                    if (localData != null) {
                        localData.id = newName

                        newLocalFile.parentFile?.mkdirs()
                        dataType.manager.save(newLocalFile, localData)

                        oldLocalFile.delete()
                        logger.info(
                            "【バックアップ内部ID更新】ローカルキャッシュ[${dataType.categoryDirName}/$subDir]の内部IDを同期しました: $oldName -> $newName"
                        )
                    } else {
                        oldLocalFile.renameTo(newLocalFile)
                    }
                }
            }

            RenameResult.SUCCESS
        } catch (e: Exception) {
            val msg = e.message ?: ""

            when {
                msg.contains("No such file", ignoreCase = true) || msg.contains("not found", ignoreCase = true) -> {
                    logger.warn("リネーム対象のリモートファイルが存在しません: $oldFullPath")
                    RenameResult.FILE_NOT_FOUND
                }

                msg.contains("Already exists", ignoreCase = true) || msg.contains("Failure", ignoreCase = true) -> {
                    logger.warn("変更後のファイル名が既に存在するか、サーバーから拒否されました: $newFullPath")
                    RenameResult.ALREADY_EXISTS
                }

                else -> {
                    logger.error("リモートファイルのリネーム中に致命的な例外が発生: $oldFullPath -> $newFullPath", e)
                    RenameResult.FAILED
                }
            }
        }
    }
}