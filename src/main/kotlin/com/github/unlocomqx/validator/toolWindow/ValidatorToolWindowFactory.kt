package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorDefaultTreeCellRenderer
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorSection
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.CodeNodesBuilder
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.FilesNodesBuilder
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
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
import javax.swing.tree.DefaultMutableTreeNode
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

        private val treeModel: TreeTableModel =
            ListTreeTableModel(DefaultMutableTreeNode("Results"), arrayOf(TreeColumnInfo("Results")))
        private val myTree: TreeTable = TreeTable(treeModel).apply {
            setRootVisible(true)
            setTreeCellRenderer(ValidatorDefaultTreeCellRenderer())
            background = null
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
                    text = LocaleBundle.message("finish")
                    addActionListener {
                        browser.cefBrowser.loadURL(LocaleBundle.message("validator_url"))
                        showBrowser()
                        results = emptyMap()
                        initTreeModel()
                    }
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                })
//                TreeUtil.expandAll(myTree)
                add(JBScrollPane(myTree).apply {
                    isVisible = true
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    weighty = 1.0
                    gridx = 0
                })

                initTreeModel()
            }
            add(resultsPanel)
        }

        private fun initTreeModel() {
            // remove all nodes
            val root = treeModel.root as DefaultMutableTreeNode
            root.removeAllChildren()
//            treeModel.reload()

            sections.forEach { (_, value) ->
                val sectionNode = DefaultMutableTreeNode(ValidatorSection(value, "idle"))
                root.insert(
                    sectionNode,
                    treeModel.getChildCount(treeModel.root)
                )
            }
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

            results.plus(Pair(name, result))

            // update section state
            val sectionNode = treeModel.getChild(treeModel.root, sections.keys.indexOf(name)) as DefaultMutableTreeNode
            val section = sectionNode.userObject as ValidatorSection
            section.state = if (result == "[]") "success" else "error"
//            treeModel.nodeChanged(sectionNode)

            if (result == "[]") {
                return
            }

            try {
                val jsonObject = JSONObject(result)
                val builder = treeNodesBuilders[name]
                if (builder != null) {
                    val builderInstance = builder.createInstance()
                    val nodes = builderInstance.buildNodes(name, jsonObject)
                    val root = treeModel.root as DefaultMutableTreeNode
                    val sectionNode = treeModel.getChild(root, sections.keys.indexOf(name)) as DefaultMutableTreeNode
                    nodes.forEach {
                        sectionNode.insert(
                            it,
                            treeModel.getChildCount(sectionNode)
                        )
                    }
                }
            } catch (e: Exception) {
                thisLogger().warn("Error while parsing result", e)
            }
        }

        fun clearResults() {
            results = emptyMap<String, String>()
        }

        fun setState(state: String) {
            myTree.setPaintBusy(state == "loading")
        }

        fun setSectionLoading(resultName: String, loading: Boolean) {
            val sectionNode =
                treeModel.getChild(treeModel.root, sections.keys.indexOf(resultName)) as DefaultMutableTreeNode
            val section = sectionNode.userObject as ValidatorSection
            section.state = if (loading) "loading" else "idle"
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
