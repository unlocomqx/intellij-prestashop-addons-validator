package com.github.unlocomqx.validator.toolWindow.NodesBuilders

import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorItem
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorLine
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorMessage
import com.github.unlocomqx.validator.toolWindow.helpers.FileHelper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBEmptyBorder
import org.codehaus.jettison.json.JSONObject
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.SwingConstants
import javax.swing.border.CompoundBorder

interface NodesBuilder {
    fun buildComponents(name: String, jsonObject: JSONObject): MutableList<Component>
}

class FilesNodesBuilder : NodesBuilder {
    override fun buildComponents(name: String, jsonObject: JSONObject): MutableList<Component> {
        if (!jsonObject.has("files")) {
            return mutableListOf()
        }
        val files = try {
            jsonObject.getJSONObject("files")
        } catch (e: Exception) {
            return mutableListOf()
        }
        val errors = files.keys()
        val components = mutableListOf<Component>()
        while (errors.hasNext()) {
            val error = errors.next() as String
            val filesList = files.getJSONArray(error)
            val filesArray = Array(filesList.length()) { filesList.getString(it) }
            val errorNode = createValidatorNode(ValidatorMessage(error, "error"))
            filesArray.forEach {
                val fileNode = createValidatorNode(ValidatorFile(it)).apply {
                    border = JBEmptyBorder(0, 20, 0, 0)
                }
                errorNode.add(fileNode)
            }
            components.add(errorNode)
        }
        return components
    }
}

class CodeNodesBuilder : NodesBuilder {
    override fun buildComponents(name: String, jsonObject: JSONObject): MutableList<Component> {
        if (!jsonObject.has("code")) {
            return mutableListOf()
        }
        val files = try {
            jsonObject.getJSONObject("code")
        } catch (e: Exception) {
            return mutableListOf()
        }
        val nodes = mutableListOf<Component>()
        val fileKeys = files.keys()
        while (fileKeys.hasNext()) {
            val file = fileKeys.next() as String
            val fileMessages = files.getJSONObject(file)
            val messages = fileMessages.getJSONObject("messages")
            val messageKeys = messages.keys()
            while (messageKeys.hasNext()) {
                val message = messageKeys.next() as String
                val messageContent = messages.getJSONObject(message)
                val lines = messageContent.getJSONArray("content")
                val linesArray = Array(lines.length()) { lines.getJSONObject(it) }
                linesArray.forEach {
                    val lineNode = createValidatorNode(ValidatorLine(it))
                    nodes.add(lineNode)
                }
            }
        }
        return nodes
    }
}

private fun createValidatorNode(validatorItem: ValidatorItem): JBPanel<JBPanel<*>> {

    if (validatorItem is ValidatorMessage) {
        val message = validatorItem.message
        val icon = validatorItem.icon
        val component = ValidatorNode().apply {
            add(JBLabel(message, icon, SwingConstants.LEFT))
        }
        return component
    }

    if (validatorItem is ValidatorFile) {
        val path = validatorItem.path
        val itemIcon = validatorItem.icon
        val component = ValidatorNode().apply {
            add(JBLabel().apply {
                icon = itemIcon
                if (validatorItem.virtualFile != null) {
                    text = "<html><a href=\"$path\">$path</a></html>"
                } else {
                    text = path
                }
                cursor = Cursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        FileHelper.navigateToFile(validatorItem)
                    }
                })
            })
        }
        return component
    }

    if (validatorItem is ValidatorLine) {
        val jsonObject = validatorItem.jsonObject
        val path = jsonObject.getString("file")
        val line = jsonObject.getInt("line")
        val message = jsonObject.getString("message")
        val messageWithLinks = replaceHintsWithLinks(message)
        val icon = validatorItem.icon
        val component = ValidatorNode().apply {
            add(JBLabel().apply {
                alignmentX = Component.LEFT_ALIGNMENT
                toolTipText = path
                text = "<html>$messageWithLinks</html>"
            })
            add(JBLabel().apply {
                alignmentX = Component.LEFT_ALIGNMENT
                text = "<html><a href=\"$path:$line\">$path:$line</a></html>"
                cursor = Cursor(Cursor.HAND_CURSOR)
                border = JBEmptyBorder(0, 20, 0, 0)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        FileHelper.navigateToFile(validatorItem)
                    }
                })
            })
        }
        return component
    }

    return JBPanel<JBPanel<*>>().apply {
        add(JBLabel("Unknown item"))
    }
}

class ValidatorNode : JBPanel<JBPanel<*>>() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBEmptyBorder(5, 5, 10, 5),
        )
    }
}

fun replaceHintsWithLinks(message: String?): String {
    if (message == null) {
        return ""
    }

    var messageWithLinks: String = message
    // match words containing an underscore or more
    val matches = Regex("\\w+").findAll(message)
    for (match in matches) {
        // if contains underscore, it's a link
        if (match.value.contains("_")) {
            val link = "https://cs.symfony.com/doc/rules/operator/${match.value}.html"
            messageWithLinks =
                messageWithLinks.replace(
                    match.value,
                    "<a href=\"${link}\" target=\"_blank\">${match.value}</a>"
                )
        }
    }
    return messageWithLinks
}