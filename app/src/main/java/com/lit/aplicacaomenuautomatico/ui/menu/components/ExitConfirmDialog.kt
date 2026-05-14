package com.lit.aplicacaomenuautomatico.ui.menu.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.lit.aplicacaomenuautomatico.ui.theme.ErrorColor
import com.lit.aplicacaomenuautomatico.ui.theme.Primary

/**
 * Diálogo de confirmação exibido quando o usuário pressiona Voltar no menu raiz (MAIN).
 * Evita saídas acidentais do aplicativo.
 *
 * @param onConfirmar Callback chamado ao confirmar a saída — encerra o app
 * @param onCancelar Callback chamado ao cancelar — retorna ao menu sem ação
 */
@Composable
fun ExitConfirmDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                text = "Sair do aplicativo?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Deseja encerrar o Menu Automático?",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            // Botão de confirmação em vermelho para reforçar a ação destrutiva
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorColor)
            ) {
                Text("Sair")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                colors = ButtonDefaults.textButtonColors(contentColor = Primary)
            ) {
                Text("Cancelar")
            }
        }
    )
}
