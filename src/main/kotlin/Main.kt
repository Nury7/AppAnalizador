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
import java.util.Stack
import java.util.StringTokenizer
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

                val syntaxErrors = syntaxAnalyze(tokens)

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

    return Pair(Pair(tokens, vars), errors)
}

fun syntaxAnalyze(tokens: List<Token>): List<String> {
    val errors = mutableListOf<String>()
    val stack = Stack<ProductionRule>()

    for (token in tokens) {
        val matchingProductionRule = ProductionRules.getMatchingProductionRule(token)

        if (matchingProductionRule != null) {
            if (stack.isNotEmpty() && matchingProductionRule == stack.peek()) {
                continue
            }

            if (stack.size >= 2 && stack.elementAt(stack.size - 2) == ProductionRules.FUNCION &&
                stack.peek() == ProductionRules.PARAMETROS_TEXTO
            ) {
                stack.pop()
                stack.pop()
                stack.push(ProductionRules.TEXTO)
            } else if ((matchingProductionRule == ProductionRules.TEXTO ||
                        matchingProductionRule == ProductionRules.OPERACION ||
                        matchingProductionRule == ProductionRules.DECLARACION ||
                        matchingProductionRule == ProductionRules.ASIGNACION) &&
                (stack.isEmpty() || stack.peek() != ProductionRules.INSTRUCCION)
            ) {
                stack.push(ProductionRules.INSTRUCCION)
            } else if (matchingProductionRule == ProductionRules.CODIGO && stack.peek() == ProductionRules.INSTRUCCION) {
                stack.pop()
                stack.push(ProductionRules.CODIGO)
            } else {
                stack.push(matchingProductionRule)
            }
            println("Stack: $stack")
        } else {
            errors.add("Error: Token '${token.value}' inesperado")
        }
    }

    if (stack.isNotEmpty()) {
        errors.add("Error: Estructura incorrecta del programa")
    }

    return errors
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
