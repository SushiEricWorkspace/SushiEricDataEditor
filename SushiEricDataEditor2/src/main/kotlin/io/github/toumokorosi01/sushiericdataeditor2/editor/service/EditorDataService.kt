package io.github.toumokorosi01.sushiericdataeditor2.editor.service

import io.github.toumokorosi01.common.Dir
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.item.ItemManager
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.mob.MobManager
import io.github.toumokorosi01.common.data.ore.data.OreData
import io.github.toumokorosi01.common.data.ore.OreManager
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

class EditorDataService(private val ssh: SshManager) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val currentProfileName: String?
        get() = ssh.currentProfile?.name

    private fun localBackupFile(categoryDirName: String, subDirName: String, fileName: String): java.io.File {
        val profileName = ssh.currentProfile?.name ?: "default"
        return FilePath.AUTOSAVE_DIR.toFile()
            .resolve(profileName)
            .resolve(categoryDirName)
            .resolve(subDirName)
            .resolve("$fileName.yml")
    }

    /**
     * 手元で変更されたItemデータまたはベースとなったオリジナルデータを、
     * サーバー保存と「全く同じ形式」でローカルの指定フォルダに自動保存します。
     */
    fun saveToLocalBackup(fileName: String, subDirName: String, itemData: ItemData): Boolean {
        return saveToLocalBackupCore(
            fileName = fileName,
            subDirName = subDirName,
            categoryDirName = "items",
            data = itemData,
            save = ItemManager::save
        )
    }

    /**
     * 手元で変更されたOreデータまたはベースとなったオリジナルデータを、
     * サーバー保存と「全く同じ形式」でローカルの指定フォルダに自動保存します。
     */
    fun saveToLocalBackup(fileName: String, subDirName: String, oreData: OreData): Boolean {
        return saveToLocalBackupCore(
            fileName = fileName,
            subDirName = subDirName,
            categoryDirName = "ores",
            data = oreData,
            save = OreManager::save
        )
    }

    /**
     * 手元で変更されたMobデータまたはベースとなったオリジナルデータを、
     * サーバー保存と「全く同じ形式」でローカルの指定フォルダに自動保存します。
     */
    fun saveToLocalBackup(fileName: String, subDirName: String, mobData: MobData): Boolean {
        return saveToLocalBackupCore(
            fileName = fileName,
            subDirName = subDirName,
            categoryDirName = "mobs",
            data = mobData,
            save = MobManager::save
        )
    }

    private fun <T> saveToLocalBackupCore(
        fileName: String,
        subDirName: String,
        categoryDirName: String,
        data: T,
        save: (java.io.File, T) -> Unit
    ): Boolean {
        return try {
            val backupFile = localBackupFile(categoryDirName, subDirName, fileName)
            backupFile.parentFile?.mkdirs()

            save(backupFile, data)

            logger.debug("【自動保存成功】ローカルキャッシュ[$categoryDirName/$subDirName]を更新しました: ${backupFile.name}")
            true
        } catch (e: Exception) {
            logger.error("ローカルへの自動保存中に例外が発生しました: categoryDirName=$categoryDirName, subDirName=$subDirName, fileName=$fileName", e)
            false
        }
    }

    /**
     * ローカルから「編集中のキャッシュ」と「当時のオリジナル」をペアで同時に読み込みます。
     *
     * @param fileName アイテムID（拡張子なし）
     * @return 復元された [Pair<ItemData, ItemData>] (first: 編集データ, second: オリジナルデータ)。
     *         どちらか一方でも欠けている、または破損している場合は不整合を防ぐため null を返します。
     */
    fun loadBackupPair(fileName: String): Pair<ItemData, ItemData>? {
        try {
            val profileName = ssh.currentProfile?.name ?: "default"
            val itemsDir = FilePath.AUTOSAVE_DIR.toFile().resolve(profileName).resolve("items")

            val editingFile = itemsDir.resolve("editing").resolve("$fileName.yml")
            val originalFile = itemsDir.resolve("original").resolve("$fileName.yml")

            // 💡 整合性を保つため、新旧両方のファイルが揃っているときだけ復元対象とする
            if (!editingFile.exists() || !originalFile.exists()) return null

            // それぞれ同じ Configurate 形式で安全にデコード
            val editingData = ItemManager.load(editingFile) ?: return null
            val originalData = ItemManager.load(originalFile) ?: return null

            logger.info("【自動保存からの復元】新旧ペアのローカルキャッシュを復元しました: $fileName")
            return Pair(editingData, originalData)

        } catch (e: Exception) {
            logger.error("ローカルキャッシュ（ペア）の読み込みに失敗しました: fileName=$fileName", e)
            return null
        }
    }


    fun loadOreBackupPair(fileName: String): Pair<OreData, OreData>? {
        return loadBackupPairCore(
            fileName = fileName,
            categoryDirName = "ores",
            load = OreManager::load
        )
    }

    fun loadMobBackupPair(fileName: String): Pair<MobData, MobData>? {
        return loadBackupPairCore(
            fileName = fileName,
            categoryDirName = "mobs",
            load = MobManager::load
        )
    }

    private fun <T> loadBackupPairCore(
        fileName: String,
        categoryDirName: String,
        load: (java.io.File, String?) -> T?
    ): Pair<T, T>? {
        return try {
            val editingFile = localBackupFile(categoryDirName, "editing", fileName)
            val originalFile = localBackupFile(categoryDirName, "original", fileName)

            if (!editingFile.exists() || !originalFile.exists()) return null

            val editingData = load(editingFile, null) ?: return null
            val originalData = load(originalFile, null) ?: return null

            logger.info("【自動保存からの復元】新旧ペアのローカルキャッシュを復元しました: $categoryDirName/$fileName")
            Pair(editingData, originalData)
        } catch (e: Exception) {
            logger.error("ローカルキャッシュ（ペア）の読み込みに失敗しました: categoryDirName=$categoryDirName, fileName=$fileName", e)
            null
        }
    }


    /**
     * サーバー保存成功時や削除時に、用済みとなったItemローカルバックアップファイルを削除します。
     */
    fun deleteLocalBackup(fileName: String) {
        deleteLocalBackupCore(fileName, "items")
    }

    /**
     * サーバー保存成功時や削除時に、用済みとなったOreローカルバックアップファイルを削除します。
     */
    fun deleteOreLocalBackup(fileName: String) {
        deleteLocalBackupCore(fileName, "ores")
    }

    /**
     * サーバー保存成功時や削除時に、用済みとなったMobローカルバックアップファイルを削除します。
     */
    fun deleteMobLocalBackup(fileName: String) {
        deleteLocalBackupCore(fileName, "mobs")
    }

    private fun deleteLocalBackupCore(fileName: String, categoryDirName: String) {
        try {
            val editingFile = localBackupFile(categoryDirName, "editing", fileName)
            val originalFile = localBackupFile(categoryDirName, "original", fileName)

            if (editingFile.exists()) editingFile.delete()
            if (originalFile.exists()) originalFile.delete()

            logger.info("【バックアップ削除】用済みのためローカルキャッシュを削除しました: $categoryDirName/$fileName")
        } catch (e: Exception) {
            logger.error("ローカルバックアップの削除に失敗しました: categoryDirName=$categoryDirName, fileName=$fileName", e)
        }
    }

    fun forceBackToSelect() {
        Utility.navigateToServerSelect()
    }

    /**
     * 指定されたリモートディレクトリ配下にあるYAML設定ファイル（.yml）の一覧をサーバーから取得します。
     *
     * この処理は、指定された [Dir] からリモートのフルパスを算出し、サーバー上の通常ファイル（REGULAR）かつ
     * 拡張子が `.yml` で終わるものだけをフィルタリングして [RemoteResource] のリストとして返します。
     *
     * 接続先のプロファイルが存在しない場合や、下層のSSH通信でエラーと判定された場合、あるいは
     * ディレクトリが存在しないなどの例外が発生した場合は、安全に空のリストと失敗フラグを返します。
     *
     * ### 戻り値（返り値）の条件:
     * 返り値は `Pair<List<RemoteResource>, Boolean>` の形式で返されます。
     * - **`List<RemoteResource>` (First)**: 取得およびフィルタリングに成功したリソースのリスト。失敗時は空のリスト（`emptyList()`）になります。
     * - **`Boolean` (Second)**: 処理全体の成否フラグ。一覧の取得に成功、または親ディレクトリが存在しないだけの場合は `true`、何らかの理由で失敗した場合は `false` になります。
     *
     * | 発生する状況 | 戻り値 (`First` to `Second`) | 概要 |
     * | :--- | :--- | :--- |
     * | **正常に取得完了** | `List<RemoteResource>` to `true` | リモートからのファイル一覧取得・フィルタリングにすべて成功した場合（0件の場合も含む） |
     * | **プロファイル未選択** | `emptyList()` to `false` | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **SSH側で取得失敗** | `emptyList()` to `false` | `ssh.listFiles` が内部的に失敗（`isSuccess` が `false`）と判定した場合 |
     * | **対象ディレクトリが不在** | `emptyList()` to `true` | リモート側に指定のディレクトリが存在しない場合（`NO_SUCH_FILE` 例外から自動判別し、ログ出力はスキップ） |
     * | **その他通信失敗** | `emptyList()` to `false` | 上記以外の致命的なSFTP例外や予期せぬI/O例外が発生した場合（スタックトレースをログ出力） |
     *
     * @param dir 取得対象とするリモートのデータディレクトリ種別（例: [Dir.Item.Stats]）
     * @return フィルタリングされたリモートリソースのリストと、処理の成否フラグのペア
     */
    fun listYmlResources(dir: Dir): Pair<List<RemoteResource>, Boolean> {
        val relPath = dir.getRawPath()
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
     * 指定されたファイル名のリモートアイテム設定ファイルをサーバーからダウンロードし、[ItemData]として読み込みます。
     *
     * 読み込み処理の共通仕様、戻り値の条件、エラー時の[LoadResult]については[loadCore]を参照してください。
     *
     * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return 読み込まれた[ItemData]（失敗時は`null`）と、処理結果を表す[LoadResult]のペア
     */
    fun loadItem(fileName: String): Pair<ItemData?, LoadResult> {
        return loadCore(
            fileName = fileName,
            path = Dir.Item.Stats.File(fileName),
            load = ItemManager::load
        )
    }

    /**
     * 指定されたファイル名のリモート鉱石設定ファイルをサーバーからダウンロードし、[OreData]として読み込みます。
     *
     * 読み込み処理の共通仕様、戻り値の条件、エラー時の[LoadResult]については[loadCore]を参照してください。
     *
     * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return 読み込まれた[OreData]（失敗時は`null`）と、処理結果を表す[LoadResult]のペア
     */
    fun loadOre(fileName: String): Pair<OreData?, LoadResult> {
        return loadCore(
            fileName = fileName,
            path = Dir.Ore.Ores.File(fileName),
            load = OreManager::load
        )
    }

    /**
     * 指定されたファイル名のリモートモブ設定ファイルをサーバーからダウンロードし、[MobData]として読み込みます。
     *
     * 読み込み処理の共通仕様、戻り値の条件、エラー時の[LoadResult]については[loadCore]を参照してください。
     *
     * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return 読み込まれた[MobData]（失敗時は`null`）と、処理結果を表す[LoadResult]のペア
     */
    fun loadMob(fileName: String): Pair<MobData?, LoadResult> {
        return loadCore(
            fileName = fileName,
            path = Dir.Mob.Mobs.File(fileName),
            load = MobManager::load
        )
    }

    /**
     * 指定されたリモート設定ファイルをサーバーからダウンロードし、任意のデータ型[T]として読み込みます。
     *
     * この処理は、内部でOSの一時ディレクトリにテンポラリファイル（.yml）を作成してダウンロードを行い、
     * 読み込み完了後に自動でその一時ファイルを削除します。
     *
     * リモートファイルが存在しない場合は、ダウンロード時の例外メッセージ（No such file等）から
     * 自動的に判別して適切なステータスを返します。
     *
     * ### 戻り値（返り値）の条件
     *
     * 返り値は`Pair<T?, LoadResult>`の形式で返されます。
     *
     * - **`T`（First）**: 読み込みに成功した場合はそのデータオブジェクト、失敗または中断した場合は`null`になります。
     * - **`LoadResult`（Second）**: 処理の詳細な成否・失敗理由を表す列挙型です。
     *
     * | 発生する状況 | 戻り値（`First` to `Second`） | 概要 |
     * | :--- | :--- | :--- |
     * | **正常に読み込み完了** | `T` to [LoadResult.SUCCESS] | リモートからの取得・解析にすべて成功した場合 |
     * | **SFTP非アクティブ** | `null` to [LoadResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない場合 |
     * | **プロファイル未選択** | `null` to [LoadResult.PROFILE_NOT_SELECTED] | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **ファイルが存在しない** | `null` to [LoadResult.FILE_NOT_FOUND] | リモート側に指定のファイルが存在しない場合（例外から判定） |
     * | **YAML構造が不正** | `null` to [LoadResult.INVALID_YAML] | ファイルの取得には成功したが、パース（解析）結果が`null`だった場合 |
     * | **その他ダウンロード失敗** | `null` to [LoadResult.FAILED] | 上記以外のネットワークエラーや予期せぬ致命的な例外が発生した場合 |
     *
     * @param T 読み込み対象のデータ型
     * @param fileName 読み込むファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param path リモート側の読み込み対象ファイルパス
     * @param load ダウンロード済みの一時ファイルとファイル名から、データオブジェクトを読み込む関数
     * @return 読み込まれたデータ（失敗時は`null`）と、処理の最終結果ステータス[LoadResult]のペア
     */
    private fun <T> loadCore(
        fileName: String,
        path: io.github.toumokorosi01.common.Path,
        load: (java.io.File, String?) -> T?
    ): Pair<T?, LoadResult> {
        if (!ssh.isSftpActive) return null to LoadResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return null to LoadResult.PROFILE_NOT_SELECTED
        val fullPath = Utility.getFullRemotePath(profile, path)

        val tempFile = kotlin.io.path.createTempFile("remote_load_", ".yml").toFile()

        return try {
            ssh.download(fullPath, tempFile.absolutePath)

            val data = load(tempFile, fileName)

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
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }


    /**
     * 指定された [editingData] を一時ファイルに書き出した後、リモートサーバーの指定されたファイル名へアップロードして保存します。
     *
     * この処理は、内部でOSの一時ディレクトリにテンポラリファイル（.yml）を作成してローカル保存を行い、
     * サーバーへのアップロードが完了した後に（例外発生時も含め）自動でその一時ファイルを確実に削除します。
     *
     * ### 戻り値（返り値）の条件:
     * 返り値は [SaveResult] 形式で返されます。
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :---: | :--- |
     * | **正常に保存完了** | [SaveResult.SUCCESS] | ローカルでの書き出し、サーバーへのアップロード、一時ファイル削除がすべて成功した場合 |
     * | **SFTP非アクティブ** | [SaveResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない、または現在アクティブな接続プロファイルが存在しない場合 |
     * | **書き出し・保存失敗** | [SaveResult.FAILED] | YAMLの生成失敗、ネットワークエラー、または権限不足など（例外発生時） |
     *
     * @param fileName 保存先となるリモートのファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param editingData 現在画面などで編集され、サーバーに保存したい対象の最新データ。
     * @return 保存処理の結果ステータスを表す [SaveResult]
     */
    fun save(fileName: String, editingData: ItemData): SaveResult {
        // 1. 接続状態のチェック
        if (!ssh.isSftpActive) return SaveResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return SaveResult.SFTP_INACTIVE

        val fullPath = Utility.getFullRemotePath(profile, Dir.Item.Stats.File(fileName))
        var tempFile: java.io.File? = null

        return try {
            // 2. 一時ファイルを作成
            tempFile = kotlin.io.path.createTempFile("remote_save_", ".yml").toFile()

            // 3. Repository.save(File) を使って一時ファイルにデータを書き出す
            ItemManager.save(tempFile, editingData)

            // 4. 書き出された一時ファイルをサーバーへアップロード
            ssh.upload(tempFile.absolutePath, fullPath)

            SaveResult.SUCCESS
        } catch (e: Exception) {
            logger.error("リモート保存失敗: $fullPath", e)
            SaveResult.FAILED
        } finally {
            // 5. アップロードの成否に関わらず、一時ファイルが作成されていれば確実に削除する
            tempFile?.delete()
        }
    }


    fun save(fileName: String, editingData: OreData): SaveResult {
        return saveCore(
            fileName = fileName,
            path = Dir.Ore.Ores.File(fileName),
            data = editingData,
            save = OreManager::save
        )
    }

    fun save(fileName: String, editingData: MobData): SaveResult {
        return saveCore(
            fileName = fileName,
            path = Dir.Mob.Mobs.File(fileName),
            data = editingData,
            save = MobManager::save
        )
    }

    private fun <T> saveCore(
        fileName: String,
        path: io.github.toumokorosi01.common.Path,
        data: T,
        save: (java.io.File, T) -> Unit
    ): SaveResult {
        if (!ssh.isSftpActive) return SaveResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return SaveResult.SFTP_INACTIVE

        val fullPath = Utility.getFullRemotePath(profile, path)
        var tempFile: java.io.File? = null

        return try {
            tempFile = kotlin.io.path.createTempFile("remote_save_", ".yml").toFile()
            save(tempFile, data)
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
     * 指定されたファイル名のリモート設定ファイルをサーバーから削除します。
     *
     * リモートファイルが存在しない場合は、削除時の例外メッセージ（No such file 等）から
     * 自動的に判別して適切なステータスを返します。
     *
     * ### 戻り値（返り値）の条件:
     * 返り値は [DeleteResult] 形式で返されます。
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :---: | :--- |
     * | **正常に削除完了** | [DeleteResult.SUCCESS] | リモートサーバーからのファイル削除が正常に完了した場合 |
     * | **SFTP非アクティブ** | [DeleteResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない場合 |
     * | **プロファイル未選択** | [DeleteResult.PROFILE_NOT_SELECTED] | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **ファイルが存在しない** | [DeleteResult.FILE_NOT_FOUND] | 削除対象のファイルがサーバー上に最初から存在しない場合（例外から判定） |
     * | **その他削除失敗** | [DeleteResult.FAILED] | 権限不足、通信切断、または予期せぬ致命的な例外が発生した場合 |
     *
     * @param fileName 削除するファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return 削除処理の最終結果ステータスを表す [DeleteResult]
     */
    fun delete(fileName: String): DeleteResult {
        if (!ssh.isSftpActive) return DeleteResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return DeleteResult.PROFILE_NOT_SELECTED
        val fullPath = Utility.getFullRemotePath(profile, Dir.Item.Stats.File(fileName))

        return try {
            // 💡 リモートサーバーのファイルを物理削除 (プロジェクトのSFTPメソッド名に合わせてください)
            ssh.remove(fullPath)
            deleteLocalBackup(fileName)

            DeleteResult.SUCCESS
        } catch (e: Exception) {
            val msg = e.message ?: ""

            // 💡 例外メッセージから「ファイルが最初から存在しない」状態を特定する
            if (msg.contains("No such file", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
                logger.warn("削除対象のリモートファイルが存在しません: $fullPath")
                DeleteResult.FILE_NOT_FOUND
            } else {
                // 権限エラー(Permission denied)や接続切れなど
                logger.error("リモートファイルの削除中に致命的な例外が発生: $fullPath", e)
                DeleteResult.FAILED
            }
        }
    }

    /**
     * 指定されたリモート設定ファイルの名称（ID）をサーバー側でリネームし、
     * 同時にローカルに保持されている新旧ペアの自動保存バックアップファイル名も新しい名称へと追従させます。
     *
     * リネームが正常に完了した場合、連動してローカルの一時ディレクトリ内にある
     * 変更前の古い自動保存バックアップファイル（新旧ペア）も安全に新しい名称へとリネームされます。
     *
     * 変更先のファイル名が既に存在する場合などは、リネーム時の例外メッセージ（Failure等）から
     * 自動的に判別して適切なステータスを返します。
     *
     * ### 戻り値（返り値）の条件:
     * 返り値は [RenameResult] 形式で返されます。
     *
     * | 発生する状況 | 戻り値 | 概要 |
     * | :--- | :---: | :--- |
     * | **正常にリネーム完了** | [RenameResult.SUCCESS] | サーバー上での名前変更、およびローカルバックアップの追従がすべて成功した場合 |
     * | **SFTP非アクティブ** | [RenameResult.SFTP_INACTIVE] | SSH/SFTPセッションが有効でない場合 |
     * | **プロファイル未選択** | [RenameResult.PROFILE_NOT_SELECTED] | 現在アクティブな接続プロファイルが存在しない場合 |
     * | **変更前ファイルが存在しない** | [RenameResult.FILE_NOT_FOUND] | リネーム元（変更前）のファイルがサーバー上に最初から存在しない場合（例外から判定） |
     * | **変更後ファイルが既に存在** | [RenameResult.ALREADY_EXISTS] | 変更先（新しい名前）のファイルがサーバー上に既に存在し、上書きが拒否された場合（例外から判定） |
     * | **その他リネーム失敗** | [RenameResult.FAILED] | 権限不足、通信切断、または予期せぬ致命的な例外が発生した場合 |
     *
     * @param oldName 変更前のファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @param newName 変更後の新しいファイル名。拡張子（.ymlなど）は除いた名前を指定します。
     * @return リネーム処理の最終結果ステータスを表す [RenameResult]
     */
    fun rename(oldName: String, newName: String): RenameResult {
        if (!ssh.isSftpActive) return RenameResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return RenameResult.PROFILE_NOT_SELECTED

        val oldFullPath = Utility.getFullRemotePath(profile, Dir.Item.Stats.File(oldName))
        val newFullPath = Utility.getFullRemotePath(profile, Dir.Item.Stats.File(newName))

        return try {
            // 1. サーバー側（リモート）のファイル名を変更
            ssh.rename(oldFullPath, newFullPath)

            // 2. ローカルバックアップ（editing と original の両ペア）も追従して改名する
            val profileName = profile.name
            val itemsDir = FilePath.AUTOSAVE_DIR.toFile().resolve(profileName).resolve("items")

            listOf("editing", "original").forEach { subDir ->
                val oldLocalFile = itemsDir.resolve(subDir).resolve("$oldName.yml")

                if (oldLocalFile.exists()) {
                    val newLocalFile = itemsDir.resolve(subDir).resolve("$newName.yml")

                    // 💡 ファイルの中身をロードして、内部のIDを新しい名前に書き換える
                    val localData = ItemManager.load(oldLocalFile)
                    if (localData != null) {
                        localData.id = newName // 内部要素のIDを統一！

                        // 新しい名前のファイルとして保存
                        newLocalFile.parentFile?.mkdirs()
                        ItemManager.save(newLocalFile, localData)

                        // 保存できたら古いファイルは削除
                        oldLocalFile.delete()
                        logger.info("【バックアップ内部ID更新】ローカルキャッシュ[$subDir]の内部IDを同期しました: $oldName -> $newName")
                    } else {
                        // 万が一パースに失敗した場合はリネームして逃がす
                        oldLocalFile.renameTo(newLocalFile)
                    }
                }
            }

            RenameResult.SUCCESS
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                // 例外メッセージから「ファイルが存在しない」状態を特定
                msg.contains("No such file", ignoreCase = true) || msg.contains("not found", ignoreCase = true) -> {
                    logger.warn("リネーム対象のリモートファイルが存在しません: $oldFullPath")
                    RenameResult.FILE_NOT_FOUND
                }
                // 重複やサーバー拒否（SSHJでは主にFailureを包含する広範なメッセージになるケースをケア）
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
    fun deleteOre(fileName: String): DeleteResult {
        return deleteCore(fileName, Dir.Ore.Ores.File(fileName), ::deleteOreLocalBackup)
    }

    fun deleteMob(fileName: String): DeleteResult {
        return deleteCore(fileName, Dir.Mob.Mobs.File(fileName), ::deleteMobLocalBackup)
    }

    private fun deleteCore(
        fileName: String,
        path: io.github.toumokorosi01.common.Path,
        deleteBackup: (String) -> Unit
    ): DeleteResult {
        if (!ssh.isSftpActive) return DeleteResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return DeleteResult.PROFILE_NOT_SELECTED
        val fullPath = Utility.getFullRemotePath(profile, path)

        return try {
            ssh.remove(fullPath)
            deleteBackup(fileName)
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

    fun renameOre(oldName: String, newName: String): RenameResult {
        return renameCore(
            oldName = oldName,
            newName = newName,
            oldPath = Dir.Ore.Ores.File(oldName),
            newPath = Dir.Ore.Ores.File(newName),
            categoryDirName = "ores",
            load = OreManager::load,
            save = { file, data -> OreManager.save(file, data.copy(id = newName)) }
        )
    }

    fun renameMob(oldName: String, newName: String): RenameResult {
        return renameCore(
            oldName = oldName,
            newName = newName,
            oldPath = Dir.Mob.Mobs.File(oldName),
            newPath = Dir.Mob.Mobs.File(newName),
            categoryDirName = "mobs",
            load = MobManager::load,
            save = { file, data -> MobManager.save(file, data.copy(id = newName)) }
        )
    }

    private fun <T> renameCore(
        oldName: String,
        newName: String,
        oldPath: io.github.toumokorosi01.common.Path,
        newPath: io.github.toumokorosi01.common.Path,
        categoryDirName: String,
        load: (java.io.File, String?) -> T?,
        save: (java.io.File, T) -> Unit
    ): RenameResult {
        if (!ssh.isSftpActive) return RenameResult.SFTP_INACTIVE
        val profile = ssh.currentProfile ?: return RenameResult.PROFILE_NOT_SELECTED

        val oldFullPath = Utility.getFullRemotePath(profile, oldPath)
        val newFullPath = Utility.getFullRemotePath(profile, newPath)

        return try {
            ssh.rename(oldFullPath, newFullPath)

            listOf("editing", "original").forEach { subDir ->
                val oldLocalFile = localBackupFile(categoryDirName, subDir, oldName)

                if (oldLocalFile.exists()) {
                    val newLocalFile = localBackupFile(categoryDirName, subDir, newName)
                    val localData = load(oldLocalFile, null)

                    if (localData != null) {
                        newLocalFile.parentFile?.mkdirs()
                        save(newLocalFile, localData)
                        oldLocalFile.delete()
                        logger.info("【バックアップ内部ID更新】ローカルキャッシュ[$categoryDirName/$subDir]の内部IDを同期しました: $oldName -> $newName")
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