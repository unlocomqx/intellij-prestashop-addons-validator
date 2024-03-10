package com.github.unlocomqx.validator.toolWindow.CellRenderer

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.codehaus.jettison.json.JSONObject
import java.awt.Color
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
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
            append("${jsonObject.getString("message")} - ")
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
}
