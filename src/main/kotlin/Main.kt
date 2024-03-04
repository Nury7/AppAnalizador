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
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess
import java.util.Stack


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
                val lists = analyze(codeText.value.text)
                tokens = lists.first.first
                vars = lists.first.second
                val errors = lists.second
                if (errors.isEmpty()) {
                    withoutErrors.value = true
                    consoleText.value = TextFieldValue("SOURCE CODE WITHOUT ERRORS")
                } else {
                    withoutErrors.value = false
                    val concatenatedErrors = errors.joinToString("\n")
                    consoleText.value = TextFieldValue(concatenatedErrors)
                }
            },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.onPointerEvent(PointerEventType.Enter) { isHoveringAnalyze.value = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringAnalyze.value = false }.padding(8.dp).then(
                        if (isHoveringAnalyze.value) Modifier.width(120.dp) else Modifier.width(120.dp)
                    )) {
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
                    )) {
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
                    )) {
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
                    )) {
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
                    )) {
                AnimatedVisibility(visible = isHoveringVariables.value) {
                    Text(
                        text = "Variables"
                    )
                }
                Icon(imageVector = Icons.Default.List, contentDescription = null)
            }

            Button(
                onClick = {
                    print("Árbol de sintáxis épico")
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

fun analyze(sourceCode: String): Pair<Pair<List<Token>, List<String>>, List<String>> {
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
                    tokens.add(Token(tokenType, word))
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

    val syntaxAnalyzer = SyntaxAnalyzer(tokens)

    val syntaxAnalysisResult = syntaxAnalyzer.parse()
    if (!syntaxAnalysisResult) {
        val syntaxErrors = syntaxAnalyzer.getSyntaxErrors()
        errors.addAll(syntaxErrors)
    }

    return Pair(Pair(tokens, vars), errors)
}

class SyntaxAnalyzer(private val tokens: List<Token>) {

    private val stack = Stack<ProductionRule>()
    private var index = 0
    private val syntaxErrors = mutableListOf<String>()

    fun parse(): Boolean {
        stack.push(ProductionRule.PROGRAMA)
        return programa()
    }

    private fun programa(): Boolean {
        if (!inicio()) {
            reportSyntaxError(ProductionRule.INICIO, "Expected 'START' at the beginning of the program")
            return false
        }

        if (!codigo()) {
            reportSyntaxError(ProductionRule.CODIGO, "Error in program code")
            return false
        }

        if (!fin()) {
            reportSyntaxError(ProductionRule.FIN, "Expected 'END' at the end of the program")
            return false
        }

        if (!stack.isEmpty()) {
            reportSyntaxError(ProductionRule.PROGRAMA, "Unexpected tokens after the end of the program")
            return false
        }

        return true
    }

    private fun inicio(): Boolean {
        if (matchAndAdvance(TokenType.START) && matchAndAdvance(TokenType.OPEN_CURLY_BRACE)) {
            stack.push(ProductionRule.INICIO)
            return true
        }
        reportSyntaxError(ProductionRule.INICIO, "Expected 'START' at the beginning of the program")
        return false
    }

    private fun fin(): Boolean {
        if (matchAndAdvance(TokenType.CLOSE_CURLY_BRACE) && matchAndAdvance(TokenType.END)) {
            stack.push(ProductionRule.FIN)
            return true
        }
        return false
    }

    private fun codigo(): Boolean {
        stack.push(ProductionRule.CODIGO)
        return instruccion()
    }

    private fun instruccion(): Boolean {
        stack.push(ProductionRule.INSTRUCCION)
        return when {
            texto() -> true
            operacion() -> true
            declaracion() -> true
            asignacion() -> true
            else -> false
        }
    }

    private fun declaracion(): Boolean {
        stack.push(ProductionRule.DECLARACION)
        return tipoDeclaracion() && valoresDeclaracion() && stack.pop() == ProductionRule.DECLARACION
    }

    private fun tipoDeclaracion(): Boolean {
        if (matchAndAdvance(TokenType.TYPE_INTEGER) || matchAndAdvance(TokenType.TYPE_FLOAT)) {
            stack.push(ProductionRule.TIPO_DECLARACION)
            return true
        }
        reportSyntaxError(ProductionRule.TIPO_DECLARACION, "Expected data type for declaration")
        return false
    }

    private fun valoresDeclaracion(): Boolean {
        if (valorDeclaracion()) {
            stack.push(ProductionRule.VALORES_DECLARACION)
            return true
        }
        reportSyntaxError(ProductionRule.VALORES_DECLARACION, "Expected values for declaration")
        return false
    }

    private fun valorDeclaracion(): Boolean {
        stack.push(ProductionRule.VALOR_DECLARACION)
        return matchAndAdvance(TokenType.IDENTIFIER) || asignacion()
    }

    private fun asignacion(): Boolean {
        stack.push(ProductionRule.ASIGNACION)
        return matchAndAdvance(TokenType.IDENTIFIER) && matchAndAdvance(TokenType.DESIGNATOR) &&
                valorAsignado() && stack.pop() == ProductionRule.ASIGNACION
    }

    private fun valorAsignado(): Boolean {
        stack.push(ProductionRule.VALOR_ASIGNADO)
        return matchAndAdvance(TokenType.INTEGER) || matchAndAdvance(TokenType.FLOAT) || operacion()
    }

    private fun texto(): Boolean {
        stack.push(ProductionRule.TEXTO)
        return funcion() && parametrosTexto() && stack.pop() == ProductionRule.TEXTO
    }

    private fun funcion(): Boolean {
        if (matchAndAdvance(TokenType.OP_READ) || matchAndAdvance(TokenType.OP_WRITE)) {
            stack.push(ProductionRule.FUNCION)
            return true
        }
        reportSyntaxError(ProductionRule.FUNCION, "Expected 'READ' or 'WRITE' function")
        return false
    }

    private fun parametrosTexto(): Boolean {
        if (matchAndAdvance(TokenType.OPEN_PARENTHESES) && matchAndAdvance(TokenType.IDENTIFIER) && matchAndAdvance(
                TokenType.CLOSE_PARENTHESES
            )
        ) {
            stack.push(ProductionRule.PARAMETROS_TEXTO)
            return true
        }
        reportSyntaxError(ProductionRule.PARAMETROS_TEXTO, "Expected parameter for text function")
        return false
    }

    private fun operacion(): Boolean {
        stack.push(ProductionRule.OPERACION)
        return tipoOperacion() && parametrosOperacion() && stack.pop() == ProductionRule.OPERACION
    }

    private fun tipoOperacion(): Boolean {
        if (matchAndAdvance(TokenType.OP_SUM) || matchAndAdvance(TokenType.OP_RES) ||
            matchAndAdvance(TokenType.OP_MUL) || matchAndAdvance(TokenType.OP_DIV)
        ) {
            stack.push(ProductionRule.TIPO_OPERACION)
            return true
        }
        reportSyntaxError(ProductionRule.TIPO_OPERACION, "Expected operator type for operation")
        return false
    }

    private fun parametrosOperacion(): Boolean {
        if (matchAndAdvance(TokenType.OPEN_PARENTHESES) && valores() && matchAndAdvance(TokenType.CLOSE_PARENTHESES)) {
            stack.push(ProductionRule.PARAMETROS_OPERACION)
            return true
        }
        reportSyntaxError(ProductionRule.PARAMETROS_OPERACION, "Expected parameters for operation")
        return false
    }

    private fun valores(): Boolean {
        stack.push(ProductionRule.VALORES)
        return valorOperacion() && matchAndAdvance(TokenType.COMA) && valorOperacion() && stack.pop() == ProductionRule.VALORES
    }

    private fun valorOperacion(): Boolean {
        stack.push(ProductionRule.VALOR_OPERACION)
        return matchAndAdvance(TokenType.IDENTIFIER) || matchAndAdvance(TokenType.INTEGER) ||
                matchAndAdvance(TokenType.FLOAT) || operacion()
    }

    private fun reportSyntaxError(rule: ProductionRule, message: String) {
        syntaxErrors.add("Syntax error at index $index in $rule: $message")
    }

    fun getSyntaxErrors(): List<String> {
        return syntaxErrors
    }

    private fun matchAndAdvance(expectedType: TokenType): Boolean {
        return if (index < tokens.size && tokens[index].type == expectedType) {
            index++
            true
        } else {
            false
        }
    }
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
