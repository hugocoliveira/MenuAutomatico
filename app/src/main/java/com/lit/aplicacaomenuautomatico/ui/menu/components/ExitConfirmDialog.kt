package com.lit.aplicacaomenuautomatico.ui.menu.components

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material3 (diálogo e botões)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.AlertDialog    // diálogo modal com título, corpo e botões
import androidx.compose.material3.ButtonDefaults // customiza cores dos botões (TextButton)
import androidx.compose.material3.MaterialTheme  // acessa a tipografia do tema
import androidx.compose.material3.Text           // exibe textos no título, corpo e botões
import androidx.compose.material3.TextButton     // botão de texto plano sem fundo (estilo Material3)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose runtime
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.runtime.Composable // marca a função como Composable

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — tema do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.ui.theme.ErrorColor // vermelho #BA1A1A — reforça ação destrutiva
import com.lit.aplicacaomenuautomatico.ui.theme.Primary    // azul #051FC7 — botão Cancelar (ação segura)

// ═════════════════════════════════════════════════════════════════════════════
// COMPOSABLE: ExitConfirmDialog
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Diálogo de confirmação exibido quando o usuário pressiona Voltar no menu raiz (MAIN).
 *
 * Objetivo: evitar saídas acidentais do aplicativo — o operador de armazém pode
 * pressionar Voltar sem intenção de fechar o app enquanto navega rapidamente.
 *
 * Comportamento:
 *  - Tocar fora do diálogo → chama [onCancelar] (não fecha automaticamente)
 *  - "Sair" em vermelho    → [onConfirmar] → a Activity encerra o app (finish())
 *  - "Cancelar" em azul   → [onCancelar]  → fecha o diálogo e volta ao menu
 *
 * Composable stateless — sem estado interno; todo o controle vem do [MenuViewModel].
 *
 * @param onConfirmar Callback ao confirmar saída — encerra o processo do app (Activity.finish())
 * @param onCancelar  Callback ao cancelar — fecha o diálogo e retorna ao menu sem ação
 */
@Composable
fun ExitConfirmDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        // Tocar fora do diálogo = cancelar (mesma ação que o botão "Cancelar")
        onDismissRequest = onCancelar,

        // ─── TÍTULO DO DIÁLOGO ────────────────────────────────────────────────
        title = {
            Text(
                text  = "Sair do aplicativo?",
                style = MaterialTheme.typography.titleLarge // ~20sp SemiBold (definido em Type.kt)
            )
        },

        // ─── CORPO DO DIÁLOGO ─────────────────────────────────────────────────
        text = {
            Text(
                text  = "Deseja encerrar o Menu Automático?",
                style = MaterialTheme.typography.bodyLarge // ~16sp Normal — texto corrido informativo
            )
        },

        // ─── BOTÃO DE CONFIRMAÇÃO (ação destrutiva) ───────────────────────────
        confirmButton = {
            // TextButton em vermelho reforça visualmente que esta é a ação destrutiva (encerrar o app)
            TextButton(
                onClick = onConfirmar,
                colors  = ButtonDefaults.textButtonColors(contentColor = ErrorColor) // texto vermelho
            ) {
                Text("Sair")
            }
        },

        // ─── BOTÃO DE CANCELAMENTO (ação segura) ──────────────────────────────
        dismissButton = {
            // TextButton em azul primário indica a ação segura (voltar ao menu)
            TextButton(
                onClick = onCancelar,
                colors  = ButtonDefaults.textButtonColors(contentColor = Primary) // texto azul primário
            ) {
                Text("Cancelar")
            }
        }
    )
}
