package com.lit.aplicacaomenuautomatico.ui.menu.components

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose (layout e interação)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.foundation.clickable           // torna o Card clicável com efeito ripple
import androidx.compose.foundation.layout.Column       // empilha os textos verticalmente
import androidx.compose.foundation.layout.Row          // alinha ícone e textos horizontalmente
import androidx.compose.foundation.layout.Spacer       // espaço horizontal entre texto e ícone
import androidx.compose.foundation.layout.fillMaxWidth // modifier: ocupa 100% da largura do item de lista
import androidx.compose.foundation.layout.padding      // modifier: margem interna do Row (conteúdo do card)
import androidx.compose.foundation.layout.size         // modifier: define tamanho fixo do ícone (20dp)
import androidx.compose.foundation.layout.width        // modifier: define largura do Spacer entre texto e ícone
import androidx.compose.foundation.shape.RoundedCornerShape // cantos arredondados do Card

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material Icons
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos // seta direita (submenu)
import androidx.compose.material.icons.filled.OpenInNew                    // ícone de abrir app externo

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material3 (componentes visuais)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.Card         // container com elevação, fundo e cantos arredondados
import androidx.compose.material3.CardDefaults // cores e elevação padrão do Card
import androidx.compose.material3.Icon         // exibe ícone vetorial Material
import androidx.compose.material3.MaterialTheme // acessa a tipografia do tema
import androidx.compose.material3.Text         // exibe o texto do item de menu

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose runtime
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.runtime.Composable // marca a função como Composable
import androidx.compose.ui.Alignment       // alinha ícone e texto verticalmente no centro do Row
import androidx.compose.ui.Modifier        // encadeia modificadores de layout e aparência
import androidx.compose.ui.unit.dp         // unidade densidade-independente (dp)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — modelo e tema do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp    // modelo de domínio do item de menu
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurface      // texto principal — quase preto
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurfaceVariant // texto secundário — cinza azulado
import com.lit.aplicacaomenuautomatico.ui.theme.Primary         // azul #051FC7 — cor dos ícones
import com.lit.aplicacaomenuautomatico.ui.theme.SurfaceVariant  // azul claro — fundo do card

// ═════════════════════════════════════════════════════════════════════════════
// COMPOSABLE: MenuItemCard
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Card que representa um único item de menu na lista.
 * Composable stateless — recebe dados via parâmetros e emite evento via lambda.
 *
 * Aparência:
 *  - Fundo [SurfaceVariant] (azul muito claro) com elevação de 2dp
 *  - Cantos arredondados de 12dp para visual moderno
 *  - Texto principal à esquerda e ícone de tipo à direita
 *
 * Ícone diferenciado por tipo de item:
 *  - Type = "1" (submenu) → ArrowForwardIos (seta direita) — indica que há filhos para navegar
 *  - Type = "2" (ação)    → OpenInNew — indica que abre um app externo
 *
 * @param item    Dados do item de menu a exibir (texto, tipo, código)
 * @param onClick Callback chamado ao tocar o card — interpretado pelo MenuScreen
 */
@Composable
fun MenuItemCard(
    item: MenuApp,
    onClick: () -> Unit
) {
    // ─── CARD (container principal) ───────────────────────────────────────────
    Card(
        modifier = Modifier
            .fillMaxWidth()           // card ocupa toda a largura da coluna da lista
            .clickable(onClick = onClick), // toda a área do card é clicável (com ripple)
        shape  = RoundedCornerShape(12.dp), // cantos arredondados de 12dp — igual aos campos e botões
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant // fundo azul muito claro (#E8EAFF)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp // sombra sutil que diferencia o card do fundo da tela
        )
    ) {
        // ─── ROW: ícone à esquerda dos textos ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp), // margens internas do conteúdo do card
            verticalAlignment = Alignment.CenterVertically       // centraliza ícone com texto verticalmente
        ) {
            // ─── COLUNA DE TEXTOS ─────────────────────────────────────────────
            // weight(1f): a coluna ocupa todo o espaço horizontal restante (empurra ícone à direita)
            Column(modifier = Modifier.weight(1f)) {
                // Texto principal: campo Text do OData (retorna em português com sap-language=PT).
                // ifBlank usa SText como fallback caso Text venha vazio do SAP.
                Text(
                    text  = item.text.ifBlank { item.sText }, // fallback: SText se Text vazio
                    style = MaterialTheme.typography.titleMedium, // 16sp Medium — texto principal
                    color = OnSurface                             // quase preto (#0D0F1C)
                )
            }

            // Espaço horizontal de 12dp entre o bloco de texto e o ícone
            Spacer(modifier = Modifier.width(12.dp))

            // ─── ÍCONE DE TIPO ────────────────────────────────────────────────
            // Diferencia visualmente submenus de ações — o operador reconhece instantaneamente
            // o comportamento do item antes de tocar.
            Icon(
                imageVector = if (item.isSubmenu) {
                    Icons.AutoMirrored.Filled.ArrowForwardIos // seta → indica filhos abaixo
                } else {
                    Icons.Default.OpenInNew // janela → indica abertura de app externo
                },
                contentDescription = if (item.isSubmenu) "Abrir submenu" else "Lançar aplicativo",
                tint     = Primary,               // ícone azul #051FC7 — destaque sobre fundo claro
                modifier = Modifier.size(20.dp)   // ícone de 20dp × 20dp (compacto, não domina o card)
            )
        }
    }
}
