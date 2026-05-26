package com.lit.aplicacaomenuautomatico.ui.menu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurface
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurfaceVariant
import com.lit.aplicacaomenuautomatico.ui.theme.Primary
import com.lit.aplicacaomenuautomatico.ui.theme.SurfaceVariant

/**
 * Card que representa um item de menu na lista.
 * Composable stateless — recebe os dados e um callback, sem estado interno.
 *
 * Ícone diferenciado por tipo:
 *  - Type=1 (submenu): seta para direita → indica que há filhos para navegar
 *  - Type=2 (ação): ícone de abrir app externo → indica lançamento de aplicativo
 *
 * @param item Dados do item de menu a exibir
 * @param onClick Callback chamado ao tocar no card
 */
@Composable
fun MenuItemCard(
    item: MenuApp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coluna de texto com título e subtítulo do item
            Column(modifier = Modifier.weight(1f)) {
                // Texto principal: campo Text do OData (com sap-language=PT retorna em português).
                // SText usado como fallback caso Text venha vazio.
                Text(
                    text = item.text.ifBlank { item.sText },
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Ícone indica o comportamento do item ao ser tocado
            Icon(
                imageVector = if (item.isSubmenu) {
                    // Submenu: seta para a direita indica que há mais itens abaixo
                    Icons.AutoMirrored.Filled.ArrowForwardIos
                } else {
                    // Ação: ícone de "abrir em novo" indica lançamento de app externo
                    Icons.Default.OpenInNew
                },
                contentDescription = if (item.isSubmenu) "Abrir submenu" else "Lançar aplicativo",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
