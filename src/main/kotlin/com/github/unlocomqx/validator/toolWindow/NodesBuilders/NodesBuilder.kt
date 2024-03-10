package com.github.unlocomqx.validator.toolWindow.NodesBuilders

import com.github.unlocomqx.validator.toolWindow.CellRenderer.ValidatorFile
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
        val errors = files.keys()
        val nodes = mutableListOf<DefaultMutableTreeNode>()
        while (errors.hasNext()) {
            val error = errors.next()
            val errorNode = DefaultMutableTreeNode(error)
            nodes.add(errorNode)
        }
        return nodes
    }
}