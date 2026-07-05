package dev.tinnci.kanadojo

internal val Script.displayNameResId: Int
    get() = when (this) {
        Script.Hiragana -> R.string.script_hiragana
        Script.Katakana -> R.string.script_katakana
    }

internal val Script.shortNameResId: Int
    get() = when (this) {
        Script.Hiragana -> R.string.script_hiragana_short
        Script.Katakana -> R.string.script_katakana_short
    }
