package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
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
import java.awt.Font
import javax.swing.BoxLayout

class ValidatorToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().setLevel(LogLevel.DEBUG)
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

        private val results = emptyMap<String, String>()

        private val browserPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JBPanel.CENTER_ALIGNMENT
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val cefApp = JBCefApp.getInstance()
            val cefClient = cefApp.createClient()
            val browser = JBCefBrowserBuilder()
                .setClient(cefClient)
                .setUrl(LocaleBundle.message("validator_url"))
                .build()
            cefClient.addRequestHandler(AssetRequestHandler(), browser.cefBrowser)
            browserPanel.apply {
                add(JBLabel().apply {
                    text = LocaleBundle.message("start_upload")
                    alignmentX = JBLabel.CENTER_ALIGNMENT
                    font = font.deriveFont(Font.BOLD)
                })
                add(browser.component)
            }
            add(browserPanel)
        }

        fun hideBrowser() {
            browserPanel.isVisible = false
        }

        fun addResult(name: String, result: String) {
            results.plus(Pair(name, result))
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
            if (request?.url?.contains("/validate") == false) {
                return
            }

            if (ReqMatcher.matchValidateReq(request?.url)) {
                thisLogger().warn("Validate request detected")
                ValidatorToolWindow.instance.hideBrowser()
                return
            }

            val resultName = ReqMatcher.matchResultReq(request?.url)
            if (resultName != null) {
                thisLogger().warn("Upload request detected")
                // read content of response
                if (status != CefURLRequest.Status.UR_SUCCESS) {
                    ValidatorToolWindow.instance.addResult(resultName, "Error: $status")
                    return
                }
                // this is incorrect
                ValidatorToolWindow.instance.addResult(resultName, response.toString())
                return
            }

            super.onResourceLoadComplete(browser, frame, request, response, status, receivedContentLength)
        }
    }

}
