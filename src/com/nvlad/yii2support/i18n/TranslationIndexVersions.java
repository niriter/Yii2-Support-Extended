package com.nvlad.yii2support.i18n;

/**
 * Persistent schema versions for the i18n file-based indexes.
 *
 * <p>Versions are independent and monotonically increasing. Increment the affected version in
 * the same change whenever unchanged source files could produce different indexed data, including
 * changes to:</p>
 * <ul>
 *     <li>input filters or provider/call recognition;</li>
 *     <li>category or message extraction, normalization, interpolation handling, or unescaping;</li>
 *     <li>index key/value composition, descriptors, or externalizers.</li>
 * </ul>
 *
 * <p>Never reset or reuse a published version. Keep a short reason in the history below. A bump
 * deliberately invalidates persisted data and makes the IDE rebuild that index.</p>
 */
final class TranslationIndexVersions {
    // 1: Initial category/message scalar-key schema.
    // 2: Provider-path filtering and recursive TranslationProvider resolution.
    // 3: Restrict provider resolution to a top-level return statement.
    // 4: Add category keys for indexed completion provider discovery.
    static final int KEYS = 4;

    // 1: Initial category-to-UTF-8-message-set schema for literal Yii::t() calls.
    static final int USAGES = 1;

    private TranslationIndexVersions() {
    }
}
