package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.diff.comparison.expand
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
import com.intellij.ui.treeStructure.Tree
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import javax.swing.BoxLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


val sections = mapOf(
    "warmup" to "Warmup",
    "structure" to "Structure",
    "errors" to "Errors",
    "compatibility" to "Compatibility",
    "requirements" to "Requirements",
    "optimizations" to "Optimizations",
    "traslations" to "Translations",
    "licences" to "Licences",
    "security" to "Security",
    "standards" to "Standards"
)

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

        private var results = emptyMap<String, String>()

        private val browserPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JBPanel.CENTER_ALIGNMENT
        }

        private val resultsPanel = JBPanel<JBPanel<*>>().apply {
            isVisible = false
            layout = GridBagLayout().apply {
                alignmentY = JBPanel.TOP_ALIGNMENT
            }
            alignmentX = JBPanel.LEFT_ALIGNMENT
        }

        private val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("Results"))
        private val myTree: Tree = Tree().apply {
            model = treeModel
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JBPanel.CENTER_ALIGNMENT

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

            resultsPanel.apply {
                add(JButton().apply {
                    text = LocaleBundle.message("cancel")
                    addActionListener {
                        browser.cefBrowser.loadURL(LocaleBundle.message("validator_url"))
                        showBrowser()
                    }
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                })
                add(myTree, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                    fill = GridBagConstraints.HORIZONTAL
                    weightx = 1.0
                    weighty = 1.0
                    gridx = 0
                })

                sections.forEach { (key, value) ->
                    val sectionNode = DefaultMutableTreeNode(value)
                    treeModel.insertNodeInto(
                        sectionNode,
                        treeModel.root as DefaultMutableTreeNode,
                        treeModel.getChildCount(treeModel.root)
                    )
                }
            }
            add(resultsPanel)
        }

        fun hideBrowser() {
            browserPanel.isVisible = false
            resultsPanel.isVisible = true
        }

        private fun showBrowser() {
            browserPanel.isVisible = true
            resultsPanel.isVisible = false
        }

        fun addResult(name: String, result: String) {
            results.plus(Pair(name, result))
            val index = sections.keys.indexOf(name)
            val sectionNode = treeModel.getChild(treeModel.root, index) as DefaultMutableTreeNode
            val resultNode = DefaultMutableTreeNode(result)
            treeModel.insertNodeInto(resultNode, sectionNode, treeModel.getChildCount(sectionNode))
        }

        fun clearResults() {
            results = emptyMap<String, String>()
        }
    }

    class AssetRequestHandler() : CefRequestHandlerAdapter() {
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
        override fun getResourceHandler(
            browser: CefBrowser?,
            frame: CefFrame?,
            request: CefRequest?
        ): CefResourceHandler {
            if (request?.url?.contains("/validate") == false) {
                return super.getResourceHandler(browser, frame, request)
            }

            if (ReqMatcher.matchValidateReq(request?.url)) {
                ValidatorToolWindow.instance.hideBrowser()
                return super.getResourceHandler(browser, frame, request)
            }

            val resultName = ReqMatcher.matchResultReq(request?.url)
            if (resultName != null) {
                return ValidatorResultResourceHandler()
            }

            return super.getResourceHandler(browser, frame, request)
        }

        class ValidatorResultResourceHandler : CefResourceHandlerAdapter() {
            override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
                if (request?.url == null) {
                    callback?.cancel()
                    return false
                }

                val resultName = ReqMatcher.matchResultReq(request?.url)
                if (resultName == null) {
                    callback?.cancel()
                    return false
                }

                val url = URI(request.url).toURL()
                // make request to server
                val con: HttpURLConnection = url.openConnection() as HttpURLConnection
                con.requestMethod = "GET";
                con.setRequestProperty("Cookie", request.getHeaderByName("Cookie"))
                val responseCode = con.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    val reader = BufferedReader(InputStreamReader(con.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while ((reader.readLine().also { inputLine = it }) != null) {
                        response.append(inputLine)
                    }
                    reader.close()

                    // print result
                    ValidatorToolWindow.instance.addResult(resultName, response.toString())
                } else {
                    println("GET request did not work.")
                }
                callback?.cancel()
                return false
            }
        }
    }

}
