package com.github.unlocomqx.validator.toolWindow.CellRenderer

import com.github.unlocomqx.validator.toolWindow.helpers.FileHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import org.codehaus.jettison.json.JSONObject
import java.awt.Component
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import kotlin.io.path.absolutePathString

class ValidatorSection(var label: String, var state: String) {
    val icon: Icon
        get() {
            return when (state) {
                "success" -> AllIcons.General.InspectionsOK
                "error" -> AllIcons.General.Error
                "warning" -> AllIcons.General.Warning
                "loading" -> AllIcons.General.InlineRefresh
                else -> AllIcons.Toolwindows.ToolWindowProblemsEmpty
            }
        }
}

class ValidatorMessage(var message: String, var type: String) {
    val icon: Icon
        get() {
            return when (type) {
                "error" -> AllIcons.General.Error
                "warning" -> AllIcons.General.Warning
                else -> AllIcons.General.Information
            }
        }
}

class ValidatorFile(var path: String) : ValidatorItemWithVirtualFile {
    val icon: Icon
        get() {
            return FileTypeManager.getInstance().getFileTypeByFileName(path).icon
        }

    override val virtualFile: VirtualFile?
        get() {
            val fullPath: Path = Path.of(ProjectManager.getInstance().openProjects[0].basePath, path)
            return LocalFileSystem.getInstance().findFileByPath(fullPath.absolutePathString())
        }
}

interface ValidatorItemWithVirtualFile {
    val virtualFile: VirtualFile?
}

class ValidatorLine(var jsonObject: JSONObject) : ValidatorItemWithVirtualFile {
    val icon: Icon
        get() {
            return when (jsonObject.get("type")) {
                "error" -> AllIcons.General.Error
                "warning" -> AllIcons.General.Warning
                else -> AllIcons.General.Information
            }
        }

    override val virtualFile: VirtualFile?
        get() {
            val fullPath: Path =
                Path.of(ProjectManager.getInstance().openProjects[0].basePath, jsonObject.getString("file"))
            return LocalFileSystem.getInstance().findFileByPath(fullPath.absolutePathString())
        }
}

class ValidatorDefaultTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val treeNode = value as DefaultMutableTreeNode
        val userObject = treeNode.userObject
        if (userObject is String) {
            backgroundNonSelectionColor = JBColor.background()
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        }

        if (userObject is ValidatorSection) {
            val label = userObject.label
            val icon = userObject.icon
            val component = super.getTreeCellRendererComponent(tree, label, sel, expanded, leaf, row, hasFocus)
            setIcon(icon)
            backgroundNonSelectionColor = JBColor.background()
            return component
        }

        if (userObject is ValidatorMessage) {
            val message = userObject.message
            val icon = userObject.icon
            val component = super.getTreeCellRendererComponent(tree, message, sel, expanded, leaf, row, hasFocus)
            setIcon(icon)
            return component
        }

        if (userObject is ValidatorFile) {
            val path = userObject.path
            val icon = userObject.icon
            val component = JBLabel(path).apply {
                toolTipText = path
                text = "<html><a href=\"file://${path}\">$path</a></html>"
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        FileHelper.navigateToFile(userObject)
                    }
                })
            }
            setIcon(icon)
            return component
        }

        if (userObject is ValidatorLine) {
            val jsonObject = userObject.jsonObject
            val path = jsonObject.getString("file")
            val line = jsonObject.getInt("line")
            val message = jsonObject.getString("message")
            val messageWithLinks = replaceHintsWithLinks(message)
            val icon = userObject.icon
            val component = JBPanel<JBPanel<*>>().apply {
                layout = FlowLayout(FlowLayout.LEFT)
                add(JBLabel("$path:$line $message").apply {
                    toolTipText = path
                    // apply link style
                    text = "<html>$messageWithLinks</html>"
                })
                add(JBLabel().apply {
                    text = "<html><a href=\"#navigate/$path:$line\">$path:$line</a></html>"
                    cursor = java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            FileHelper.navigateToFile(userObject)
                        }
                    })
                })
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        FileHelper.navigateToFile(userObject)
                    }
                })
            }
            setIcon(icon)
            return component
        }

        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
    }

    private fun replaceHintsWithLinks(message: String?): String {
        if (message == null) {
            return ""
        }

        var messageWithLinks: String = message
        // match words containing an underscore or more
        val matches = Regex("\\w+").findAll(message)
        for (match in matches) {
            // if contains underscore, it's a link
            if (match.value.contains("_")) {
                val link = "https://cs.symfony.com/doc/rules/operator/${match.value}.html"
                messageWithLinks =
                    messageWithLinks.replace(
                        match.value,
                        "<a href=\"${link}\" target=\"_blank\">${match.value}</a>"
                    )
            }
        }
        return messageWithLinks
    }
}

class ValidatorTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val treeNode = value as DefaultMutableTreeNode
        val userObject = treeNode.userObject
        if (userObject is String) {
            append(userObject)
            return
        }

        if (userObject is ValidatorSection) {
            append(userObject.label)
            icon = userObject.icon
            return
        }

        if (userObject is ValidatorMessage) {
            append(userObject.message)
            icon = userObject.icon
        }

        if (userObject is ValidatorFile) {
            val virtualFile = userObject.virtualFile
            append(
                userObject.path,
                if (virtualFile != null) SimpleTextAttributes.LINK_ATTRIBUTES else SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES
            )
            icon = userObject.icon
            if (virtualFile != null) {
                setToolTipText(virtualFile.path)
            }
        }

        if (userObject is ValidatorLine) {
            val jsonObject = userObject.jsonObject
            val path = jsonObject.getString("file")
            val line = jsonObject.getInt("line")
            val virtualFile = userObject.virtualFile
            val message = jsonObject.getString("message")
            val messagesWithLinks = getReadMoreFragments(message)
            messagesWithLinks.forEach {
                if (it.contains("href")) {
                    appendHTML(it, SimpleTextAttributes.LINK_BOLD_ATTRIBUTES)
                } else {
                    append(it, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
            append(
                "$path:$line",
                if (virtualFile != null) SimpleTextAttributes.LINK_ATTRIBUTES else SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES
            )
            icon = userObject.icon
            if (virtualFile != null) {
                setToolTipText(virtualFile.path)
            }
        }
    }

    private fun getReadMoreFragments(message: String?): Array<String> {
        if (message == null) {
            return arrayOf("")
        }

        val matches = Regex("\\w+").findAll(message)
        val fragments = mutableListOf<String>()
        for (match in matches) {
            // if contains underscore, it's a link
            if (match.value.contains("_")) {
                val link = "https://cs.symfony.com/doc/rules/operator/${match.value}.html"
                fragments.add("<html><a href=\"${link}\" title=\"ABC\">${match.value}</a></html>&nbsp;")
            } else {
                fragments.add(match.value + " ")
            }
        }
        return fragments.toTypedArray()
    }
}
