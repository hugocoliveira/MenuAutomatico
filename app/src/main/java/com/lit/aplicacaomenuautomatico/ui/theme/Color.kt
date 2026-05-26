package com.lit.aplicacaomenuautomatico.ui.theme

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — cor base do Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.ui.graphics.Color // representa cores ARGB; 0xFF = opacidade total

// ═════════════════════════════════════════════════════════════════════════════
// PALETA DE CORES DO APP
// Todas as cores são definidas aqui e referenciadas em Theme.kt e nos Composables.
// A convenção 0xFFRRGGBB: FF = alpha (opaco), RRGGBB = código hexadecimal da cor.
// ═════════════════════════════════════════════════════════════════════════════

// ─── Cor primária — azul #051FC7 ─────────────────────────────────────────────

/** Cor principal do app — azul escuro usado em TopAppBar, botões e destaques */
val Primary = Color(0xFF051FC7)

/** Container claro da cor primária — fundo de cards selecionados e chips */
val PrimaryContainer = Color(0xFFD6DCFF)

/** Texto e ícones sobre fundos da cor Primary (azul escuro) — branco para contraste */
val OnPrimary = Color(0xFFFFFFFF)

/** Texto e ícones sobre fundos PrimaryContainer (azul claro) — azul muito escuro */
val OnPrimaryContainer = Color(0xFF00105A)

// ─── Cor secundária ───────────────────────────────────────────────────────────

/** Azul médio — botões secundários e destaques complementares ao Primary */
val Secondary = Color(0xFF3A4480)

/** Texto e ícones sobre fundos Secondary — branco para contraste */
val OnSecondary = Color(0xFFFFFFFF)

// ─── Superfícies e fundos de tela ────────────────────────────────────────────

/** Fundo geral das telas — branco levemente azulado para suavidade visual */
val Surface = Color(0xFFF8F9FF)

/** Variante de superfície — fundo de cards e itens de lista */
val SurfaceVariant = Color(0xFFE8EAFF)

/** Texto principal sobre superfícies claras (Surface, SurfaceVariant) — quase preto */
val OnSurface = Color(0xFF0D0F1C)

/** Texto secundário e subtítulos sobre superfícies — cinza azulado */
val OnSurfaceVariant = Color(0xFF44475A)

/** Fundo geral da tela — igual ao Surface para consistência Material3 */
val Background = Color(0xFFF8F9FF)

/** Texto sobre o fundo geral — mesmo valor de OnSurface */
val OnBackground = Color(0xFF0D0F1C)

// ─── Estados de erro ──────────────────────────────────────────────────────────

/** Vermelho para mensagens de erro, bordas de campos inválidos e estados críticos */
val ErrorColor = Color(0xFFBA1A1A)

/** Texto e ícones sobre fundos de erro — branco para contraste com o vermelho */
val OnError = Color(0xFFFFFFFF)

// ─── Bordas e divisores ───────────────────────────────────────────────────────

/** Cinza azulado claro — usado em bordas de campos, divisores e contornos sutis */
val Outline = Color(0xFFC4C6D0)
