package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.CodeNodesBuilder
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.FilesNodesBuilder
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.codehaus.jettison.json.JSONObject
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.SwingConstants
import kotlin.reflect.full.createInstance


val sections = mapOf(
    "requirements" to "Requirements",
    "structure" to "Structure",
    "errors" to "Errors",
    "compatibility" to "Compatibility",
    "optimizations" to "Optimizations",
    "translations" to "Translations",
    "licences" to "Licences",
    "security" to "Security",
    "standards" to "Standards"
)

val treeNodesBuilders = mapOf(
    "requirements" to FilesNodesBuilder::class,
    "structure" to FilesNodesBuilder::class,
    "errors" to CodeNodesBuilder::class,
    "compatibility" to FilesNodesBuilder::class,
    "optimizations" to FilesNodesBuilder::class,
    "translations" to FilesNodesBuilder::class,
    "licences" to CodeNodesBuilder::class,
    "security" to FilesNodesBuilder::class,
    "standards" to CodeNodesBuilder::class
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

        private var results = mapOf<String, String>()

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

        private val myTabbedPane = JBTabbedPane(SwingConstants.LEFT).apply {
            alignmentX = JBTabbedPane.LEFT_ALIGNMENT
            sections.forEach { (_, value) ->
                addTab(value, JBPanel<JBPanel<*>>().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                })
            }
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
                add(myTabbedPane)
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
                    text = LocaleBundle.message("finish")
                    addActionListener {
                        browser.cefBrowser.loadURL(LocaleBundle.message("validator_url"))
                        showBrowser()
                        clearResults()
                        resetTabs()
                    }
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                })

                add(JButton().apply {
                    text = LocaleBundle.message("reload")
                    addActionListener {
                        resetTabs()
                        sections.forEach { (key) ->
                            val result = results[key]
                            if (result != null) {
                                addResult(key, result)
                            }
                        }
                    }
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                })

                add(JBScrollPane(myTabbedPane).apply {
                    isVisible = true
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    weighty = 1.0
                    gridx = 0
                })
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
            if (sections.keys.indexOf(name) == -1) {
                return
            }

            results = results + (name to result)

            myTabbedPane.setIconAt(
                sections.keys.indexOf(name),
                if (result == "[]") AllIcons.General.InspectionsOK else AllIcons.General.Error
            )

            if (result == "[]") {
                return
            }

            try {
                val sectionTab = myTabbedPane.getComponentAt(sections.keys.indexOf(name)) as JBPanel<*>
                val jsonObject = JSONObject(result)
                val builder = treeNodesBuilders[name]
                if (builder != null) {
                    val builderInstance = builder.createInstance()
                    val nodes = builderInstance.buildComponents(name, jsonObject)
                    nodes.forEach {
                        sectionTab.add(it)
                    }
                }
            } catch (e: Exception) {
                thisLogger().warn("Error while parsing result", e)
            }
        }

        private fun clearResults() {
            results = emptyMap<String, String>()
        }

        private fun resetTabs() {
            for (i in 0 until myTabbedPane.tabCount) {
                myTabbedPane.setIconAt(i, null)
                val sectionTab = myTabbedPane.getComponentAt(i) as JBPanel<*>
                sectionTab.removeAll()
            }
        }

        fun setState(state: String) {
//            myTabbedPane.setPaintBusy(state == "loading")
        }

        fun setSectionLoading(resultName: String, loading: Boolean) {
            if (sections.keys.indexOf(resultName) == -1) {
                return
            }
            myTabbedPane.setIconAt(
                sections.keys.indexOf(resultName),
                if (loading) AllIcons.General.InlineRefresh else AllIcons.General.Error
            )
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
            if (request?.url?.contains("/validate") == false) {
                return null
            }

            if (ReqMatcher.matchValidateReq(request?.url)) {
                ValidatorToolWindow.instance.hideBrowser()
                return null
            }

            ReqMatcher.matchResultReq(request?.url) ?: return null

            return ResourceRequestHandler()
        }

        class ResourceRequestHandler : CefResourceRequestHandlerAdapter() {
            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?
            ): CefResourceHandler {
                return ValidatorResultResourceHandler()
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

                    ValidatorToolWindow.instance.setState("loading")
                    ValidatorToolWindow.instance.setSectionLoading(resultName, true)

                    try {
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
                            ValidatorToolWindow.instance.setSectionLoading(resultName, false)
                        }
                    } catch (e: Exception) {
                        thisLogger().warn("Error while processing request", e)
                        ValidatorToolWindow.instance.setSectionLoading(resultName, false)
                        callback?.cancel()
                        return false
                    } finally {
                        ValidatorToolWindow.instance.setState("idle")
                    }
                    callback?.cancel()
                    return false
                }
            }
        }
    }
}
