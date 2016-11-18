package com.github.shiraji.styleviewer.view

import com.github.shiraji.styleviewer.data.Style
import com.github.shiraji.styleviewer.data.StyleValue
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
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

    private val styleMap: MutableMap<String, MutableList<Style>> = linkedMapOf()

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
        ApplicationManager.getApplication().invokeLater {
            refreshListModel()
        }

        val detailPanel = StyleViewerDetailPanel()
        detailPanel.rootPanel.isVisible = false

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val list = JBList(listModel).apply {
            fixedCellHeight = 48
            ListSpeedSearch(this)
            addListSelectionListener {
                val addedStyle = mutableListOf<String>()

                panel.removeAll()
                val name = selectedValue as? String
                styleMap[name]?.addDetailToPanel(name!!, addedStyle, panel)
                panel.revalidate()
            }
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        val scrollPane = ScrollPaneFactory.createScrollPane(list)
        val scrollPane2 = ScrollPaneFactory.createScrollPane(panel)
        return JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, scrollPane2)
    }

    private fun MutableList<Style>.addDetailToPanel(name: String, addedStyle: MutableList<String>, panel: JPanel) {
        // ignore the style that is already added
        // avoid infinite loop
        if (addedStyle.contains(name)) return
        addedStyle.add(name)

        val detailPanel = StyleViewerDetailPanel()
        detailPanel.styleName.text = name

        val tableModel = DefaultTableModel()

        forEach {
            if (it.hasVersion()) {
                tableModel.addColumn("key (${it.version})")
            } else {
                tableModel.addColumn("key")
            }
            tableModel.addColumn("value")


            it.values.forEach {
                tableModel.insertRow(0, arrayOf(it.name, it.value))
            }
        }


        detailPanel.valueTable.model = tableModel
        panel.add(detailPanel.rootPanel)

        // inherit by name
        styleMap[name.substringBeforeLast(".")]?.addDetailToPanel(addedStyle, panel)

        // inherit by parent tag
        styleMap[parent]?.addDetailToPanel(addedStyle, panel)
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
            addToStyleMap(PsiManager.getInstance(project), it)
        }
    }

    private fun addToStyleMap(psiManager: PsiManager, virtualFile: VirtualFile) {
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
                val styles = styleMap[name] ?: mutableListOf<Style>()

                val version = try {
                    println(xmlFile.name.substringAfter("-"))

                    TODO("ここ確認")
                    xmlFile.name.substringAfter("-").toInt()
                } catch (e: NumberFormatException) {
                    0
                }

                val style = Style(name = name,
                        parent = styleTag.getAttribute("parent")?.value,
                        tag = styleTag,
                        version = version,
                        values = values)
                styles.add(style)
                styleMap.put(name, styles)
            }
        }
    }


    override fun dispose() {
    }
}