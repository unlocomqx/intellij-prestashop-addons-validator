package com.github.unlocomqx.validator.toolWindow

import com.github.unlocomqx.validator.LocaleBundle
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorTreeCellRenderer
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.CodeNodesBuilder
import com.github.unlocomqx.validator.toolWindow.NodesBuilders.FilesNodesBuilder
import com.github.unlocomqx.validator.toolWindow.helpers.FileHelper
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.codehaus.jettison.json.JSONObject
import org.eclipse.jgit.lib.ObjectChecker.tree
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
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
    "errors" to FilesNodesBuilder::class,
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

        private val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("Results"))
        private val myTree: Tree = Tree().apply {
            model = treeModel
            cellRenderer = ValidatorTreeCellRenderer()
            object : DoubleClickListener() {
                override fun onDoubleClick(e: MouseEvent): Boolean {
                    if (e.clickCount == 2) {
                        val node =
                            (e.source as Tree).getPathForLocation(e.x, e.y)?.lastPathComponent as DefaultMutableTreeNode
                        val nodeInfo = node.userObject
                        if (nodeInfo is ValidatorFile){
                            FileHelper.navigateToFile(nodeInfo.path)
                        }
                    }
                    return false
                }
            }.installOn(this)

            this.addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    if (e.keyChar.code == KeyEvent.VK_ENTER) {
                        val a = 1
                    }
                }
            })
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
                        val root = treeModel.root as DefaultMutableTreeNode
                        for (i in 0 until treeModel.getChildCount(root)) {
                            val sectionNode = treeModel.getChild(root, i) as DefaultMutableTreeNode
                            for (j in 0 until treeModel.getChildCount(sectionNode)) {
                                treeModel.removeNodeFromParent(treeModel.getChild(sectionNode, j) as DefaultMutableTreeNode)
                            }
                        }
                    }
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                })
                TreeUtil.expandAll(myTree)
                add(JBScrollPane(myTree).apply {
                    isVisible = true
                }, GridBagConstraints().apply {
                    anchor = GridBagConstraints.NORTHWEST
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    weighty = 1.0
                    gridx = 0
                })

                sections.forEach { (_, value) ->
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

             try {
               val jsonObject =  JSONObject(result)
                 val builder = treeNodesBuilders[name]
                 if (builder != null) {
                     val builderInstance = builder.createInstance()
                     val nodes = builderInstance.buildNodes(name, jsonObject)
                     val sectionNode = treeModel.getChild(treeModel.root, sections.keys.indexOf(name))
                     nodes.forEach {
                         treeModel.insertNodeInto(
                             it,
                             sectionNode as DefaultMutableTreeNode,
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
}
