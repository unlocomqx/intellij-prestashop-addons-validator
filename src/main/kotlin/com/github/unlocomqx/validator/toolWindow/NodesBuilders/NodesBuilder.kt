package com.github.unlocomqx.validator.toolWindow.NodesBuilders

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
            val error = errors.next()
            val errorNode = DefaultMutableTreeNode(error)
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