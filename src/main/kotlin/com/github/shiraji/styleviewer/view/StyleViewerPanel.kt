package com.github.shiraji.styleviewer.view

import com.github.shiraji.styleviewer.data.Style
import com.github.shiraji.styleviewer.data.StyleValue
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.util.Alarm
import java.awt.Cursor
import javax.swing.*
import javax.swing.table.DefaultTableModel

class StyleViewerPanel(val project: Project) : SimpleToolWindowPanel(true, true), DataProvider, Disposable {

    private val listModel: DefaultListModel<String> = DefaultListModel()

    private val alarm = Alarm(Alarm.ThreadToUse.SHARED_THREAD)

    private val styleMap: MutableMap<String, Style> = linkedMapOf()

    init {
//        setToolbar(createToolbarPanel())
        setContent(createContentPanel())
    }

//    private fun createToolbarPanel(): JComponent? {
//        val group = DefaultActionGroup()
//        group.add(RefreshAction())
//        group.add(FilterAction())
//        group.add(SortAscAction())
//        val actionToolBar = ActionManager.getInstance().createActionToolbar("Style", group, true)
//        return JBUI.Panels.simplePanel(actionToolBar.component)
//    }

    private fun createContentPanel(): JComponent {
        refreshListModel()
        val detailPanel = StyleViewerDetailPanel()
        detailPanel.rootPanel.isVisible = false

        val detailPanel2 = StyleViewerDetailPanel()
        detailPanel2.rootPanel.isVisible = false


        val list = JBList(listModel).apply {
            fixedCellHeight = 48
            ListSpeedSearch(this)
            addListSelectionListener {
                val name = selectedValue as? String
                val style = styleMap[name]
                if (style == null) {
                    detailPanel.rootPanel.isVisible = false
                    detailPanel.styleName.text = "Choose style to see detail"
                    detailPanel2.rootPanel.isVisible = false
                    detailPanel2.styleName.text = "Choose style to see detail"
                } else {
                    detailPanel.rootPanel.isVisible = true
                    detailPanel.styleName.text = name
                    detailPanel2.rootPanel.isVisible = true
                    detailPanel2.styleName.text = name

                    val tableModel = DefaultTableModel(arrayOf("name", "value"), 0)
                    style.values.forEach {
                        tableModel.insertRow(0, arrayOf(it.name, it.value))
                    }
                    detailPanel.valueTable.model = tableModel
                    detailPanel2.valueTable.model = tableModel
                }
            }
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        val scrollPane = ScrollPaneFactory.createScrollPane(list)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(detailPanel.rootPanel)
        panel.add(detailPanel2.rootPanel)

        val scrollPane2 = ScrollPaneFactory.createScrollPane(panel)
        return JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, scrollPane2)
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
        val keys = styleMap.keys //if (sortAsc) styleMap.keys.sorted() else styleMap.keys
        keys.forEach {
            listModel.addElement(it)
        }
    }

    private fun resetStyleMap() {
        styleMap.clear()
        initStyleMap()
    }

    private fun initStyleMap() {
        FileTypeIndex.getFiles(XmlFileType.INSTANCE, ProjectScope.getAllScope(project)).forEach {
            addToStyleMap(PsiManager.getInstance(project), it, true, false)
        }
    }

    private fun addToStyleMap(psiManager: PsiManager, virtualFile: VirtualFile, isInProject: Boolean, isInAndroidSdk: Boolean) {
        val xmlFile = psiManager.findFile(virtualFile) as? XmlFile ?: return
        xmlFile.rootTag?.findSubTags("style")?.forEach {
            styleTag ->
            val values = mutableListOf<StyleValue>()
            styleTag.findSubTags("item").forEach {
                itemTag ->
                itemTag.getAttribute("name")?.value?.let {
                    values.add(StyleValue(it, itemTag.value.text))
                }
            }

            styleTag.getAttribute("name")?.value?.let {
                name ->
                val style = Style(name = name,
                        parent = styleTag.getAttribute("parent")?.value,
                        filePath = xmlFile.name,
                        values = values)
                styleMap.put(name, style)
            }
        }
    }


    override fun dispose() {
    }
}