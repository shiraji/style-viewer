package com.github.shiraji.styleviewer.view

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class StyleViewerToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val panel = StyleViewerPanel(project)
        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
        Disposer.register(project, panel)
    }
}