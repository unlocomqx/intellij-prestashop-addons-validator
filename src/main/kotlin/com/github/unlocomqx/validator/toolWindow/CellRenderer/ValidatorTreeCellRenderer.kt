package com.github.unlocomqx.validator.toolWindow.CellRenderer

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ClickListener
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.rd.swing.mouseClicked
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.io.path.absolutePathString

class ValidatorMessage(var message: String, var type: String) {
    val icon: Icon
        get() {
            return when (type) {
                "error" -> AllIcons.General.Error
                "warning" -> AllIcons.General.Warning
                else -> AllIcons.General.Information
            }
        }

    val color: Color
        get() {
            return when (type) {
                "error" -> Color.red
                "warning" -> Color.orange
                else -> Color.green
            }
        }
}

class ValidatorFile(var path: String) {
    val icon: Icon
        get() {
            return FileTypeManager.getInstance().getFileTypeByFileName(path).icon
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

        if (userObject is ValidatorMessage) {
            append(userObject.message)
            icon = userObject.icon
        }

        if (userObject is ValidatorFile) {
            val fullPath: Path = Path.of(ProjectManager.getInstance().openProjects[0].basePath, userObject.path)
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath.absolutePathString())

            append(
               userObject.path,
                if (virtualFile != null) SimpleTextAttributes.LINK_ATTRIBUTES else SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES
            )
            icon = userObject.icon
            if (virtualFile != null) {
                setToolTipText(virtualFile.path)
            }
        }
    }
}
