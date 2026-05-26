package com.lit.aplicacaomenuautomatico.ui.theme

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — tipografia Compose / Material3
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.Typography   // objeto que agrupa todos os estilos de texto do Material3
import androidx.compose.ui.text.TextStyle      // define fonte, peso, tamanho, espaçamento de uma variante
import androidx.compose.ui.text.font.FontFamily // família tipográfica (Default = fonte do sistema Android)
import androidx.compose.ui.text.font.FontWeight // peso da fonte: Normal (400), Medium (500), SemiBold (600), Bold (700)
import androidx.compose.ui.unit.sp             // unidade escalonável — tamanho de fonte em sp (respeita acessibilidade)

// ═════════════════════════════════════════════════════════════════════════════
// TIPOGRAFIA DO APP
// Fonte padrão do sistema (sem fonte customizada) para garantir legibilidade
// nos coletores industriais e evitar dependências de assets de fonte.
//
// Mapeamento de uso por variante:
//  - headlineMedium → título da tela / nome do menu na TopAppBar
//  - titleLarge     → título em diálogos
//  - titleMedium    → texto principal dos itens de menu (MenuItemCard)
//  - bodyLarge      → corpo de texto padrão
//  - bodySmall      → subtítulos, mensagens de erro, textos de suporte
//  - labelMedium    → labels de campos e rótulos de ícones
// ═════════════════════════════════════════════════════════════════════════════
val Typography = Typography(

    // Título de tela — ex.: "LIT Mobile RF" na LoginScreen e nome do menu na TopAppBar
    headlineMedium = TextStyle(
        fontFamily    = FontFamily.Default, // fonte do sistema Android (Roboto na maioria dos dispositivos)
        fontWeight    = FontWeight.Bold,    // negrito (700) — destaque máximo de hierarquia
        fontSize      = 28.sp,             // 28sp — tamanho grande para título principal
        lineHeight    = 36.sp,             // 36sp — espaçamento entre linhas proporcional ao tamanho
        letterSpacing = 0.sp               // sem espaçamento extra entre letras
    ),

    // Título grande — ex.: título de AlertDialog (diálogo de atualização, saída)
    titleLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.SemiBold, // semi-negrito (600) — hierarquia intermediária
        fontSize      = 20.sp,              // 20sp — título de diálogo
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),

    // Texto principal dos itens de menu e botão "Entrar"
    titleMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium, // médio (500) — legível mas não agressivo
        fontSize      = 16.sp,            // 16sp — tamanho padrão de texto primário
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp           // leve espaçamento para legibilidade em listas densas
    ),

    // Corpo de texto padrão — ex.: texto em diálogos de confirmação
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal, // normal (400) — sem destaque, texto corrido
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Texto secundário — ex.: subtítulo "SAP Warehouse Management", mensagens de erro
    bodySmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,  // 12sp — menor que o padrão, para informações de suporte
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label — ex.: rótulos de campos e chips
    labelMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    )
)
