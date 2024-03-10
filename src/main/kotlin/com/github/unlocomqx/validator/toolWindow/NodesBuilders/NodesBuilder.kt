package com.github.unlocomqx.validator.toolWindow.NodesBuilders

import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorLine
import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorMessage
import org.codehaus.jettison.json.JSONObject
import javax.swing.tree.DefaultMutableTreeNode

interface NodesBuilder {
    fun buildNodes(name: String, jsonObject: JSONObject): MutableList<DefaultMutableTreeNode>
}

class FilesNodesBuilder : NodesBuilder {
    override fun buildNodes(name: String, jsonObject: JSONObject): MutableList<DefaultMutableTreeNode> {
        if (!jsonObject.has("files")) {
            return mutableListOf()
        }
        val files = try {
            jsonObject.getJSONObject("files")
        } catch (e: Exception) {
            return mutableListOf()
        }
        val errors = files.keys()
        val nodes = mutableListOf<DefaultMutableTreeNode>()
        while (errors.hasNext()) {
            val error = errors.next() as String
            val filesList = files.getJSONArray(error)
            val filesArray = Array(filesList.length()) { filesList.getString(it) }
            val errorNode = DefaultMutableTreeNode(ValidatorMessage(error, "error"))
            filesArray.forEach {
                val fileNode = DefaultMutableTreeNode(ValidatorFile(it))
                errorNode.add(fileNode)
            }
            nodes.add(errorNode)
        }
        return nodes
    }
}

class CodeNodesBuilder : NodesBuilder {
    override fun buildNodes(name: String, jsonObject: JSONObject): MutableList<DefaultMutableTreeNode> {
        if (!jsonObject.has("code")) {
            return mutableListOf()
        }
        val files = try {
            jsonObject.getJSONObject("code")
        } catch (e: Exception) {
            return mutableListOf()
        }
        val nodes = mutableListOf<DefaultMutableTreeNode>()
        while (files.keys().hasNext()) {
            val file = files.keys().next() as String
            val fileMessages = files.getJSONObject(file)
            val messages = fileMessages.getJSONObject("messages")
            while (messages.keys().hasNext()) {
                val message = messages.keys().next() as String
                val messageContent = messages.getJSONObject(message)
                val type = messageContent.getString("type")
                val lines = messageContent.getJSONArray("content")
                var linesArray = Array(lines.length()) { lines.getJSONObject(it) }
                linesArray.forEach {
                    val lineNode = DefaultMutableTreeNode(ValidatorLine(it))
                    nodes.add(lineNode)
                }
            }
        }
        return nodes
    }
}