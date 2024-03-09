package com.github.unlocomqx.validator.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.unlocomqx.validator.LocaleBundle
import com.intellij.ide.BrowserUtil
import com.intellij.ui.jcef.JBCefApp

@Service(Service.Level.PROJECT)
class ValidatorService(project: Project) {

    init {
        thisLogger().info(LocaleBundle.message("projectService", project.name))
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun openLoginScreen() {
        // ternary using JBCefApp.isSupported()

//        BrowserUtil.browse(LocaleBundle.message("validator_url"))
    }
}
