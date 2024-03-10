package com.github.unlocomqx.validator.toolWindow.helpers

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.testFramework.utils.vfs.getPsiFile
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FileHelper {
    companion object {
        fun navigateToFile(path: String) {
            val project = ProjectManager.getInstance().openProjects[0]
            val fullPath: Path = Path.of(project.basePath, path)
            LocalFileSystem.getInstance().findFileByPath(fullPath.absolutePathString())?.findPsiFile(project)?.navigate(true)
        }
    }

}
