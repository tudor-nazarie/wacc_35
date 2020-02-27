package wacc.ast.codegen.types

import wacc.ast.codegen.types.Instruction.Special.Label

data class Function(val label: Label, val instructions: List<Instruction>, val main: Boolean = false) {
    override fun toString(): String {
        val builder = StringBuilder()
        if (main) {
            // TODO find a way to use the Global special instruction?
            builder.append(".global ${label.name}\n")
        }
        builder.append("$label")
        instructions.forEach { builder.append(it) }
        return builder.toString()
    }
}