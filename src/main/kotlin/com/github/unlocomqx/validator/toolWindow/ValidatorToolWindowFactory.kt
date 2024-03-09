package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.cef.network.CefURLRequest

class ValidatorToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().setLevel(LogLevel.DEBUG)
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val newToolWindow = ValidatorToolWindow.getInstance(toolWindow)
        val content = ContentFactory.getInstance().createContent(newToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ValidatorToolWindow(toolWindow: ToolWindow) {

        companion object {
             lateinit var instance: ValidatorToolWindow
            fun getInstance(toolWindow: ToolWindow): ValidatorToolWindow {
                if (!::instance.isInitialized) {
                    instance = ValidatorToolWindow(toolWindow)
                }
                return instance
            }
        }

        val browserPanel = JBPanel<JBPanel<*>>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {

//            browserPanel.add(JBPanel<JBPanel<*>>().apply {
//                add(JBLabel(LocaleBundle.message("start_upload")))
//            })

            val cefApp = JBCefApp.getInstance()
            val cefClient = cefApp.createClient()
            val browser = JBCefBrowserBuilder()
                .setClient(cefClient)
                .setUrl(LocaleBundle.message("validator_url"))
                .build()
            cefClient.addRequestHandler(AssetRequestHandler(), browser.cefBrowser)
            browserPanel.add(browser.component)
            add(browserPanel)
        }

        fun hideBrowser() {
            browserPanel.isVisible = false
        }
    }

    class AssetRequestHandler() : CefRequestHandlerAdapter() {
        override fun onBeforeBrowse(
            browser: CefBrowser?,
            frame: CefFrame?,
            request: CefRequest?,
            user_gesture: Boolean,
            is_redirect: Boolean
        ): Boolean {
            return false
        }

        override fun onOpenURLFromTab(
            browser: CefBrowser?,
            frame: CefFrame?,
            target_url: String?,
            user_gesture: Boolean
        ): Boolean {
            return true
        }

        override fun getResourceRequestHandler(
            browser: CefBrowser?,
            frame: CefFrame?,
            request: CefRequest?,
            isNavigation: Boolean,
            isDownload: Boolean,
            requestInitiator: String?,
            disableDefaultHandling: BoolRef?
        ): CefResourceRequestHandler? {
            return ResourceRequestHandler()
        }
    }

    class ResourceRequestHandler : CefResourceRequestHandlerAdapter() {
        override fun onResourceLoadComplete(
            browser: CefBrowser?,
            frame: CefFrame?,
            request: CefRequest?,
            response: CefResponse?,
            status: CefURLRequest.Status?,
            receivedContentLength: Long
        ) {
//            thisLogger().warn("Resource load complete ${request?.url}")
            val isValidateReq = ReqMatcher.matchValidateReq(request?.url)
            if (isValidateReq) {
                thisLogger().warn("Validate request detected")
                ValidatorToolWindow.instance.hideBrowser()
            }
            super.onResourceLoadComplete(browser, frame, request, response, status, receivedContentLength)
        }
    }

}
