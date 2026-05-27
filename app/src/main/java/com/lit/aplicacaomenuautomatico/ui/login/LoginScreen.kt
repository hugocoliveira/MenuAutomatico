package com.lit.aplicacaomenuautomatico.ui.login

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / broadcast
// ─────────────────────────────────────────────────────────────────────────────
import android.content.BroadcastReceiver   // escuta pacotes instalados pelo sistema
import android.content.Intent              // cria intents para broadcast e apps externos
import android.content.IntentFilter        // filtra quais broadcasts o receiver captura
import android.graphics.BitmapFactory      // decodifica a imagem fundo.png em memória

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Jetpack Compose (animação, layout, imagem, interação)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.animation.AnimatedVisibility   // exibe/oculta a mensagem de erro com animação
import androidx.compose.animation.fadeIn               // animação de entrada da mensagem de erro
import androidx.compose.animation.fadeOut              // animação de saída da mensagem de erro
import androidx.compose.foundation.Image               // componente para exibir imagens bitmap
import androidx.compose.foundation.background          // define cor/gradiente de fundo de um Box
import androidx.compose.foundation.border              // adiciona borda ao redor de um componente
import androidx.compose.foundation.layout.Arrangement  // controla alinhamento vertical/horizontal nos layouts
import androidx.compose.foundation.layout.Box          // container que empilha filhos em camadas (Z)
import androidx.compose.foundation.layout.Column       // empilha filhos verticalmente
import androidx.compose.foundation.layout.Row          // alinha filhos horizontalmente
import androidx.compose.foundation.layout.Spacer       // espaço vazio para separar componentes
import androidx.compose.foundation.layout.fillMaxSize  // modifier: ocupa 100% da largura e altura disponíveis
import androidx.compose.foundation.layout.fillMaxWidth // modifier: ocupa 100% da largura disponível
import androidx.compose.foundation.layout.height       // modifier: define altura fixa
import androidx.compose.foundation.layout.offset       // modifier: desloca o componente sem afetar o layout
import androidx.compose.foundation.layout.padding      // modifier: aplica margens internas (insets)
import androidx.compose.foundation.layout.size         // modifier: define largura e altura iguais (quadrado)
import androidx.compose.foundation.layout.width        // modifier: define largura fixa
import androidx.compose.foundation.shape.RoundedCornerShape // define cantos arredondados em campos e botões
import androidx.compose.foundation.text.KeyboardActions     // ações ao pressionar teclas do teclado (Next, Done)
import androidx.compose.foundation.text.KeyboardOptions     // tipo de teclado e botão de ação IME

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material Icons
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download      // ícone de download (app não instalado)
import androidx.compose.material.icons.filled.Lock          // ícone de cadeado no campo senha
import androidx.compose.material.icons.filled.Person        // ícone de pessoa no campo usuário
import androidx.compose.material.icons.filled.SystemUpdate  // ícone de atualização de sistema
import androidx.compose.material.icons.filled.Visibility    // ícone de olho aberto (senha visível)
import androidx.compose.material.icons.filled.VisibilityOff // ícone de olho fechado (senha oculta)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Componentes Material3
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.AlertDialog               // diálogo modal de atualização obrigatória
import androidx.compose.material3.Button                    // botão principal (Entrar / Baixar e instalar)
import androidx.compose.material3.ButtonDefaults            // customiza cores, elevation e shape do botão
import androidx.compose.material3.CircularProgressIndicator // spinner enquanto autentica ou verifica updates
import androidx.compose.material3.HorizontalDivider         // linha divisória entre itens no diálogo
import androidx.compose.material3.Icon                      // exibe ícones vetoriais Material
import androidx.compose.material3.IconButton                // botão de ícone (toggle visibilidade da senha)
import androidx.compose.material3.MaterialTheme             // acessa paleta de cores e tipografia do tema
import androidx.compose.material3.OutlinedTextField         // campo de texto com borda estilo Material3
import androidx.compose.material3.OutlinedTextFieldDefaults // customiza cores dos campos de texto
import androidx.compose.material3.Text                      // exibe textos (títulos, labels, mensagens)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose runtime e estado
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.runtime.Composable         // marca funções como Composable (UI declarativa)
import androidx.compose.runtime.DisposableEffect   // efeito com cleanup — registra/desregistra o BroadcastReceiver
import androidx.compose.runtime.LaunchedEffect     // efeito de corrotina — reage a mudanças de estado
import androidx.compose.runtime.getValue           // delegate para leitura de State<T>
import androidx.compose.runtime.mutableStateOf     // cria um estado mutável reativo
import androidx.compose.runtime.remember            // memoriza o valor entre recomposições
import androidx.compose.runtime.setValue           // delegate para escrita de State<T>

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Utilidades de UI
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.ui.Alignment                        // alinha filhos dentro do Box/Column/Row
import androidx.compose.ui.Modifier                         // encadeia modificadores de layout e aparência
import androidx.compose.ui.focus.FocusDirection             // direção de movimento do foco (Down = próximo campo)
import androidx.compose.ui.graphics.Color                   // define cores ARGB inline
import androidx.compose.ui.graphics.asImageBitmap           // converte android.graphics.Bitmap para ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter   // painter que exibe um ImageBitmap
import androidx.compose.ui.layout.ContentScale              // define como a imagem é escalada (Crop, Fit, etc.)
import androidx.compose.ui.platform.LocalContext            // acessa o Context atual (para assets, receivers)
import androidx.compose.ui.platform.LocalFocusManager       // gerencia foco entre campos de texto
import androidx.compose.ui.platform.LocalInspectionMode     // true em preview; false em runtime
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // oculta o teclado virtual
import androidx.compose.ui.res.painterResource              // carrega drawable por ID de recurso
import androidx.compose.ui.text.font.FontWeight             // define peso da fonte (Bold, SemiBold, etc.)
import androidx.compose.ui.text.input.ImeAction             // define o botão de ação do teclado (Next, Done)
import androidx.compose.ui.text.input.KeyboardType          // tipo do teclado (Text, Password, Number, etc.)
import androidx.compose.ui.text.input.PasswordVisualTransformation // máscara de senha (exibe bullets •)
import androidx.compose.ui.text.input.VisualTransformation  // transformação visual do campo (None = visível)
import androidx.compose.ui.tooling.preview.Preview          // anotação para preview no Android Studio
import androidx.compose.ui.unit.dp                          // unidade densidade-independente (dimensões)
import androidx.compose.ui.unit.sp                          // unidade escalonável (tamanho de fonte)
import androidx.lifecycle.compose.collectAsStateWithLifecycle // coleta StateFlow respeitando o ciclo de vida

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos e tema do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.R                         // acesso aos recursos do projeto (drawables, strings)
import com.lit.aplicacaomenuautomatico.ui.theme.AplicacaoMenuAutomaticoTheme // tema Material3 do app
import com.lit.aplicacaomenuautomatico.ui.theme.Primary          // cor primária (#051FC7) definida em Color.kt

/**
 * Tela de login do aplicativo.
 * É sempre a primeira tela exibida ao abrir o app (exceto modo offline com dados cached).
 *
 * Composable stateless — todo o estado vive no [LoginViewModel].
 * A tela coleta o [LoginUiState] via [collectAsStateWithLifecycle] para respeitar
 * o ciclo de vida e evitar coletas desnecessárias em background.
 *
 * @param viewModel ViewModel com a lógica de autenticação
 * @param onLoginSucesso Callback chamado após login bem-sucedido — navega para o Menu
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSucesso: () -> Unit
) {
    // ─── ESTADO REATIVO ───────────────────────────────────────────────────────
    // Coleta o StateFlow do ViewModel convertido em State<T> do Compose.
    // collectAsStateWithLifecycle garante que a coleta para quando o app vai para background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ─── ESTADO LOCAL DA UI ───────────────────────────────────────────────────
    // Estes três estados são locais à tela e não precisam sobreviver a rotações,
    // por isso ficam aqui em vez do ViewModel.
    // TODO: remover valores padrão antes do deploy em produção
    var usuario      by remember { mutableStateOf("ANDROID_API") } // texto digitado no campo usuário
    var senha        by remember { mutableStateOf("Lit@2026") }    // texto digitado no campo senha
    var senhaVisivel by remember { mutableStateOf(false) }         // controla máscara de senha

    // ─── UTILITÁRIOS DE FOCO E TECLADO ───────────────────────────────────────
    val focusManager       = LocalFocusManager.current              // move/limpa foco entre campos
    val keyboardController = LocalSoftwareKeyboardController.current // exibe/oculta teclado virtual
    val context            = LocalContext.current                   // necessário para assets e receivers

    // ─── IMAGEM DE FUNDO ──────────────────────────────────────────────────────
    // Em preview o AssetManager não tem acesso aos assets — carrega apenas em runtime.
    // O remember evita decodificar o bitmap a cada recomposição.
    val isPreview   = LocalInspectionMode.current
    val fundoBitmap = if (isPreview) null else remember {
        BitmapFactory.decodeStream(context.assets.open("fundo.png"))
    }

    // ─── EFEITO: NAVEGAÇÃO AO MENU ────────────────────────────────────────────
    // Observa mudanças de uiState. Quando muda para Sucesso, oculta o teclado,
    // limpa o foco e invoca o callback de navegação passado pelo host (MainActivity).
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Sucesso) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onLoginSucesso()
        }
    }

    // ─── DIÁLOGO DE ATUALIZAÇÃO OBRIGATÓRIA ──────────────────────────────────
    // Quando o ViewModel detecta que algum app precisa ser instalado/atualizado,
    // exibe este diálogo bloqueante (sem botão de fechar) antes de liberar o menu.
    if (uiState is LoginUiState.AtualizacaoObrigatoria) {
        val atualizacoes = (uiState as LoginUiState.AtualizacaoObrigatoria).atualizacoes

        // Monitora instalações de pacotes enquanto o diálogo estiver visível.
        // Ao detectar que um dos apps pendentes foi instalado/atualizado, dispara
        // re-verificação no ViewModel — se tudo estiver ok, o estado muda para Sucesso
        // e a navegação ao menu ocorre automaticamente.
        DisposableEffect(Unit) {
            // Receiver registrado apenas enquanto o diálogo está na tela.
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: android.content.Context, intent: Intent) {
                    // Extrai o package ID do app que acabou de ser instalado/atualizado
                    val packageInstalado = intent.data?.schemeSpecificPart ?: return
                    // Só re-verifica se o app instalado era um dos que estavam pendentes
                    if (atualizacoes.any { it.packageId == packageInstalado }) {
                        viewModel.reVerificarAposInstalacao()
                    }
                }
            }
            // Filtra apenas eventos de instalação e atualização de pacotes
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)     // app novo instalado
                addAction(Intent.ACTION_PACKAGE_REPLACED)  // app existente atualizado
                addDataScheme("package")                   // obrigatório para eventos de pacote
            }
            context.registerReceiver(receiver, filter)
            // Cleanup: desregistra o receiver quando o Composable sai da tela
            onDispose { context.unregisterReceiver(receiver) }
        }

        // Exibe o diálogo — o Composable abaixo define o conteúdo visual
        DialogAtualizacaoObrigatoria(
            atualizacoes = atualizacoes,
            onAtualizarClick = {
                // Dispara download de cada APK pendente via broadcast para o serviço de atualização
                atualizacoes.forEach { resultado ->
                    val intent = Intent("com.updater.lib.DOWNLOAD_APK").apply {
                        setPackage(context.packageName)                              // direciona ao updater interno
                        putExtra("apk_url",      resultado.updateInfo.apkUrl)        // URL do APK a baixar
                        putExtra("version_name", resultado.updateInfo.versionName)   // versão legível (ex: "1.5")
                        putExtra("version_code", resultado.updateInfo.versionCode)   // código numérico da versão
                        // Nome do app para o receiver criar arquivo com nome único e exibir notificação correta
                        putExtra("app_name", resultado.nomeApp)
                    }
                    context.sendBroadcast(intent)
                }
            }
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ESTRUTURA VISUAL PRINCIPAL
    // Box empilha camadas (Z-order): fundo → overlay → versão → conteúdo
    // ═════════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize()) { // ocupa toda a tela

        // ─── CAMADA 1: IMAGEM DE FUNDO ────────────────────────────────────────
        // Exibe fundo.png cobrindo toda a tela (ContentScale.Crop corta bordas se necessário).
        // Em preview fundoBitmap é null, então esta camada não é renderizada.
        if (fundoBitmap != null) {
            Image(
                painter       = BitmapPainter(fundoBitmap.asImageBitmap()),
                contentDescription = null,                   // decorativo — sem texto alternativo
                modifier      = Modifier.fillMaxSize(),      // estica a imagem para cobrir toda a tela
                contentScale  = ContentScale.Crop            // recorta bordas para preencher sem distorcer
            )
        }

        // ─── CAMADA 2: OVERLAY ESCURO ─────────────────────────────────────────
        // Semi-transparente sobre a imagem para aumentar o contraste dos textos brancos.
        // Em preview (sem fundo) usa azul escuro sólido como substituto visual.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 0x55000020 = preto/azulado com ~33% opacidade; em preview usa azul sólido
                .background(if (fundoBitmap != null) Color(0x55000020) else Color(0xFF0A1060))
        )

        // ─── CAMADA 3: VERSÃO DO APP (canto inferior) ────────────────────────
        // Exibida sobre todas as outras camadas, fixada na parte inferior central.
        Text(
            text  = "LIT Solutions  •  v${com.lit.aplicacaomenuautomatico.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), // labelSmall com 13sp fixo
            color = Color.White.copy(alpha = 0.55f),   // branco com 55% de opacidade (discreto)
            modifier = Modifier
                .align(Alignment.BottomCenter)          // ancora ao centro-inferior do Box pai
                .padding(bottom = 60.dp)                // margem inferior de 60dp da borda da tela
        )

        // ─── CAMADA 4: CONTEÚDO PRINCIPAL (logo + formulário) ────────────────
        // Column centraliza tudo horizontalmente e distribui verticalmente.
        // offset(y = -80.dp) sobe o formulário 80dp para posicionamento visual desejado.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-80).dp)                  // sobe a coluna 80dp (~2cm)
                .padding(horizontal = 32.dp),           // margem lateral de 32dp em cada lado
            horizontalAlignment = Alignment.CenterHorizontally, // centraliza todos os filhos horizontalmente
            verticalArrangement = Arrangement.Top              // alinha ao topo (Spacer.weight distribui o resto)
        ) {
            // Espaçador para descer apenas a logo 1,1cm (0,5cm anterior + 0,6cm solicitado)
            Spacer(modifier = Modifier.height(44.dp))

            // ─── LOGO ─────────────────────────────────────────────────────────
            // Carrega o drawable R.drawable.lit (arquivo lit.png/lit.xml em res/drawable).
            // Tamanho fixo de 234dp × 234dp (quadrado).
            Image(
                painter            = painterResource(id = R.drawable.lit),
                contentDescription = "Logo LIT Solutions",
                modifier           = Modifier.size(234.dp)  // largura e altura = 234dp
            )

            // Spacer elástico: empurra o bloco título+formulário para o centro-baixo da tela.
            // weight(1f) distribui todo o espaço vertical restante entre logo e formulário.
            Spacer(modifier = Modifier.weight(1f))

            // ─── TÍTULO PRINCIPAL ─────────────────────────────────────────────
            // headlineMedium = 28sp bold (definido em Type.kt / MaterialTheme.typography)
            Text(
                text       = "LIT Mobile RF",
                style      = MaterialTheme.typography.headlineMedium, // tamanho ~28sp
                fontWeight = FontWeight.Bold,                          // negrito
                color      = Color.White
            )

            // ─── SUBTÍTULO ────────────────────────────────────────────────────
            // bodySmall = 12sp; 70% de opacidade deixa visualmente hierárquico
            Text(
                text  = "SAP Warehouse Management",
                style = MaterialTheme.typography.bodySmall,    // tamanho ~12sp
                color = Color.White.copy(alpha = 0.7f)         // branco com 70% de opacidade
            )

            // Espaço de 10dp entre o subtítulo e o campo usuário
            Spacer(modifier = Modifier.height(10.dp))

            // ─── CAMPO USUÁRIO ────────────────────────────────────────────────
            OutlinedTextField(
                value       = usuario,
                onValueChange = {
                    usuario = it
                    // Limpa o estado de erro ao digitar, para feedback imediato ao usuário
                    if (uiState is LoginUiState.Erro) viewModel.limparErro()
                },
                label       = { Text("Usuário SAP") },         // label flutuante acima do campo
                leadingIcon = {
                    // Ícone de pessoa à esquerda do campo
                    Icon(
                        imageVector    = Icons.Default.Person,
                        contentDescription = "Usuário",
                        tint           = Color.White            // ícone branco
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,           // teclado alfanumérico padrão
                    imeAction    = ImeAction.Next               // botão "próximo" no teclado
                ),
                keyboardActions = KeyboardActions(
                    // Ao pressionar "Next", move o foco para o campo de senha (abaixo)
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,                             // impede quebra de linha
                // Campo ativo apenas nos estados Ocioso e Erro (bloqueado durante carregamento)
                enabled    = uiState is LoginUiState.Ocioso || uiState is LoginUiState.Erro,
                modifier   = Modifier.fillMaxWidth(),          // ocupa toda a largura disponível
                shape      = RoundedCornerShape(12.dp),        // cantos arredondados com raio de 12dp
                colors     = OutlinedTextFieldDefaults.colors(
                    // ── Cores quando o campo está com foco ──
                    focusedBorderColor    = Color(0xFF00E5FF), // borda ciano ao focar
                    focusedLabelColor     = Color(0xFF00E5FF), // label ciano ao focar
                    focusedTextColor      = Color.White,        // texto branco ao focar
                    focusedContainerColor = Color.White.copy(alpha = 0.08f), // fundo leve ao focar
                    // ── Cores quando o campo não está com foco ──
                    unfocusedBorderColor    = Color.White.copy(alpha = 0.5f),  // borda branca 50%
                    unfocusedLabelColor     = Color.White.copy(alpha = 0.7f),  // label branco 70%
                    unfocusedTextColor      = Color.White,                      // texto branco
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f), // fundo leve
                    cursorColor             = Color(0xFF00E5FF), // cursor ciano
                )
            )

            // Espaço de 10dp entre campo usuário e campo senha
            Spacer(modifier = Modifier.height(10.dp))

            // ─── CAMPO SENHA ──────────────────────────────────────────────────
            OutlinedTextField(
                value       = senha,
                onValueChange = {
                    senha = it
                    // Limpa o estado de erro ao digitar
                    if (uiState is LoginUiState.Erro) viewModel.limparErro()
                },
                label       = { Text("Senha") },
                leadingIcon = {
                    // Ícone de cadeado à esquerda do campo
                    Icon(
                        imageVector    = Icons.Default.Lock,
                        contentDescription = "Senha",
                        tint           = Color.White
                    )
                },
                trailingIcon = {
                    // Botão à direita que alterna a visibilidade da senha
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            // Olho aberto quando senha visível, olho fechado quando oculta
                            imageVector    = if (senhaVisivel) Icons.Default.Visibility
                                             else Icons.Default.VisibilityOff,
                            contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                            tint           = Color.White.copy(alpha = 0.7f) // branco 70%
                        )
                    }
                },
                // PasswordVisualTransformation substitui caracteres por bullets (•••)
                // VisualTransformation.None exibe o texto original sem máscara
                visualTransformation = if (senhaVisivel) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,       // teclado de senha (sem sugestões)
                    imeAction    = ImeAction.Done               // botão "concluir" no teclado
                ),
                keyboardActions = KeyboardActions(
                    // Ao pressionar "Done": fecha o teclado e inicia o login
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login(usuario, senha)
                    }
                ),
                singleLine = true,
                enabled    = uiState is LoginUiState.Ocioso || uiState is LoginUiState.Erro,
                modifier   = Modifier.fillMaxWidth(),
                shape      = RoundedCornerShape(12.dp),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color(0xFF00E5FF),
                    focusedLabelColor       = Color(0xFF00E5FF),
                    focusedTextColor        = Color.White,
                    focusedContainerColor   = Color.White.copy(alpha = 0.08f),
                    unfocusedBorderColor    = Color.White.copy(alpha = 0.5f),
                    unfocusedLabelColor     = Color.White.copy(alpha = 0.7f),
                    unfocusedTextColor      = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    cursorColor             = Color(0xFF00E5FF),
                )
            )

            // Espaço de 4dp entre o campo senha e a mensagem de erro (compacto)
            Spacer(modifier = Modifier.height(4.dp))

            // ─── MENSAGEM DE ERRO ─────────────────────────────────────────────
            // AnimatedVisibility exibe com fadeIn/fadeOut — aparece suavemente ao errar login.
            // Invisível nos demais estados (Ocioso, Carregando, Sucesso).
            AnimatedVisibility(
                visible = uiState is LoginUiState.Erro,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Text(
                    text  = (uiState as? LoginUiState.Erro)?.mensagem ?: "",
                    color = MaterialTheme.colorScheme.error,    // cor de erro do tema (vermelho)
                    style = MaterialTheme.typography.bodySmall,  // fonte pequena (~12sp)
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)             // pequena margem interna lateral
                )
            }

            // Espaço de 14dp entre mensagem de erro e o botão/spinner
            Spacer(modifier = Modifier.height(14.dp))

            // ─── ÁREA DE AÇÃO: SPINNER OU BOTÃO ──────────────────────────────
            // Durante Carregando ou VerificandoAtualizacoes: exibe spinner + texto de status.
            // Nos demais estados: exibe o botão "Entrar".
            if (uiState is LoginUiState.Carregando || uiState is LoginUiState.VerificandoAtualizacoes) {

                // ─── INDICADOR DE CARREGAMENTO ────────────────────────────────
                CircularProgressIndicator(
                    color    = Color(0xFF00E5FF),    // spinner ciano para contraste com o fundo escuro
                    modifier = Modifier.size(48.dp)  // spinner de 48dp × 48dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Texto de status diferenciado conforme a etapa atual
                Text(
                    text  = if (uiState is LoginUiState.VerificandoAtualizacoes)
                                "Verificando atualizações..."
                            else
                                "Autenticando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

            } else {

                // ─── BOTÃO ENTRAR ─────────────────────────────────────────────
                // Aparência: transparente com borda branca e texto branco.
                // O efeito "ghost button" é criado por:
                //   1. containerColor = Transparent → sem preenchimento de fundo
                //   2. .border(...)                 → borda branca visível
                //   3. elevation = 0.dp             → sem sombra
                Button(
                    onClick = {
                        focusManager.clearFocus()          // fecha o teclado antes de submeter
                        viewModel.login(usuario, senha)    // delega autenticação ao ViewModel
                    },
                    modifier = Modifier
                        .fillMaxWidth()                    // botão ocupa toda a largura do formulário
                        .height(52.dp)                    // altura fixa de 52dp (~1,4cm em 160dpi)
                        .border(
                            width  = 1.5.dp,              // espessura da borda branca
                            color  = Color.White,
                            shape  = RoundedCornerShape(12.dp) // cantos arredondados iguais ao campo
                        ),
                    shape  = RoundedCornerShape(12.dp),   // cantos arredondados do botão em si
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, // FUNDO TRANSPARENTE — botão fantasma
                        contentColor   = Color.White        // cor padrão do conteúdo interno
                    ),
                    // elevation(0.dp) remove a sombra padrão do botão Material3
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation  = 0.dp,
                        pressedElevation  = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(
                        text       = "Entrar",
                        style      = MaterialTheme.typography.titleMedium, // ~16sp
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            // Espaçador fixo de 334dp no final da Column — empurra o conteúdo para cima
            // deixando espaço livre na parte inferior da tela (reserva visual para teclado)
            Spacer(modifier = Modifier.height(334.dp))
        }
    }
}

/**
 * Diálogo obrigatório exibido quando um ou mais apps precisam ser instalados ou atualizados.
 * Não possui botão de cancelar — o operador deve resolver todas as pendências antes de
 * acessar o menu.
 *
 * Diferencia visualmente dois casos:
 *  - App instalado com atualização disponível → ícone SystemUpdate
 *  - App não instalado                        → ícone Download com texto "Não instalado"
 *
 * @param atualizacoes Lista de apps com pendências (atualização ou instalação)
 * @param onAtualizarClick Callback acionado ao pressionar o botão — inicia todos os downloads
 */
@Composable
private fun DialogAtualizacaoObrigatoria(
    atualizacoes: List<AppUpdateResult>,
    onAtualizarClick: () -> Unit
) {
    // Estado local: true após o primeiro clique em "Baixar e instalar"
    // Impede double-tap e muda o texto do botão para "Aguardando instalação..."
    var downloadIniciado by remember { mutableStateOf(false) }

    // ─── TÍTULO E SUBTÍTULO DINÂMICOS ────────────────────────────────────────
    // Calculados uma vez com base na lista — sem lógica de negócio no Composable.
    val temNaoInstalado = atualizacoes.any { !it.estaInstalado }  // ao menos um não instalado
    val temAtualizacao  = atualizacoes.any {  it.estaInstalado }  // ao menos um instalado desatualizado
    val titulo = when {
        temNaoInstalado && temAtualizacao -> "Apps pendentes"
        temNaoInstalado                  -> "Instalação necessária"
        else                             -> "Atualização obrigatória"
    }
    val subtitulo = when {
        temNaoInstalado && temAtualizacao ->
            "Os aplicativos abaixo precisam ser instalados ou atualizados antes de continuar:"
        temNaoInstalado ->
            "Os aplicativos abaixo não estão instalados. Baixe-os para continuar:"
        else ->
            "Os aplicativos abaixo precisam ser atualizados antes de continuar:"
    }

    AlertDialog(
        // Sem dismiss: toca fora ou pressiona Back não fecha o diálogo (ação obrigatória)
        onDismissRequest = { /* bloqueado intencionalmente — atualização obrigatória */ },

        // ─── ÍCONE DO DIÁLOGO ─────────────────────────────────────────────────
        icon = {
            Icon(
                imageVector    = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint           = Primary,              // cor primária do app (#051FC7)
                modifier       = Modifier.size(32.dp)  // ícone de 32dp no topo do diálogo
            )
        },

        // ─── TÍTULO DO DIÁLOGO ────────────────────────────────────────────────
        title = {
            Text(
                text       = titulo,
                style      = MaterialTheme.typography.titleLarge, // ~22sp
                fontWeight = FontWeight.Bold,
                color      = Primary
            )
        },

        // ─── CORPO DO DIÁLOGO ─────────────────────────────────────────────────
        text = {
            Column {
                // Subtítulo explicativo
                Text(
                    text  = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,           // ~14sp
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Lista de apps pendentes com separadores entre eles
                atualizacoes.forEachIndexed { index, resultado ->
                    // Adiciona divisor entre itens (não antes do primeiro)
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    // Linha: ícone + nome do app + versão disponível
                    Row(
                        verticalAlignment = Alignment.CenterVertically, // centraliza ícone com texto
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        // Ícone diferente: SystemUpdate se instalado (atualização), Download se ausente
                        Icon(
                            imageVector = if (resultado.estaInstalado)
                                Icons.Default.SystemUpdate  // atualização de versão existente
                            else
                                Icons.Default.Download,     // instalação do zero
                            contentDescription = null,
                            tint           = Primary,
                            modifier       = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // espaço entre ícone e texto

                        // Coluna com nome do app e versão disponível
                        Column {
                            Text(
                                text       = resultado.nomeApp,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                // Exibe "Atualização X.X" ou "Não instalado — versão X.X disponível"
                                text  = if (resultado.estaInstalado)
                                    "Atualização ${resultado.updateInfo.versionName} disponível"
                                else
                                    "Não instalado — versão ${resultado.updateInfo.versionName} disponível",
                                style = MaterialTheme.typography.bodySmall, // ~12sp
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mensagem de confirmação exibida após clicar em "Baixar e instalar"
                if (downloadIniciado) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text       = "Download iniciado. Instale os apps e faça login novamente.",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },

        // ─── BOTÃO DE CONFIRMAÇÃO ─────────────────────────────────────────────
        confirmButton = {
            Button(
                onClick = {
                    // Evita múltiplos cliques: só executa no primeiro toque
                    if (!downloadIniciado) {
                        downloadIniciado = true
                        onAtualizarClick() // dispara downloads no LoginScreen pai
                    }
                },
                enabled = !downloadIniciado,                  // desabilita após o primeiro clique
                colors  = ButtonDefaults.buttonColors(containerColor = Primary), // fundo azul primário
                shape   = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text  = if (downloadIniciado) "Aguardando instalação..." else "Baixar e instalar",
                    color = Color.White
                )
            }
        }
    )
}

/**
 * Preview da tela de login — usa fundo azul escuro como substituto da imagem de assets
 * (assets não estão disponíveis no ambiente de preview do Android Studio).
 * Dimensões baseadas no coletor Zebra MC3300 (480×800dp).
 */
@Preview(name = "Login — MC3300", showBackground = true, widthDp = 480, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    AplicacaoMenuAutomaticoTheme {
        // Substitui a imagem de fundo por um Box azul escuro sólido (assets indisponíveis em preview)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A1060)) // azul escuro substituto do fundo.png
        ) {
            // Texto de versão — mesmo posicionamento do runtime
            Text(
                text  = "LIT Solutions  •  v1.18",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-80).dp)          // mesmo deslocamento do runtime
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Espaçador para descer apenas a logo 1,1cm (0,5cm anterior + 0,6cm solicitado)
                Spacer(modifier = Modifier.height(44.dp))

                // Logo — mesmo tamanho do runtime (234dp)
                Image(
                    painter            = painterResource(id = R.drawable.lit),
                    contentDescription = null,
                    modifier           = Modifier.size(234.dp) // tamanho da logo
                )

                // Espaço elástico entre logo e formulário
                Spacer(modifier = Modifier.weight(1f))

                // Título e subtítulo
                Text(
                    text       = "LIT Mobile RF",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text  = "SAP Warehouse Management",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(50.dp))

                // Campo usuário (somente leitura em preview)
                OutlinedTextField(
                    value         = "ANDROID_API",
                    onValueChange = {},
                    label         = { Text("Usuário SAP") },
                    leadingIcon   = { Icon(Icons.Default.Person, null, tint = Color.White) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor    = Color.White.copy(alpha = 0.5f),
                        unfocusedLabelColor     = Color.White.copy(alpha = 0.7f),
                        unfocusedTextColor      = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Campo senha (somente leitura em preview)
                OutlinedTextField(
                    value         = "••••••••",
                    onValueChange = {},
                    label         = { Text("Senha") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                    trailingIcon  = {
                        Icon(Icons.Default.VisibilityOff, null, tint = Color.White.copy(alpha = 0.7f))
                    },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor    = Color.White.copy(alpha = 0.5f),
                        unfocusedLabelColor     = Color.White.copy(alpha = 0.7f),
                        unfocusedTextColor      = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Botão transparente com borda branca (ghost button) — estado visual apenas
                Button(
                    onClick   = {},
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.5.dp, Color.White, RoundedCornerShape(12.dp)),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, // fundo transparente
                        contentColor   = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp) // sem sombra
                ) {
                    Text(
                        text       = "Entrar",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                // Reserva de espaço inferior — mesmo valor do runtime
                Spacer(modifier = Modifier.height(334.dp))
            }
        }
    }
}
