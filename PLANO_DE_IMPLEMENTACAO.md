# Plano de implementação — Case Técnico Mobile (KMP · Android + iOS)

> Prazo de entrega: **16/11 (segunda) às 23:59**. Hoje: 12/11.
> Este arquivo é o roteiro de construção. Não coloque nele o token da API nem a senha do Figma — o repositório é público.

---

## 1. Análise do case

### 1.1 O que é pedido

App de CRUD de dispositivos (**centrais de alarme** e **dispositivos de vídeo**) integrado a uma API REST, com **banco local como fonte única de verdade** e **favoritos apenas locais**.

| Área | Requisito | Tipo |
|---|---|---|
| Dashboard | Busca por nome, chips (Todos / Favoritos / Câmeras / Alarmes), card com ícone do tipo + nome + kebab | Obrigatório |
| Bottom Sheet | Editar, Informações, Marcar/Desmarcar favorito, Remover | Obrigatório |
| Adicionar / Editar | Formulário com validação e feedback; edição pré-preenchida | Obrigatório |
| Informações | Exibe os dados do dispositivo | Obrigatório |
| Favoritos | Só local; sobrevive a reabrir o app **e** a uma sincronização | Obrigatório |
| Banco local | UI observa o banco de forma reativa; resposta da API é gravada no banco; funciona offline (leitura) | Obrigatório |
| Backend | Listar, criar, atualizar, remover; Snackbar de sucesso/erro; estados de loading, vazio e erro | Obrigatório |
| Plataforma | Android minSdk 29 / target 36 / compile 36 · iOS 16.0 | Obrigatório |
| Arquitetura | MVVM com camadas separadas; Coroutines/Flow; Compose / SwiftUI | Obrigatório |
| Qualidade | Testes das regras de negócio; Conventional Commits; token fora do repositório; README completo | Obrigatório |
| Extras | DI, testes de UI/banco, fila offline, CI, **KMP**, tema claro/escuro, acessibilidade, modularização, ktlint/detekt/SwiftLint | Diferencial |

### 1.2 O que a API realmente oferece

Extraído do OpenAPI em `GET /api-json` (a doc Swagger fica em `/api`). Autenticação: `Authorization: Bearer <token>`.

| Recurso | Endpoints | Campos |
|---|---|---|
| `/alarm-centrals` | `GET` (lista), `POST`, `GET /{id}`, `PATCH /{id}`, `DELETE /{id}` | `id` (UUID), `name`, `macAddress`, `password` |
| `/video-devices` | idem | `id` (UUID), `name`, `serial`, `username`, `password` |

Detalhes que afetam o código:

- A listagem vem embrulhada: `{ "count": 1, "data": [ ... ] }`.
- **São dois recursos separados** — o app precisa unificá-los numa única lista de "dispositivos".
- `macAddress`: MAC válido **sem separadores** (12 hex, ex.: `1A2B3C4D5E6F`).
- Senha do alarme: **4 ou 6 dígitos numéricos**. Senha do vídeo: texto livre.
- `PATCH` exige **todos** os campos (o DTO de update marca tudo como required).
- Erros: `403` (token inválido/ausente), `404` (não existe), `409` (MAC ou serial duplicado).
- Não existe campo de favorito no servidor → confirma que favorito é 100% local.
- A API é **HTTP sem TLS** (`http://...:37025`) → exige liberar cleartext no Android e exceção de ATS no iOS, restritas a esse domínio.

### 1.3 Pontos de atenção

1. **O case pede UI nativa** (Compose *ou* SwiftUI). Por isso o KMP entra em dados, domínio e ViewModels — a UI **não** usa Compose Multiplatform. Assim os dois apps atendem ao enunciado e o KMP conta como diferencial.
2. **Fazer as duas plataformas é escopo além do pedido.** Ordem de trabalho: shared → Android completo → iOS. Se o tempo acabar, ainda existe uma entrega Android completa e coerente.
3. **Preservação de favoritos na sincronização** é pergunta explícita do enunciado — precisa de solução clara e de teste (seção 3.3).
4. O avaliador quer ouvir **trade-offs**. Anote as decisões no README à medida que forem tomadas, não no final.

---

## 2. Arquitetura

```
┌─────────────────────────┐      ┌─────────────────────────┐
│ androidApp              │      │ iosApp (Xcode)          │
│ Jetpack Compose         │      │ SwiftUI                 │
│ telas + componentes     │      │ telas + componentes     │
└───────────┬─────────────┘      └───────────┬─────────────┘
            │ StateFlow                      │ StateFlow → AsyncSequence (SKIE)
┌───────────▼────────────────────────────────▼─────────────┐
│ shared (KMP)                                             │
│  presentation  ViewModels + UiState + eventos (Snackbar) │
│  domain        Device (sealed), validadores, DeviceError │
│  data          DeviceRepository                          │
│                 ├─ remote: Ktor + kotlinx.serialization  │
│                 └─ local:  Room (KMP) + SQLite bundled   │
│  di            Koin                                      │
└──────────────────────────────────────────────────────────┘
```

Fluxo de dados (single source of truth):

```
UI ← ViewModel ← Repository.observeDevices() ← Room (Flow)
                                                  ▲
UI → ViewModel → Repository.refresh()/create()... ─┴→ API → grava no Room
```

A UI **nunca** renderiza a resposta da API diretamente; ela só reage ao que foi gravado no banco.

### 2.1 Bibliotecas

| Finalidade | Escolha | Por quê |
|---|---|---|
| Rede | **Ktor Client** (engine OkHttp no Android, Darwin no iOS) | Multiplataforma, `MockEngine` para testes |
| JSON | kotlinx.serialization | Padrão KMP |
| Banco | **Room KMP** + `sqlite-bundled` | É a opção citada no case, funciona em Android e iOS, expõe `Flow` |
| DI | **Koin** | Multiplataforma (Hilt é só Android) |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel` (KMP) | ViewModels compartilhados, uma única suíte de testes |
| Interop Swift | **SKIE** | `Flow` → `AsyncSequence`, `suspend` → `async`, sealed → enum exaustivo |
| Testes | kotlin.test, kotlinx-coroutines-test, Turbine, Ktor MockEngine | Rodam em `commonTest` |
| Qualidade | ktlint + detekt (Kotlin), SwiftLint (Swift) | Diferencial |
| CI | GitHub Actions | Diferencial |

> Versões (Fase 0 concluída): Kotlin 2.4.21, AGP 9.5.1, Gradle 9.9.1, SKIE 0.10.16 — combinação já validada compilando nos dois alvos.
>
> **Atenção ao `compileSdk = 36`:** as versões mais novas do AndroidX (core-ktx 1.19, activity 1.13, Compose BOM 2026.09) exigem compileSdk 37. Como o case fixa 36, as versões estão travadas em core-ktx 1.18.0, activity 1.12.4 e Compose BOM 2026.06.01. Ao adicionar lifecycle, navigation e Room, escolha a versão mais recente que ainda aceite compileSdk 36 (o erro de build diz exatamente qual dependência estourou). Vale citar no README como trade-off.
>
> O projeto Xcode é gerado por `xcodegen` a partir de `iosApp/project.yml` (o `.xcodeproj` também é versionado, para o avaliador não precisar da ferramenta).

### 2.2 Estrutura de pastas

```
IntelBras/
├── androidApp/                    # app Android (Compose)
│   └── src/main/kotlin/.../
│       ├── ui/theme/              # cores, tipografia (do Figma)
│       ├── ui/components/         # DeviceCard, FilterChips, SearchField, DeviceActionsSheet...
│       ├── ui/dashboard/          # DashboardScreen
│       ├── ui/form/               # DeviceFormScreen
│       ├── ui/details/            # DeviceDetailsScreen
│       └── navigation/
├── shared/
│   └── src/
│       ├── commonMain/kotlin/.../
│       │   ├── domain/model/      # Device, DeviceType, DeviceError
│       │   ├── domain/validation/ # DeviceValidator
│       │   ├── data/remote/       # DeviceApi, DTOs, mapeamento de erros
│       │   ├── data/local/        # AppDatabase, DAOs, entities
│       │   ├── data/repository/   # DeviceRepository(+Impl)
│       │   ├── presentation/      # DashboardViewModel, DeviceFormViewModel, DeviceDetailsViewModel
│       │   └── di/                # módulos Koin, initKoin(ApiConfig)
│       ├── androidMain/           # builder do Room, engine OkHttp
│       ├── iosMain/               # builder do Room, engine Darwin, helpers p/ Swift
│       └── commonTest/            # testes de regra de negócio
├── iosApp/                        # projeto Xcode (SwiftUI)
│   ├── Config/Secrets.example.xcconfig
│   └── iosApp/{Theme,Components,Dashboard,Form,Details}/
├── .github/workflows/ci.yml
├── local.properties               # NÃO versionado (token Android)
└── README.md
```

Modularização: `androidApp` / `shared` / `iosApp` já são módulos separados com fronteiras por pacote dentro do `shared`. Quebrar o `shared` em `:core:network`, `:core:database` etc. fica como melhoria futura no README (custo alto de configuração para o prazo).

---

## 3. Decisões de design que precisam estar certas

### 3.1 Modelo de domínio

```kotlin
enum class DeviceType { ALARM, VIDEO }

sealed interface Device {
    val id: String
    val name: String
    val isFavorite: Boolean
    val type: DeviceType

    data class Alarm(..., val macAddress: String, val password: String) : Device
    data class Video(..., val serial: String, val username: String, val password: String) : Device
}
```

### 3.2 Esquema do banco

```
devices                                   favorites
────────────────────────────              ─────────────────
id          TEXT  PK                      device_id TEXT PK
type        TEXT  (ALARM | VIDEO)
name        TEXT
mac_address TEXT? (alarme)
serial      TEXT? (vídeo)
username    TEXT? (vídeo)
password    TEXT
sync_state  TEXT  (SYNCED | PENDING_CREATE | PENDING_UPDATE | PENDING_DELETE)
```

- Uma tabela só para os dois tipos: a lista, a busca e os filtros são uma única query.
- `sync_state` entra desde o início (sempre `SYNCED` no MVP) para a fila offline da Fase 9 não exigir migração.
- A leitura é `devices LEFT JOIN favorites`, devolvendo `isFavorite`. O Room invalida o `Flow` quando qualquer uma das duas tabelas muda.

### 3.3 Como os favoritos sobrevivem à sincronização

**Favorito fica numa tabela própria, que a sincronização nunca escreve.** O sync só faz upsert/delete em `devices`; como não toca em `favorites`, não existe caminho de código capaz de apagar um favorito por engano.

Sincronização, numa única transação:

1. `GET /alarm-centrals` e `GET /video-devices` em paralelo (`async`).
2. Upsert de tudo o que veio em `devices`.
3. Remove de `devices` os ids `SYNCED` que não vieram mais (apagados no servidor) — **apenas dos tipos cuja chamada deu certo**. Se uma das duas falhar, o outro tipo não é apagado.
4. Remove de `favorites` os ids órfãos.

Trade-off para o README: a alternativa seria uma coluna `is_favorite` em `devices` com upsert parcial; funciona, mas depende de ninguém nunca usar `REPLACE` naquela tabela. Tabela separada torna a garantia estrutural.

### 3.4 Escrita (MVP): servidor primeiro, banco depois

`create/update/delete` chamam a API; em caso de sucesso gravam o retorno no Room (a UI atualiza sozinha) e emitem evento de sucesso; em caso de erro, emitem `DeviceError` e nada muda localmente. Simples e consistente. A Fase 9 troca isso por fila offline.

### 3.5 Erros

```kotlin
sealed interface DeviceError {
    data object Network; data object Timeout; data object Unauthorized   // 403
    data object NotFound                                                 // 404
    data object Conflict                                                 // 409 (MAC/serial duplicado)
    data class  Unknown(val code: Int?)
}
```

O mapeamento exceção/status → `DeviceError` fica em um único lugar (data/remote). Os ViewModels expõem o erro tipado; cada plataforma traduz para texto (strings.xml / Localizable.strings).

### 3.6 Validação (em `commonMain`, função pura)

| Campo | Regra |
|---|---|
| `name` | obrigatório (após `trim`) |
| `macAddress` | 12 caracteres hexadecimais; normalizar para maiúsculas e remover `:`/`-` antes de validar |
| senha do alarme | exatamente 4 ou 6 dígitos |
| `serial`, `username`, senha do vídeo | obrigatórios |

Na edição o **tipo não pode mudar** (são recursos diferentes na API).

### 3.7 Token e URL

O `shared` não lê configuração de build: cada app passa `ApiConfig(baseUrl, token)` para `initKoin`.

- **Android:** `local.properties` (`API_TOKEN=...`, `API_BASE_URL=...`) → `buildConfigField` → `BuildConfig`.
- **iOS:** `iosApp/Config/Secrets.xcconfig` (no `.gitignore`) → `Info.plist` → `Bundle.main`. Versionar apenas o `Secrets.example.xcconfig`. Atenção: em `.xcconfig`, `//` inicia comentário — escreva a URL como `http:/$()/host:porta`.
- **CI:** token fictício via variável de ambiente (os testes usam `MockEngine`).

### 3.8 Adaptações de iOS (justificar no README)

| Protótipo | iOS | Justificativa |
|---|---|---|
| Snackbar | Banner/toast próprio na parte inferior | Não há Snackbar nativo; mantém o feedback pedido |
| Bottom Sheet | `.sheet` + `presentationDetents` | Nativo no iOS 16 |
| Campo de busca | Campo próprio conforme o Figma (ou `.searchable`) | Decidir ao ver o protótipo |
| Navegação | `NavigationStack` | Padrão da plataforma, disponível no iOS 16 |
| Estado | `ObservableObject` + `@Published` | A macro `@Observable` exige iOS 17 |

---

## 4. Passo a passo

Cada fase termina com o app compilando e um ou mais commits em Conventional Commits. Só avance quando o critério de "pronto" for atendido.

### Fase 0 — Preparação (≈1 h)

1. Confirmar ferramentas: Android Studio atual com plugin KMP, Xcode instalado, JDK 17+. Opcional: `brew install kdoctor && kdoctor`.
2. Gerar o projeto em **kmp.jetbrains.com**: alvos Android + iOS, opção **"Do not share UI"** (UI nativa). Extrair nesta pasta.
3. Ajustar `minSdk = 29`, `targetSdk = 36`, `compileSdk = 36`; no Xcode, deployment target **iOS 16.0**.
4. `git init`, `.gitignore` cobrindo `local.properties`, `Secrets.xcconfig`, `build/`, `.gradle/`, `xcuserdata/`, `.idea/`.
5. Criar o repositório **público** e fazer o primeiro push.
6. Abrir o Figma e anotar cores, tipografia, espaçamentos, ícones e todos os estados de tela.
7. Com o token em mãos, testar a API: `curl -H "Authorization: Bearer $TOKEN" http://squadapps.ddns-intelbras.com.br:37025/video-devices`.

**Pronto quando:** o app vazio roda no emulador Android e no simulador iOS.
`chore: bootstrap kmp project with android and ios targets`

### Fase 1 — Configuração, token e DI (≈1–2 h)

1. Version catalog: Ktor, kotlinx.serialization, Room + KSP + `sqlite-bundled`, Koin, lifecycle-viewmodel, coroutines, Turbine, SKIE.
2. Android: ler `local.properties` → `BuildConfig.API_TOKEN` / `API_BASE_URL`; `network_security_config.xml` liberando cleartext **só** para `squadapps.ddns-intelbras.com.br`; permissão `INTERNET`.
3. iOS: `Secrets.example.xcconfig` + `Secrets.xcconfig`; chaves no `Info.plist`; exceção de ATS (`NSExceptionDomains`) só para o mesmo domínio.
4. `shared`: `ApiConfig`, `initKoin(config)`, chamado na `Application` (Android) e no `init` do `App` (iOS).

**Pronto quando:** os dois apps sobem com Koin inicializado e `git status` não mostra nenhum arquivo com token.
`build: add dependencies and version catalog` · `feat: load api config from untracked files`

### Fase 2 — Domínio (≈1–2 h, com TDD)

1. `Device`, `DeviceType`, `DeviceError`, `DeviceDraft` (dados do formulário).
2. `DeviceValidator` com os testes primeiro: nome vazio, MAC com separadores, MAC com 11/13 caracteres, senha de alarme com 5 dígitos ou letras, campos de vídeo vazios.

**Pronto quando:** `./gradlew :shared:allTests` passa.
`feat(domain): add device models` · `feat(domain): add device validation` · `test(domain): cover validation rules`

### Fase 3 — Camada remota (≈2 h)

1. DTOs (`AlarmCentralDto`, `VideoDeviceDto`, `ListResponse<T>`) e mapeadores DTO ↔ domínio.
2. `HttpClient`: `ContentNegotiation`, `HttpTimeout`, `defaultRequest` com base URL e header `Authorization`, `expectSuccess = true`.
3. `DeviceApi`: listar, criar, atualizar (PATCH com todos os campos) e remover, para os dois recursos.
4. Tradução de exceção/status → `DeviceError` em um único ponto.
5. Testes com `MockEngine`: desserialização do envelope `{count, data}`, 403 → `Unauthorized`, 409 → `Conflict`, timeout, falha de rede.

`feat(data): add ktor api client for devices` · `test(data): cover api error mapping`

### Fase 4 — Banco local (≈2–3 h)

1. `DeviceEntity`, `FavoriteEntity`, `DeviceDao`, `FavoriteDao`, `AppDatabase` (`@ConstructedBy` + builders em `androidMain`/`iosMain`, driver `BundledSQLiteDriver`).
2. KSP do Room configurado para **todos** os alvos (Android, iosArm64, iosSimulatorArm64); exportar o schema.
3. DAO: `observeDevices(): Flow<List<DeviceWithFavorite>>` (LEFT JOIN), `upsertAll`, `deleteSyncedNotIn(type, ids)`, `deleteById`, `setFavorite`/`removeFavorite`, `deleteOrphanFavorites`.
4. Método transacional `replaceFromServer(...)` implementando a seção 3.3.
5. Teste de integração com banco em memória: **favorito sobrevive ao `replaceFromServer`**; dispositivo removido no servidor some; favorito órfão é limpo.

**Pronto quando:** o teste de preservação de favoritos passa.
`feat(data): add room database with devices and favorites` · `test(data): verify favorites survive sync`

### Fase 5 — Repositório (≈2 h)

```kotlin
interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    fun observeDevice(id: String): Flow<Device?>
    suspend fun refresh(): Result<Unit, DeviceError>
    suspend fun create(draft: DeviceDraft): Result<Device, DeviceError>
    suspend fun update(id: String, draft: DeviceDraft): Result<Device, DeviceError>
    suspend fun delete(device: Device): Result<Unit, DeviceError>
    suspend fun setFavorite(id: String, favorite: Boolean)
}
```

Testes com API falsa + DAO falso (ou banco em memória): refresh grava no banco; falha de rede mantém os dados antigos; falha parcial não apaga o outro tipo; create/update/delete só alteram o banco após sucesso; favorito não gera chamada de rede.

`feat(data): add device repository with local-first sync` · `test(data): cover repository sync rules`

### Fase 6 — ViewModels compartilhados (≈3 h)

1. **`DashboardViewModel`**
   - `uiState: StateFlow<DashboardUiState>` = `combine(repository.observeDevices(), query, filter, syncStatus)`.
   - Estado: `devices`, `query`, `filter` (ALL/FAVORITES/CAMERAS/ALARMS), `isRefreshing`, e tela `Loading | Empty | EmptySearch | Content | Error` (erro de tela cheia só quando não há cache).
   - Ações: `onQueryChange`, `onFilterSelected`, `refresh`, `toggleFavorite`, `delete`.
   - `events: Flow<DashboardEvent>` (via `Channel`) para Snackbar — evento de disparo único, não estado.
   - Busca ignorando maiúsculas e acentos.
2. **`DeviceFormViewModel`** — modo criar/editar, estado por campo com erro, `submit()` valida → chama o repositório → emite `Saved` ou erro (409 vira erro no campo MAC/serial).
3. **`DeviceDetailsViewModel`** — observa um dispositivo por id.
4. Testes com Turbine + `StandardTestDispatcher`: combinações de filtro e busca, estados vazios, refresh com erro mantendo o cache, toggle de favorito, validação do formulário, pré-preenchimento na edição.

`feat(presentation): add dashboard viewmodel` · `feat(presentation): add device form viewmodel` · `test(presentation): cover viewmodels`

### Fase 7 — UI Android em Compose (≈5–6 h)

1. Tema a partir do Figma (claro/escuro), tipografia, espaçamentos.
2. Componentes reutilizáveis e *stateless*, com `@Preview`: `SearchField`, `FilterChipsRow`, `DeviceCard`, `DeviceActionsSheet` (`ModalBottomSheet`), `EmptyState`, `ErrorState`, `LoadingState`, `ConfirmDeleteDialog`, `LabeledTextField`.
3. `DashboardScreen`: `Scaffold` + `SnackbarHost`, pull-to-refresh, FAB "Adicionar", `LazyColumn` com `key = { it.id }`.
4. `DeviceFormScreen` (criar/editar) e `DeviceDetailsScreen`.
5. Navegação (Navigation Compose com rotas tipadas); ViewModels via `koinViewModel()`; estado coletado com `collectAsStateWithLifecycle()`.
6. Acabamento: `contentDescription`, alvos de toque ≥ 48dp, confirmação antes de remover, `animateItem()` na lista, teclado/IME adequado por campo, senha com alternância de visibilidade.
7. Passar o roteiro de verificação da seção 6 no emulador.

Commits pequenos por componente/tela: `feat(android): add device card component`, `feat(android): add dashboard screen`...

### Fase 8 — UI iOS em SwiftUI (≈5–6 h)

1. Aplicar SKIE; confirmar que `uiState` chega em Swift como `AsyncSequence` e os `sealed` como `enum`.
2. Um wrapper fino por tela: `final class DashboardModel: ObservableObject` que mantém o ViewModel KMP, publica o estado (`@Published`) dentro de um `.task { for await state in vm.uiState { ... } }` e repassa as ações. Liberar o ViewModel KMP (`onCleared`) quando a tela sair.
3. Componentes espelhando os do Android: `SearchField`, `FilterChipsRow`, `DeviceCard`, `DeviceActionsSheet`, `ToastBanner`, estados vazio/erro/carregando.
4. Telas: `DashboardView`, `DeviceFormView`, `DeviceDetailsView` com `NavigationStack`, `.refreshable`, `.confirmationDialog` para remover.
5. Acessibilidade (`accessibilityLabel`), Dynamic Type, modo escuro.
6. Passar o mesmo roteiro da seção 6 no simulador com iOS 16.

`feat(ios): add dashboard view` · `feat(ios): add device form view`...

### Fase 9 — Diferenciais, em ordem de custo/benefício

1. **CI (≈1 h)** — GitHub Actions: job `ubuntu` (ktlint, detekt, `:shared:allTests`/testes JVM, `assembleDebug`) e job `macos` (testes iOS do shared + `xcodebuild build`).
2. **ktlint + detekt + SwiftLint (≈1 h).**
3. **Testes de UI (≈1–2 h)** — um Compose UI Test do dashboard (filtro por chip + abrir bottom sheet) com repositório falso.
4. **Fila offline (≈4–6 h, só se sobrar tempo)** — escrita passa a ser local primeiro com `sync_state` pendente; um `SyncManager` processa a fila na abertura do app, no refresh e quando a conexão volta (`ConnectivityObserver` com `expect/actual`). Criação offline usa UUID local, trocado pelo id do servidor na mesma transação (migrando o favorito). Conflitos 404/409 marcam o item com erro para o usuário resolver. Se não der tempo, descreva esse desenho no README em "o que eu faria com mais tempo".

### Fase 10 — README e entrega (≈2 h, não deixar para o último minuto)

1. Como rodar (Android e iOS), incluindo a configuração do token passo a passo.
2. Diagrama de arquitetura e tabela de bibliotecas.
3. Decisões e trade-offs: KMP com UI nativa; Room KMP vs SQLDelight; ViewModels compartilhados + SKIE; tabela de favoritos separada; escrita servidor-primeiro; adaptações de iOS; cleartext restrito ao domínio; senhas de dispositivos em texto puro no banco local (limitação conhecida — melhoria: criptografia/Keystore/Keychain).
4. Pendências e melhorias.
5. GIFs/prints dos dois apps.
6. Checagem final: clonar o repositório em pasta limpa, seguir o README do zero e rodar os dois apps; `git log -p | grep -i <trecho do token>` para garantir que o token nunca foi commitado.

---

## 5. Cronograma sugerido

| Dia | Meta |
|---|---|
| Qui 12/11 | Fases 0–2 (projeto rodando, configuração, domínio) |
| Sex 13/11 | Fases 3–6 (todo o `shared` testado) |
| Sáb 14/11 | Fase 7 (Android completo) + CI e lint |
| Dom 15/11 | Fase 8 (iOS completo) |
| Seg 16/11 | Diferenciais restantes, README, revisão, entrega **antes** das 23:59 |

Regra de corte: se no domingo à noite o iOS não estiver estável, entregar Android completo + `shared` + iOS parcial documentado. O enunciado valoriza solução parcial bem explicada.

---

## 6. Roteiro de verificação manual (rodar nas duas plataformas)

- [ ] Primeira abertura online: loading → lista com alarmes e câmeras.
- [ ] Busca filtra por nome; busca sem resultado mostra estado vazio próprio.
- [ ] Chips: Todos, Favoritos, Câmeras, Alarmes — e combinados com a busca.
- [ ] Kebab abre a bottom sheet com as 4 ações; o texto alterna entre "Marcar favorito" e "Desmarcar favorito".
- [ ] Criar alarme e câmera válidos → Snackbar de sucesso e item na lista.
- [ ] Formulário inválido mostra erro por campo e não chama a API.
- [ ] Criar com MAC/serial duplicado → mensagem de conflito (409).
- [ ] Editar: campos pré-preenchidos, tipo bloqueado, alteração refletida na lista.
- [ ] Informações mostra todos os dados do dispositivo.
- [ ] Remover pede confirmação → Snackbar → item some.
- [ ] Favoritar, fechar e reabrir o app → continua favorito.
- [ ] Favoritar, puxar para atualizar → continua favorito.
- [ ] Modo avião: a lista continua aparecendo; refresh mostra erro de rede sem apagar a lista.
- [ ] Token inválido: mensagem específica de autenticação.
- [ ] Banco vazio + sem rede: estado de erro com "Tentar novamente".
- [ ] Tema escuro e leitor de tela (TalkBack / VoiceOver) nos fluxos principais.

---

## 7. Perguntas prováveis na apresentação

- **Por que KMP se o case pede nativo?** UI 100% nativa em cada plataforma; só regras, dados e ViewModels são compartilhados, com uma única suíte de testes.
- **Como os favoritos sobrevivem ao sync?** Seção 3.3.
- **Por que Room e não SQLDelight/Realm/Core Data?** Citado no case, `Flow` nativo, um esquema para as duas plataformas.
- **O que acontece se só um dos dois endpoints falhar?** Só o tipo que respondeu é reconciliado.
- **Por que escrita servidor-primeiro?** Consistência simples no prazo; a fila offline está desenhada (Fase 9) e o esquema já a suporta.
- **Riscos de compartilhar ViewModels?** Dependência do SKIE e do ciclo de vida no iOS; alternativa seria compartilhar até o repositório e escrever ViewModels em Swift.
