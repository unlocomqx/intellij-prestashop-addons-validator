package com.github.unlocomqx.validator.toolWindow.helpers

import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorLine
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FileHelper {
    companion object {
        fun navigateToFile(userObject: Any) {
            var path = ""
            var line = 0
            var column = 0

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
            val fullPath: Path = Path.of(project.basePath, path)

            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath.absolutePathString()) ?: return

            OpenFileDescriptor(project, virtualFile, line, column).navigate(true)
        }
    }

}
