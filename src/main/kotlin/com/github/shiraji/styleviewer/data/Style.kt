package com.github.shiraji.styleviewer.data

import com.intellij.psi.xml.XmlTag

data class Style(val name: String,
                 val tag: XmlTag,
                 val version: Int = 0,
                 val parent: String?,
                 val values: List<StyleValue>) {
    fun hasVersion() = version > 0
}

data class StyleValue(val name: String, val value: String)