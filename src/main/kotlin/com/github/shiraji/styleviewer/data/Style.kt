package com.github.shiraji.styleviewer.data

data class Style(val name: String,
                 val filePath: String,
                 val parent: String?,
                 val values: Map<String, String>)