package com.github.shiraji.styleviewer.data

data class Style(val name: String,
                 val filePath: String,
                 val parent: String?,
                 val values: List<StyleValue>)

data class StyleValue(val name: String, val value: String)