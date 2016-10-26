package com.github.shiraji.styleviewer.view

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.xml.XmlFile
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JLabel

class StyleViewerPanel(val project: Project) : SimpleToolWindowPanel(true, true), DataProvider, Disposable {

    private val listModel: DefaultListModel<String> = DefaultListModel()

    private val alarm = Alarm(Alarm.ThreadToUse.SHARED_THREAD)

    private val styleMap: MutableMap<String, String?> = linkedMapOf()

    init {
//        setToolbar(createToolbarPanel())
        setContent(createContentPanel())
    }

    private fun createToolbarPanel(): JComponent? {
        val group = DefaultActionGroup()
//        group.add(RefreshAction())
//        group.add(FilterAction())
//        group.add(SortAscAction())
        val actionToolBar = ActionManager.getInstance().createActionToolbar("Style", group, true)
        return JBUI.Panels.simplePanel(actionToolBar.component)
    }

    private fun createContentPanel(): JComponent {
        refreshListModel()

        println(styleMap)

        return JLabel("FOO")
    }

    private fun refreshListModel() {
        try {
            setWaitCursor()
            resetStyleMap()
            reloadListModel()
        } finally {
            restoreCursor()
        }
    }

    private fun setWaitCursor() {
        alarm.addRequest({ cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) }, 100)
    }

    private fun restoreCursor() {
        alarm.cancelAllRequests()
        cursor = Cursor.getDefaultCursor()
    }

    private fun reloadListModel() {
        listModel.removeAllElements()
        val keys = styleMap.keys.sorted() //if (sortAsc) styleMap.keys.sorted() else styleMap.keys
        keys.forEach {
            listModel.addElement(it)
        }
    }

    private fun resetStyleMap() {
        styleMap.clear()
        initStyleMap()
    }

    private fun initStyleMap() {
        val psiManager = PsiManager.getInstance(project)

        FileTypeIndex.getFiles(XmlFileType.INSTANCE, ProjectScope.getAllScope(project)).forEach {
            addToStyleMap(psiManager, it, true, false)
        }

//        if (!filterLibRes) {
//            FileTypeIndex.getFiles(XmlFileType.INSTANCE, ProjectScope.getLibrariesScope(project)).forEach {
//                addToStyleMap(psiManager, it, isInProject = false, isInAndroidSdk = it.path.contains(androidSdkPathRegex))
//            }
//        }
//
//        generateColors()
    }

    private fun addToStyleMap(psiManager: PsiManager, virtualFile: VirtualFile, isInProject: Boolean, isInAndroidSdk: Boolean) {
//        if (FILTER_XML.contains(virtualFile.name)) return
        val xmlFile = psiManager.findFile(virtualFile) as? XmlFile ?: return
        xmlFile.rootTag?.findSubTags("style")?.forEach {

            it.getAttribute("name")?.value?.let {
                name ->
                val parent = it.getAttribute("parent")?.value
                styleMap.put(name, parent)
            }


//            styleMap.put("R.color.${it.getAttribute("name")?.value}",
//                    ColorManagerColorTag(
//                            tag = it,
//                            fileName = virtualFile.name,
//                            isInProject = isInProject,
//                            isInAndroidSdk = isInAndroidSdk))
        }
    }


    override fun dispose() {
    }
}