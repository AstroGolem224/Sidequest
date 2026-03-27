package com.astrogolem.sidequest.core.ui.text

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    companion object {
        fun resource(@StringRes resId: Int, vararg args: Any): UiText = Resource(
            resId = resId,
            args = args.toList(),
        )
    }
}

fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> context.getString(resId, *args.toTypedArray())
}
