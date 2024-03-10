package com.github.unlocomqx.validator.toolWindow.CellRenderer

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.utils.vfs.getFile
import com.intellij.testFramework.utils.vfs.getPsiFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

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
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(path)
            return virtualFile?.getPsiFile(ProjectManager.getInstance().openProjects.get(0))?.getIcon(0) ?: AllIcons.FileTypes.Any_type
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
            append(userObject.path, SimpleTextAttributes.LINK_ATTRIBUTES)
            icon = userObject.icon
        }
    }
}
