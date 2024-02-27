import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class CustomCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (column == 0) {
            c.font = Font(c.font.name, Font.BOLD, c.font.size)
        }

        if (column == 1) {
            c.font = Font(c.font.name, Font.ITALIC, c.font.size)
        }

        c.background = Color.BLACK
        c.foreground = Color.WHITE

        return c
    }
}