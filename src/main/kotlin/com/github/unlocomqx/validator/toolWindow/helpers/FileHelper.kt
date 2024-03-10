package com.github.unlocomqx.validator.toolWindow.helpers

import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorItemWithVirtualFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorLine
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager

class FileHelper {
    companion object {
        fun navigateToFile(userObject: ValidatorItemWithVirtualFile) {
            var path = ""
            var line = 0
            var column = 0

            val virtualFile = userObject.virtualFile ?: return

            if (userObject is ValidatorFile) {
                path = userObject.path
            }

            if (userObject is ValidatorLine) {
                path = userObject.jsonObject.getString("file")
                line = userObject.jsonObject.getInt("line") - 1
                column = userObject.jsonObject.getInt("column") - 1
            }

            if (path.isEmpty()) return

            val project = ProjectManager.getInstance().openProjects[0]

            OpenFileDescriptor(project, virtualFile, line, column).navigate(true)
        }
    }

}
