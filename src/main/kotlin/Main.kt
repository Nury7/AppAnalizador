import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.StringTokenizer
import java.util.Stack
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess


@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val fileChooser = JFileChooser()
    val title = remember { mutableStateOf("Pseudo-Kotlin") }

    var tokens: List<Token> by remember { mutableStateOf(emptyList()) }
    var vars: List<String> by remember { mutableStateOf(emptyList()) }

    val codeText = remember { mutableStateOf(TextFieldValue()) }
    val consoleText = remember { mutableStateOf(TextFieldValue()) }

    val changed = remember { mutableStateOf(false) }
    val withoutErrors = remember { mutableStateOf(false) }

    val isHoveringAnalyze = remember { mutableStateOf(false) }
    val isHoveringImport = remember { mutableStateOf(false) }
    val isHoveringExport = remember { mutableStateOf(false) }
    val isHoveringTokens = remember { mutableStateOf(false) }
    val isHoveringVariables = remember { mutableStateOf(false) }
    val isHoveringSyntaxTree = remember { mutableStateOf(false) }

    val stateVertical = rememberScrollState(0)
    val stateHorizontal = rememberScrollState(0)

    Window(
        onCloseRequest = {
            if (changed.value && codeText.value.text.isNotBlank()) {
                when (JOptionPane.showConfirmDialog(null, "Save changes?")) {
                    JOptionPane.OK_OPTION -> {
                        save(codeText, tokens, vars)
                        exitProcess(0)
                    }

                    JOptionPane.NO_OPTION -> exitProcess(0)
                }
            } else {
                exitProcess(0)
            }
        }, title = title.value
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(500.dp).padding(top = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().width(30.dp).background(Color.DarkGray)
                    .verticalScroll(stateVertical).horizontalScroll(stateHorizontal),
                contentAlignment = Alignment.TopStart
            ) {
                val lineNumbers = codeText.value.text.count { it == '\n' } + 1
                Text(
                    text = (1..lineNumbers).joinToString("\n"),
                    color = Color(170, 170, 170),
                    fontSize = 17.sp,
                    modifier = Modifier.padding(1.dp).align(Alignment.TopStart)
                )
            }

            BasicTextField(
                value = codeText.value,
                onValueChange = {
                    codeText.value = it
                    changed.value = true
                },
                textStyle = TextStyle(fontSize = 17.sp, color = Color.White),
                modifier = Modifier.fillMaxWidth().height(500.dp).background(Color.DarkGray)
                    .verticalScroll(stateVertical).horizontalScroll(stateHorizontal)
            )
        }

        BasicTextField(
            value = consoleText.value, onValueChange = {
                consoleText.value = it
            }, textStyle = TextStyle(
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                color = if (!withoutErrors.value) Color(216, 0, 50) else Color.Green,
                fontWeight = FontWeight.Bold
            ), modifier = Modifier.fillMaxSize().padding(top = 500.dp).background(Color.Black), enabled = false
        )

        Column(
            modifier = Modifier.fillMaxSize().wrapContentWidth(align = Alignment.End)
        ) {
            Button(onClick = {
                val lists = lexicalAnalyze(codeText.value.text)
                tokens = lists.first.first
                vars = lists.first.second
                val lexicalErrors = lists.second

                val syntaxLists = syntaxAnalyze(tokens)
                val syntaxErrors = syntaxLists.first
                val syntaxTree = syntaxLists.second

                val allErrors = lexicalErrors + syntaxErrors

                if (allErrors.isEmpty()) {
                    withoutErrors.value = true
                    consoleText.value = TextFieldValue("No errors found")
                } else {
                    withoutErrors.value = false
                    val concatenatedErrors = allErrors.joinToString("\n")
                    consoleText.value = TextFieldValue(concatenatedErrors)
                }
            },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringAnalyze.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringAnalyze.value = false }.padding(8.dp).then(
                        if (isHoveringAnalyze.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringAnalyze.value) {
                    Text(
                        text = "Analyze"
                    )
                }
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }

            Button(onClick = {
                fileChooser.fileFilter = FileNameExtensionFilter("Archives", "pk")
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val selectedFile = fileChooser.selectedFile
                    val jsonData = selectedFile.readText()
                    val gson = Gson()
                    val data = gson.fromJson<Map<String, Any>>(jsonData, object : TypeToken<Map<String, Any>>() {}.type)

                    codeText.value = TextFieldValue(data["code"].toString())
                    tokens = gson.fromJson(gson.toJson(data["tokens"]), object : TypeToken<List<Token>>() {}.type)
                    vars = gson.fromJson(data["vars"].toString(), object : TypeToken<List<String>>() {}.type)

                    title.value = selectedFile.nameWithoutExtension
                }
                changed.value = false
            },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringImport.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringImport.value = false }.padding(8.dp).then(
                        if (isHoveringImport.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringImport.value) {
                    Text(
                        text = "Load"
                    )
                }
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
            }

            Button(onClick = {
                title.value = save(codeText, tokens, vars)
                changed.value = false
            },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringExport.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringExport.value = false }.padding(8.dp).then(
                        if (isHoveringExport.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringExport.value) {
                    Text(
                        text = "Save"
                    )
                }
                Icon(imageVector = Icons.Default.Upload, contentDescription = null)
            }

            Button(enabled = tokens.isNotEmpty(),
                onClick = {
                    val rowData = tokens.map { arrayOf(it.type, it.value) }.toTypedArray()
                    val cols = arrayOf<Any>(
                        "Token", "Value"
                    )
                    val table = JTable(rowData, cols)
                    table.isEnabled = false
                    table.setDefaultRenderer(Any::class.java, CustomCellRenderer())
                    JOptionPane.showMessageDialog(null, JScrollPane(table), "Tokens", JOptionPane.PLAIN_MESSAGE)
                },

                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringTokens.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringTokens.value = false }.padding(8.dp).then(
                        if (isHoveringTokens.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringTokens.value) {
                    Text(
                        text = "Tokens"
                    )
                }
                Icon(imageVector = Icons.Default.List, contentDescription = null)
            }

            Button(enabled = vars.isNotEmpty(),
                onClick = {
                    val rowData = vars.map { arrayOf(it) }.toTypedArray()
                    val cols = arrayOf<Any>(
                        "Identifiers"
                    )
                    val table = JTable(rowData, cols)
                    table.isEnabled = false
                    table.setDefaultRenderer(Any::class.java, CustomCellRenderer())
                    JOptionPane.showMessageDialog(null, JScrollPane(table), "Variables", JOptionPane.PLAIN_MESSAGE)
                },

                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringVariables.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringVariables.value = false }.padding(8.dp).then(
                        if (isHoveringVariables.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringVariables.value) {
                    Text(
                        text = "Variables"
                    )
                }
                Icon(imageVector = Icons.Default.List, contentDescription = null)
            }

            Button(
                onClick = {

                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringSyntaxTree.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringSyntaxTree.value = false }.padding(8.dp).then(
                        if (isHoveringSyntaxTree.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )
            ) {
                AnimatedVisibility(visible = isHoveringSyntaxTree.value) {
                    Text(
                        text = "Syntax Tree"
                    )
                }
                Icon(imageVector = Icons.Default.List, contentDescription = null)
            }


        }
    }
}

fun lexicalAnalyze(sourceCode: String): Pair<Pair<List<Token>, List<String>>, List<String>> {
    val stringTokenizer = StringTokenizer(sourceCode, "{}();,=\n\t ", true)
    val tokens = mutableListOf<Token>()
    val vars = mutableListOf<String>()
    val errors = mutableListOf<String>()
    var lineNumber = 1

    while (stringTokenizer.hasMoreTokens()) {
        val word = stringTokenizer.nextToken()

        if (word.isNotBlank()) {
            var foundValidToken = false

            for (tokenType in TokenType.values()) {
                val matcher = Regex(tokenType.pattern).toPattern().matcher(word)
                if (matcher.matches()) {
                    tokens.add(Token(tokenType, word, lineNumber))
                    if (tokenType == TokenType.IDENTIFIER && !vars.contains(word)) {
                        vars.add(word)
                    }
                    foundValidToken = true
                    break
                }
            }

            if (!foundValidToken) {
                errors.add("Error: Invalid token at line $lineNumber: '$word'")
            }
        } else if (word == "\n") {
            lineNumber++
        }
    }

    return Pair(Pair(tokens, vars), errors)
}

fun syntaxAnalyze(tokens: List<Token>): Pair<List<String>,List<TreeNode>> {
    val errors = mutableListOf<String>()
    val tree = mutableListOf<TreeNode>()
    val stack = Stack<Any>()

    var currentIndex = 0
    var lineNumber = 1

    while (currentIndex < tokens.size) {
        val currentToken = tokens[currentIndex]
        stack.push(currentToken.type)
        lineNumber = currentToken.line


        when (stack.peek()) {
            TokenType.START -> {
                val nextIndex = currentIndex + 1
                if (nextIndex < tokens.size && tokens[nextIndex].type != TokenType.OPEN_CURLY_BRACE) {
                    errors.add("Error at line ${lineNumber}: Falta '{' después de INICIO")
                }
            }

            TokenType.OPEN_CURLY_BRACE -> {
                val indexOfStart = stack.indexOf(TokenType.START)
                if (indexOfStart == -1) {
                    errors.add("Falta INICIO antes de '{'")
                } else {
                    stack.pop()
                    stack.pop()
                    stack.push(ProductionRule.INICIO)
                }
            }

            TokenType.CLOSE_CURLY_BRACE -> {
                val nextIndex = currentIndex + 1
                if (nextIndex == tokens.size) {
                    errors.add("Falta FIN después de '}'")
                }
            }

            TokenType.END -> {
                val indexOfCloseCurly = stack.indexOf(TokenType.CLOSE_CURLY_BRACE)
                if (indexOfCloseCurly == -1) {
                    errors.add("Falta '}' antes de FIN")
                } else {
                    stack.pop()
                    stack.pop()
                    stack.push(ProductionRule.FIN)
                }
            }

            TokenType.OP_READ, TokenType.OP_WRITE -> {
                stack.pop()
                stack.push(ProductionRule.FUNCION)
            }

            TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV -> {
                stack.pop()
                stack.push(ProductionRule.TIPO_OPERACION)
            }

            TokenType.OPEN_PARENTHESES -> {
                currentIndex++
                stack.push(tokens[currentIndex].type)
                when (stack.peek()) {
                    TokenType.IDENTIFIER -> {
                        if (currentIndex + 1 < tokens.size && tokens[currentIndex + 1].type == TokenType.CLOSE_PARENTHESES) {
                            currentIndex++
                            stack.push(tokens[currentIndex].type)

                            stack.pop()
                            stack.pop()
                            stack.pop()
                            stack.push(ProductionRule.PARAMETROS_TEXTO)

                        } else if (currentIndex + 1 < tokens.size && tokens[currentIndex + 1].type == TokenType.COMA) {
                            currentIndex++
                            stack.pop()
                            stack.push(ProductionRule.VALOR_OPERACION)

                            stack.push(tokens[currentIndex++].type)
                            stack.push(tokens[currentIndex].type)

                            when (stack.peek()) {
                                TokenType.IDENTIFIER, TokenType.INTEGER, TokenType.FLOAT, ProductionRule.OPERACION -> {
                                    stack.pop()
                                    stack.push(ProductionRule.VALOR_OPERACION)
                                }

                                TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV -> {
                                    stack.pop()

                                    continue
                                }
                            }
                        } else {
                            errors.add("Parámetros inválidos")
                        }

                    }

                    TokenType.INTEGER, TokenType.FLOAT -> {
                        if (currentIndex + 1 < tokens.size && tokens[currentIndex + 1].type == TokenType.COMA) {
                            currentIndex++
                            stack.pop()
                            stack.push(ProductionRule.VALOR_OPERACION)

                            stack.push(tokens[currentIndex++].type)
                            stack.push(tokens[currentIndex].type)

                            when (stack.peek()) {
                                TokenType.IDENTIFIER, TokenType.INTEGER, TokenType.FLOAT, ProductionRule.OPERACION -> {
                                    stack.pop()
                                    stack.push(ProductionRule.VALOR_OPERACION)
                                }

                                TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV -> {
                                    stack.pop()

                                    continue
                                }
                            }
                        } else {
                            errors.add("Parámetros inválidos")
                        }
                    }

                    TokenType.OP_SUM, TokenType.OP_RES, TokenType.OP_MUL, TokenType.OP_DIV -> {
                        stack.pop()
                        continue
                    }
                }
            }

            TokenType.CLOSE_PARENTHESES -> {
                stack.pop()
                if (stack.peek() == ProductionRule.VALORES) {
                    stack.pop()
                    if (stack.peek() == TokenType.OPEN_PARENTHESES) {
                        stack.pop()
                        stack.push(ProductionRule.PARAMETROS_OPERACION)
                    } else {
                        errors.add("Falta '(' en los parámetros de operación")
                    }
                } else if (stack.peek() == ProductionRule.OPERACION) {
                    stack.pop()
                    stack.push(ProductionRule.VALOR_OPERACION)

                    currentIndex--
                } else {
                    errors.add("Faltan valores antes de ')'")
                }
            }

            TokenType.IDENTIFIER -> {
                if (currentIndex + 1 < tokens.size && tokens[currentIndex + 1].type == TokenType.DESIGNATOR) {
                    currentIndex++
                    stack.push(tokens[currentIndex].type)
                } else if (tokens[currentIndex - 1].type == TokenType.TYPE_INTEGER
                    || tokens[currentIndex - 1].type == TokenType.TYPE_FLOAT
                    || tokens[currentIndex - 1].type == TokenType.COMA) {
                    stack.pop()
                    stack.push(ProductionRule.VALOR_DECLARACION)
                } else {
                    errors.add("Falta operador de asignación después del identificador")
                }
            }

            TokenType.INTEGER, TokenType.FLOAT, ProductionRule.OPERACION -> {
                stack.pop()
                if (stack.peek() == TokenType.DESIGNATOR) {
                    stack.push(ProductionRule.VALOR_ASIGNADO)

                    stack.pop()
                    stack.pop()
                    stack.pop()
                    stack.push(ProductionRule.ASIGNACION);
                }
            }

            TokenType.TYPE_INTEGER, TokenType.TYPE_FLOAT -> {
                stack.pop()
                stack.push(ProductionRule.TIPO_DECLARACION)
            }

        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.PARAMETROS_TEXTO) {
            stack.pop()
            if (stack.peek() == ProductionRule.FUNCION) {
                stack.pop()
                stack.push(ProductionRule.TEXTO)
            } else {
                errors.add("Falta especificar función")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.PARAMETROS_OPERACION) {
            stack.pop()
            if (stack.peek() == ProductionRule.TIPO_OPERACION) {
                stack.pop()
                if (stack.peek() == TokenType.DESIGNATOR) {
                    stack.push(ProductionRule.OPERACION)

                    stack.pop()
                    stack.push(ProductionRule.VALOR_ASIGNADO)
                } else {
                    stack.push(ProductionRule.OPERACION)
                }
            } else {
                errors.add("Falta especificar operación")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.VALOR_OPERACION) {
            stack.pop()
            if (stack.peek() == TokenType.COMA) {
                stack.pop()
                if (stack.peek() == ProductionRule.VALOR_OPERACION) {
                    stack.pop()
                    stack.push(ProductionRule.VALORES)
                } else {
                    errors.add("Falta primer parámetro")
                }
            } else {
                errors.add("Falta coma en operación")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.VALORES) {
            val nextIndex = currentIndex + 1
            if (nextIndex < tokens.size && tokens[nextIndex].type != TokenType.CLOSE_PARENTHESES) {
                errors.add("Falta ')' en operación")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.VALOR_ASIGNADO) {
            stack.pop()
            if (stack.peek() == TokenType.DESIGNATOR) {
                stack.pop()
                if (stack.peek() == TokenType.IDENTIFIER) {
                    stack.pop()

                    stack.push(ProductionRule.ASIGNACION)
                } else {
                    errors.add("Falta el identificador a asignar")
                }
            } else {
                errors.add("Falta '=' en asignación")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.ASIGNACION) {
            stack.pop()

            if(stack.peek() == ProductionRule.TIPO_DECLARACION){
                stack.push(ProductionRule.VALORES_DECLARACION)
            } else {
                stack.push(ProductionRule.ASIGNACION)
            }

        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.VALOR_DECLARACION) {
            val nextIndex = currentIndex + 1

            stack.pop()
            stack.push(ProductionRule.VALORES_DECLARACION)
            if (nextIndex < tokens.size && tokens[nextIndex].type == TokenType.COMA) {
                currentIndex++
            } else {
                stack.pop()
                if (stack.peek() == ProductionRule.TIPO_DECLARACION) {
                    stack.push(ProductionRule.VALORES_DECLARACION)
                } else if (stack.peek() == TokenType.COMA) {
                    stack.pop()
                    if (stack.peek() == ProductionRule.VALORES_DECLARACION) {
                        stack.pop()
                        stack.push(ProductionRule.VALORES_DECLARACION)
                    } else {
                        errors.add("No se especifica el tipo de declaracion")
                    }
                } else {
                    errors.add("Falta ',' para separar las declaraciones")
                }
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.VALORES_DECLARACION) {
            stack.pop()
            if (stack.peek() == ProductionRule.TIPO_DECLARACION) {
                stack.pop()
                stack.push(ProductionRule.DECLARACION)
            } else {
                errors.add("Falta el tipo de dato a declarar")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == TokenType.DOT_COMA) {
            stack.pop()
            if (stack.peek() == ProductionRule.TEXTO || stack.peek() == ProductionRule.OPERACION
                || stack.peek() == ProductionRule.DECLARACION || stack.peek() == ProductionRule.ASIGNACION
            ) {
                stack.pop()
                stack.push(ProductionRule.INSTRUCCION)
            } else {
                errors.add("';' mal colocado")
            }
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.INSTRUCCION) {
            stack.pop()
            if (stack.isNotEmpty() && stack.peek() == ProductionRule.CODIGO) {
                stack.pop()
            }
            stack.push(ProductionRule.CODIGO)
            println(stack)
        }

        if (stack.isNotEmpty() && stack.peek() == ProductionRule.FIN) {
            stack.pop()
            if (stack.peek() == ProductionRule.CODIGO) {
                stack.pop()
                if (stack.peek() == ProductionRule.INICIO) {
                    stack.pop()
                    stack.push(ProductionRule.PROGRAMA)
                }
            }
        }

        currentIndex++
    }

    if (tokens.isNotEmpty() && stack.peek() == ProductionRule.PROGRAMA) {
        stack.pop()
    } else {
        errors.add("Error en la compilación")
    }

    return Pair(errors, tree)
}


fun save(codeText: MutableState<TextFieldValue>, tokens: List<Token>, vars: List<String>): String {
    val fileChooser = JFileChooser()
    fileChooser.fileFilter = FileNameExtensionFilter("Archives", "pk")
    fileChooser.dialogTitle = "Save file..."
    val result = fileChooser.showSaveDialog(null)
    var name = ""

    if (result == JFileChooser.APPROVE_OPTION) {
        val selectedFile = fileChooser.selectedFile
        val selectedFilePath = if (!selectedFile.absolutePath.endsWith(".pk")) {
            selectedFile.absolutePath + ".pk"
        } else {
            selectedFile.absolutePath
        }

        val fileToSave = File(selectedFilePath)
        try {
            val dataToSave = mapOf(
                "code" to codeText.value.text, "tokens" to tokens, "vars" to vars
            )

            val gson = Gson()
            val jsonData = gson.toJson(dataToSave)
            fileToSave.createNewFile()
            fileToSave.writeText(jsonData)
            name = fileToSave.nameWithoutExtension
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
    return name
}


fun main() = application {
    App()
}
