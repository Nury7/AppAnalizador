class ProductionRules {
    companion object {

        private val VALOR_OPERACION =
            ProductionRule("VALOR_OPERACION", listOf(TokenType.IDENTIFIER, TokenType.INTEGER, TokenType.FLOAT))
        private val PARAMETROS_OPERACION = ProductionRule(
            "PARAMETROS_OPERACION",
            listOf(
                TokenType.OPEN_PARENTHESES,
                VALOR_OPERACION,
                TokenType.COMA,
                VALOR_OPERACION,
                TokenType.CLOSE_PARENTHESES
            )
        )
        private val TIPO_OPERACION = ProductionRule(
            "TIPO_OPERACION",
            listOf(TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV)
        )

        private val OPERACION = ProductionRule("OPERACION", listOf(TIPO_OPERACION, PARAMETROS_OPERACION))

        private val VALOR_ASIGNADO = ProductionRule("VALOR_ASIGNADO", listOf(TokenType.INTEGER, TokenType.FLOAT, OPERACION))
        private val ASIGNACION =
            ProductionRule( "ASIGNACION", listOf(TokenType.IDENTIFIER, TokenType.DESIGNATOR, VALOR_ASIGNADO))

        private val VALOR_DECLARACION = ProductionRule("VALOR_DECLARACION", listOf(TokenType.IDENTIFIER, ASIGNACION))
        private val VALORES_DECLARACION =
            ProductionRule("VALORES_DECLARACION", listOf(VALOR_DECLARACION, TokenType.COMA, VALOR_DECLARACION))
        private val TIPO_DECLARACION = ProductionRule("TIPO_DECLARACION", listOf(TokenType.TYPE_INTEGER, TokenType.TYPE_FLOAT))
        private val DECLARACION = ProductionRule("DECLARACION", listOf(TIPO_DECLARACION, VALORES_DECLARACION))

        init {
            (OPERACION.tokens as MutableList)[1] = PARAMETROS_OPERACION
        }

        private val PARAMETROS_TEXTO = ProductionRule(
            "PARAMETROS_TEXTO",
            listOf(TokenType.OPEN_PARENTHESES, TokenType.IDENTIFIER, TokenType.CLOSE_PARENTHESES)
        )
        private val FUNCION = ProductionRule("FUNCION", listOf(TokenType.OP_READ, TokenType.OP_WRITE))
        private val TEXTO = ProductionRule("TEXTO", listOf(FUNCION, PARAMETROS_TEXTO))

        private val INSTRUCCION = ProductionRule("INSTRUCCION", listOf(TEXTO, OPERACION, DECLARACION, ASIGNACION))

        private val CODIGO = ProductionRule("CODIGO", listOf(INSTRUCCION, TokenType.DOT_COMA))
        private val FIN = ProductionRule("FIN", listOf(TokenType.CLOSE_CURLY_BRACE, TokenType.END))
        private val INICIO = ProductionRule("INICIO", listOf(TokenType.START, TokenType.OPEN_CURLY_BRACE))

        private val PROGRAMA = ProductionRule("PROGRAMA", listOf(INICIO, CODIGO, FIN))

        private fun checkStrictOrder(rule: ProductionRule): Boolean {
            return when (rule) {
                INICIO -> {
                    val tokens = rule.tokens
                    val startIndex = tokens.indexOf(TokenType.START)
                    val openCurlyIndex = tokens.indexOf(TokenType.OPEN_CURLY_BRACE)
                    startIndex >= 0 && openCurlyIndex >= 0 && startIndex < openCurlyIndex
                }
                TEXTO -> {
                    val tokens = rule.tokens
                    val funcionIndex = tokens.indexOf(FUNCION)
                    val parametrosIndex = tokens.indexOf(PARAMETROS_TEXTO)
                    funcionIndex >= 0 && parametrosIndex >= 0 && funcionIndex < parametrosIndex
                }
                PARAMETROS_TEXTO -> {
                    val tokens = rule.tokens
                    val openParenthesesIndex = tokens.indexOf(TokenType.OPEN_PARENTHESES)
                    val idIndex = tokens.indexOf(TokenType.IDENTIFIER)
                    val closeParenthesesIndex = tokens.indexOf(TokenType.CLOSE_PARENTHESES)
                    idIndex in (openParenthesesIndex + 1) until closeParenthesesIndex
                }
                OPERACION -> {
                    val tokens = rule.tokens
                    val tipoIndex = tokens.indexOf(TIPO_OPERACION)
                    val parametrosIndex = tokens.indexOf(PARAMETROS_OPERACION)
                    tipoIndex < parametrosIndex
                }
                PARAMETROS_OPERACION -> {
                    val tokens = rule.tokens
                    val openParenthesesIndex = tokens.indexOf(TokenType.OPEN_PARENTHESES)
                    val primerValorIndex = tokens.indexOf(VALOR_OPERACION)
                    val comaIndex = tokens.indexOf(TokenType.COMA)
                    val segundoValorIndex = tokens.indexOf(VALOR_OPERACION)
                    val closeParenthesesIndex = tokens.indexOf(TokenType.CLOSE_PARENTHESES)
                    primerValorIndex in (openParenthesesIndex + 1) until comaIndex &&
                            comaIndex < segundoValorIndex &&
                            segundoValorIndex < closeParenthesesIndex
                }
                FIN -> {
                    val tokens = rule.tokens
                    val closeCurlyIndex = tokens.indexOf(TokenType.CLOSE_CURLY_BRACE)
                    val endIndex = tokens.indexOf(TokenType.END)
                    closeCurlyIndex < endIndex
                }
                PROGRAMA -> {
                    val tokens = rule.tokens
                    val inicioIndex = tokens.indexOf(INICIO)
                    val codigoIndex = tokens.indexOf(CODIGO)
                    val finIndex = tokens.indexOf(FIN)
                    codigoIndex in (inicioIndex + 1) until finIndex
                }
                else -> true
            }
        }

        fun getMatchingProductionRule(token: Token): ProductionRule? {
            for (productionRule in allProductionRules) {
                if (productionRule.tokens.any { it == token.type } && checkStrictOrder(productionRule)) {
                    return productionRule
                }
            }
            return null
        }

        private val allProductionRules = listOf(
            VALOR_OPERACION, PARAMETROS_OPERACION, TIPO_OPERACION,
            OPERACION, VALOR_ASIGNADO, ASIGNACION, VALOR_DECLARACION,
            VALORES_DECLARACION, TIPO_DECLARACION, DECLARACION, PARAMETROS_TEXTO,
            FUNCION, TEXTO, INSTRUCCION, CODIGO, FIN, INICIO, PROGRAMA
        )

    }
}
