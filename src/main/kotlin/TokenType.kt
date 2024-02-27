enum class TokenType {
    START {
        override val pattern: String = "INICIO"
    },
    END {
        override val pattern: String = "FIN"
    },
    TYPE_INTEGER {
        override val pattern: String = "ENTERO"
    },
    TYPE_FLOAT {
        override val pattern: String = "FLOTANTE"
    },
    OP_READ {
        override val pattern: String = "LEER"
    },
    OP_WRITE {
        override val pattern: String = "IMPRIMIR"
    },
    OP_SUM {
        override val pattern: String = "SUM"
    },
    OP_RES {
        override val pattern: String = "RES"
    },
    OP_MUL {
        override val pattern: String = "MUL"
    },
    OP_DIV {
        override val pattern: String = "DIV"
    },
    OPEN_CURLY_BRACE {
        override val pattern: String = "\\{"
    },
    CLOSE_CURLY_BRACE {
        override val pattern: String = "\\}"
    },
    OPEN_PARENTHESES {
        override val pattern: String = "\\("
    },
    CLOSE_PARENTHESES {
        override val pattern: String = "\\)"
    },
    DOT_COMA {
        override val pattern: String = ";"
    },
    DOT {
        override val pattern: String = "\\."
    },
    COMA {
        override val pattern: String = ","
    },
    DESIGNATOR {
        override val pattern: String = "="
    },
    IDENTIFIER {
        override val pattern: String = "[a-zA-Z]\\d{0,3}"
    },
    INTEGER {
        override val pattern: String = "\\d{1,5}"
    },
    FLOAT {
        override val pattern: String = "\\d{1,5}(\\.\\d{1,3})?"
    };

    abstract val pattern: String
}
