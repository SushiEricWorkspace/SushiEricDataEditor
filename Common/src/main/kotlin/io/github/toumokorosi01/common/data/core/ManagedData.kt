package io.github.toumokorosi01.common.data.core

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError

/**
 * エディターで管理するデータの共通インターフェースです。
 *
 * #### 仕様:
 * - [id] に独自IDを保持します。
 * - [completed] に検証済みデータとして使用可能かどうかを保持します。
 * - [refreshCompleted] で検証結果に応じて [completed] を更新します。
 * - [DeepCopyable] を継承しているため、メモリ上での複製処理も実装必須です。
 *
 * @param T このデータ自身の型。
 * @param V このデータに対応するValidatorの型。
 */
interface ManagedData<T : ManagedData<T, V>, V : DataValidator> : DeepCopyable<T> {
    /** 独自ID */
    var id: String

    /** 完成品かどうか */
    var completed: Boolean

    /**
     * 検証エラーの有無をもとに [completed] を更新します。
     *
     * #### 仕様:
     * - [errors] が空の場合、[completed] を `true` にします。
     * - [errors] が1件以上ある場合、[completed] を `false` にします。
     *
     * @param errors 検証済みのエラー一覧。
     * @return 引数で受け取った [errors]。
     */
    fun refreshCompleted(errors: List<PropertyError>): List<PropertyError> {
        completed = errors.isEmpty()
        return errors
    }
}

/**
 * メモリ上で安全な複製を作成できることを表すインターフェースです。
 *
 * #### 仕様:
 * - ファイルI/Oを発生させず、現在のインスタンスの複製を返します。
 * - `MutableList`、`MutableMap`、ネストされた可変データなどを持つ場合は、
 *   元データとコピー先で同じ参照を共有しないように実装します。
 *
 * @param T 複製後に返す型。
 */
interface DeepCopyable<T> {
    /**
     * 現在のインスタンスをメモリ上で複製します。
     *
     * @return 元データとは独立して編集できる複製。
     */
    fun deepCopy(): T
}