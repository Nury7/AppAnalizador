class TreeNode(
    val id: Int,
    val tokenType: TokenType?,
    val productionRule: ProductionRule?,
    var parent: TreeNode? = null
) {
    val children: MutableList<TreeNode> = mutableListOf()

    fun addChild(child: TreeNode) {
        children.add(child)
        child.parent = this
    }

    override fun toString(): String {
        val parentId = parent?.id ?: "null"
        return "Nodo: $id $tokenType Padre: $parentId"
    }
}