class ProductionRules {
    companion object {

        val VALOR_OPERACION =
            ProductionRule("VALOR_OPERACION", listOf(TokenType.IDENTIFIER, TokenType.INTEGER, TokenType.FLOAT))
        val VALORES = ProductionRule("VALORES", listOf(VALOR_OPERACION, TokenType.COMA, VALOR_OPERACION))
        val PARAMETROS_OPERACION = ProductionRule(
            "PARAMETROS_OPERACION",
            listOf(TokenType.OPEN_PARENTHESES, VALORES, TokenType.CLOSE_PARENTHESES)
        )
        val TIPO_OPERACION = ProductionRule(
            "TIPO_OPERACION",
            listOf(TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV)
        )

        val OPERACION = ProductionRule("OPERACION", listOf(TIPO_OPERACION, PARAMETROS_OPERACION))

        val VALOR_ASIGNADO = ProductionRule("VALOR_ASIGNADO", listOf(TokenType.INTEGER, TokenType.FLOAT, OPERACION))
        val ASIGNACION =
            ProductionRule("ASIGNACION", listOf(TokenType.IDENTIFIER, TokenType.DESIGNATOR, VALOR_ASIGNADO))

        val VALOR_DECLARACION = ProductionRule("VALOR_DECLARACION", listOf(TokenType.IDENTIFIER, ASIGNACION))
        val VALORES_DECLARACION =
            ProductionRule("VALORES_DECLARACION", listOf(VALOR_DECLARACION, TokenType.COMA, VALOR_DECLARACION))
        val TIPO_DECLARACION = ProductionRule("TIPO_DECLARACION", listOf(TokenType.TYPE_INTEGER, TokenType.TYPE_FLOAT))
        val DECLARACION = ProductionRule("DECLARACION", listOf(TIPO_DECLARACION, VALORES_DECLARACION))

        init {
            (OPERACION.tokens as MutableList)[1] = PARAMETROS_OPERACION
            (VALORES.tokens as MutableList)[2] = VALOR_OPERACION
        }

        val PARAMETROS_TEXTO = ProductionRule(
            "PARAMETROS_TEXTO",
            listOf(TokenType.OPEN_PARENTHESES, TokenType.IDENTIFIER, TokenType.CLOSE_PARENTHESES)
        )
        val FUNCION = ProductionRule("FUNCION", listOf(TokenType.OP_READ, TokenType.OP_WRITE))
        val TEXTO = ProductionRule("TEXTO", listOf(FUNCION, PARAMETROS_TEXTO))

        val INSTRUCCION = ProductionRule("INSTRUCCION", listOf(TEXTO, OPERACION, DECLARACION, ASIGNACION))

        val CODIGO = ProductionRule("CODIGO", listOf(INSTRUCCION))
        val FIN = ProductionRule("FIN", listOf(TokenType.CLOSE_CURLY_BRACE, TokenType.END))
        val INICIO = ProductionRule("INICIO", listOf(TokenType.START, TokenType.OPEN_CURLY_BRACE))

        val PROGRAMA = ProductionRule("PROGRAMA", listOf(INICIO, CODIGO, FIN))

        fun getMatchingProductionRule(token: Token): ProductionRule? {
            for (productionRule in allProductionRules) {
                if (productionRule.tokens.any { it == token.type }) {
                    return productionRule
                }
            }
            return null
        }

        private val allProductionRules = listOf(
            VALOR_OPERACION, VALORES, PARAMETROS_OPERACION, TIPO_OPERACION,
            OPERACION, VALOR_ASIGNADO, ASIGNACION, VALOR_DECLARACION,
            VALORES_DECLARACION, TIPO_DECLARACION, DECLARACION, PARAMETROS_TEXTO,
            FUNCION, TEXTO, INSTRUCCION, CODIGO, FIN, INICIO, PROGRAMA
        )

    }
}