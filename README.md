# Dispositivos — Case Técnico Mobile

App para listar, consultar, adicionar, editar, remover e favoritar **dispositivos de vídeo** e **centrais de alarme**, integrado à API do desafio.

Foi construído para **Android e iOS** com **Kotlin Multiplatform (KMP)**: as regras de negócio, o acesso a dados e os ViewModels existem uma única vez, no módulo `shared`; a interface é nativa em cada plataforma — **Jetpack Compose** no Android e **SwiftUI** no iOS.

[![CI](https://github.com/RafaelNicolauiOS/intelbras-mobile-challenge/actions/workflows/ci.yml/badge.svg)](https://github.com/RafaelNicolauiOS/intelbras-mobile-challenge/actions/workflows/ci.yml)

## Sumário

- [Como rodar](#como-rodar)
- [Arquitetura](#arquitetura)
- [Bibliotecas](#bibliotecas)
- [Como os favoritos sobrevivem à sincronização](#como-os-favoritos-sobrevivem-à-sincronização)
- [Decisões técnicas e trade-offs](#decisões-técnicas-e-trade-offs)
- [Testes e CI](#testes-e-ci)
- [O que ficou pendente e o que eu melhoraria](#o-que-ficou-pendente-e-o-que-eu-melhoraria)

## Como rodar

### Pré-requisitos

| Ferramenta | Versão usada no desenvolvimento |
|---|---|
| JDK | 17 |
| Android SDK | plataforma 36 (minSdk 29, targetSdk 36, compileSdk 36) |
| Xcode | 27.2 (deployment target iOS 16.0) |

O Gradle é baixado pelo wrapper (`./gradlew`); não é preciso instalá-lo.

### 1. Configurar o token da API

O token **não é versionado**. Ele fica em arquivos ignorados pelo git, um por plataforma.

**Android** — crie (ou edite) o arquivo `local.properties` na raiz do projeto, usando [local.properties.example](local.properties.example) como modelo:

```properties
API_BASE_URL=http://squadapps.ddns-intelbras.com.br:37025
API_TOKEN=cole-o-token-aqui
```

O Android Studio costuma criar esse arquivo sozinho com a linha `sdk.dir=...`; basta acrescentar as duas linhas acima.

**iOS** — copie o arquivo de exemplo e cole o token:

```bash
cp iosApp/Config/Secrets.example.xcconfig iosApp/Config/Secrets.xcconfig
```

Depois edite `iosApp/Config/Secrets.xcconfig`:

```
API_TOKEN = cole-o-token-aqui
```

Sem token (ou com token inválido) o app abre normalmente e mostra a mensagem "Token de acesso inválido ou expirado.".

### 2. Android

Abra a pasta do projeto no Android Studio e rode a configuração `androidApp`, ou pela linha de comando:

```bash
./gradlew :androidApp:installDebug
```

### 3. iOS

Abra `iosApp/iosApp.xcodeproj` no Xcode, escolha um simulador e rode o esquema `iosApp`. O próprio build do Xcode compila o módulo Kotlin (`shared`) antes do app; a primeira compilação demora alguns minutos.

O projeto Xcode é gerado a partir de [iosApp/project.yml](iosApp/project.yml) com o [XcodeGen](https://github.com/yonaskolb/XcodeGen). O `.xcodeproj` também está versionado, então **não é preciso instalar o XcodeGen** para rodar — só para alterar a estrutura do projeto (`cd iosApp && xcodegen generate`).

### 4. Testes

```bash
./gradlew :shared:allTests
```

Roda os testes do módulo compartilhado na JVM (alvo Android) e no simulador iOS.

## Arquitetura

**MVVM + Coordinator**, com o banco local como **fonte única de verdade**.

```
┌──────────────────────────┐        ┌──────────────────────────┐
│ androidApp               │        │ iosApp                   │
│  view/        Compose    │        │  View/          SwiftUI  │
│  coordinator/ NavHost    │        │  ViewController/ hosting │
│                          │        │  Coordinator/   UIKit nav│
│                          │        │  ViewModel/     ponte    │
└────────────┬─────────────┘        └────────────┬─────────────┘
             │ StateFlow                         │ StateFlow → AsyncSequence (SKIE)
┌────────────▼───────────────────────────────────▼─────────────┐
│ shared (Kotlin Multiplatform)                                │
│  viewmodel/   DashboardViewModel, DeviceFormViewModel,       │
│               DeviceDetailsViewModel, MessageCenter          │
│  models/      Device, DeviceDraft, DeviceValidator,          │
│               DeviceFilter, DeviceError                      │
│  repository/  DeviceRepository                               │
│     remote/   Ktor (API REST)                                │
│     local/    Room (SQLite)                                  │
│  di/          AppContainer (composição manual)               │
└──────────────────────────────────────────────────────────────┘
```

### Fluxo de dados

```
Tela ◄── ViewModel ◄── Repository.observeDevices() ◄── Room (Flow)
                                                          ▲
Tela ──► ViewModel ──► Repository.refresh()/create()… ──► API ──► grava no Room
```

A tela **nunca** mostra a resposta da API diretamente. Toda leitura vem de um `Flow` do banco; toda operação termina gravando no banco, e a tela reage sozinha. Por isso o app continua mostrando os últimos dispositivos sincronizados quando está sem conexão.

### Papel de cada camada

| Camada | Onde | Responsabilidade |
|---|---|---|
| **View** | `androidApp/view`, `iosApp/View` | Renderiza o estado e avisa o que o usuário fez por callbacks. Não navega e não tem regra de negócio. |
| **ViewController** | `iosApp/ViewController` | Um `UIHostingController` por tela: hospeda a view SwiftUI e cuida da barra de navegação. No Android esse papel é da `MainActivity`. |
| **Coordinator** | `androidApp/coordinator`, `iosApp/Coordinator` | Único lugar que decide para onde navegar e que monta cada tela com seu ViewModel. |
| **ViewModel** | `shared/viewmodel` | Estado de cada tela (`StateFlow`) e reação às ações. Compartilhado entre as plataformas. |
| **Repository** | `shared/repository` | Único ponto de acesso a dados; coordena API e banco. |
| **Models** | `shared/models` | Entidades e regras puras: validação, filtro e busca. |

No iOS, a pasta `ViewModel/` contém apenas **pontes** (`*Observable`): classes `ObservableObject` que republicam o `StateFlow` do ViewModel Kotlin como `@Published` e repassam as ações. Não há regra de negócio em Swift.

### Regras de negócio iguais nas duas plataformas

Como validação, filtros, busca, sincronização e favoritos estão no `shared`, Android e iOS executam exatamente o mesmo código. Diferenças entre os apps são apenas de apresentação.

## Bibliotecas

| Finalidade | Biblioteca | Por quê |
|---|---|---|
| Código compartilhado | Kotlin Multiplatform 2.4 | Uma implementação de regras, dados e ViewModels para os dois apps |
| Rede | Ktor Client 3.6 (motor CIO no Android, Darwin no iOS) | Multiplataforma; `MockEngine` permite testar sem servidor |
| JSON | kotlinx.serialization | Padrão no KMP |
| Banco local | Room 2.8 (KMP) + SQLite embutido | Citado no case; expõe `Flow`; o mesmo esquema e o mesmo SQLite nas duas plataformas |
| Assíncrono | Coroutines / Flow | Exigido no case |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel` (KMP) | Mesmo ViewModel nos dois apps, com escopo de corrotinas |
| Interop Kotlin → Swift | SKIE | `Flow` vira `AsyncSequence`, `sealed` e `enum` viram enums Swift exaustivos |
| UI Android | Jetpack Compose (Material 3) + Navigation Compose | Exigido no case |
| UI iOS | SwiftUI hospedado em UIKit (`UINavigationController`) | SwiftUI exigido no case; UIKit dá aos coordinators o controle da navegação |
| Testes | kotlin.test, kotlinx-coroutines-test, Ktor MockEngine, Room em memória | Rodam no código compartilhado |
| CI | GitHub Actions | Lint, testes e build das duas plataformas |

## Como os favoritos sobrevivem à sincronização

Favorito é um dado **apenas local** — a API não tem esse campo. O banco tem duas tabelas:

```
devices                              favorites
───────────────────────              ──────────────────
id          (chave)                  device_id (chave)
type        ALARM | VIDEO
name
mac_address (alarme)
serial      (vídeo)
username    (vídeo)
password
```

- A tabela `devices` é o espelho do servidor e é reescrita a cada sincronização.
- A tabela `favorites` guarda só os ids marcados, e **a sincronização nunca escreve nela**.
- A leitura é um `LEFT JOIN` entre as duas, que devolve cada dispositivo com `isFavorite`.

Como não existe caminho de código em que a sincronização altere `favorites`, ela não tem como apagar um favorito por engano — a garantia é estrutural, não depende de cuidado em cada query. A única limpeza feita é remover favoritos órfãos, quando o dispositivo deixou de existir no servidor.

A sincronização acontece em uma transação por tipo:

1. Busca `/alarm-centrals` e `/video-devices` em paralelo.
2. Para cada tipo que respondeu com sucesso: grava o que veio, remove o que o servidor não tem mais e limpa favoritos órfãos.
3. Se um dos dois endpoints falhar, **só o outro tipo é atualizado**; o tipo que falhou mantém os últimos dados em vez de ser apagado.

Isso está coberto por testes que usam um banco Room real em memória (`DefaultDeviceRepositoryTest`).

## Decisões técnicas e trade-offs

**KMP com interface nativa, em vez de Compose Multiplatform.** O case pede Compose *ou* SwiftUI. Compartilhei tudo abaixo da tela e mantive a UI nativa, para atender ao enunciado nas duas plataformas. O custo é escrever cada tela duas vezes; o ganho é que nenhuma regra é duplicada.

**ViewModels compartilhados.** Dão uma única suíte de testes para o comportamento das telas. O custo fica no iOS: é preciso uma ponte por tela e cuidar do ciclo de vida manualmente (o `deinit` da ponte encerra o escopo do ViewModel). A alternativa seria compartilhar só até o repositório e escrever ViewModels em Swift — mais idiomático no iOS, porém com lógica de tela duplicada.

**Coordinator.** No iOS os coordinators controlam um `UINavigationController` e cada tela SwiftUI entra por um `UIHostingController`; por isso o app usa ciclo de vida UIKit (`AppDelegate`/`SceneDelegate`). No Android o coordinator encapsula o `NavController`, e as telas não o conhecem.

**Escrita "servidor primeiro".** Criar, editar e remover chamam a API e só gravam no banco depois da confirmação. É simples e nunca mostra um dado que o servidor recusou. O custo é que essas operações exigem conexão (ver pendências).

**Favoritos em tabela separada.** Explicado acima. A alternativa — uma coluna `is_favorite` em `devices` — funcionaria, mas dependeria de nenhuma escrita futura sobrescrever a coluna.

**`compileSdk` 36 e versões de bibliotecas.** O case fixa `compileSdk = 36`, mas as versões mais recentes de várias bibliotecas AndroidX já exigem 37. Por isso algumas estão em versões anteriores (core-ktx 1.18, activity 1.12, Compose BOM 2026.06, lifecycle 2.10, navigation 2.9). Pelo mesmo motivo o motor HTTP do Android é o **CIO** do Ktor e não o OkHttp, cuja versão atual também exige 37.

**API em HTTP sem TLS.** O endpoint do desafio é `http://`. Liberei tráfego sem criptografia **apenas para esse domínio**: `network_security_config.xml` no Android e exceção de ATS no iOS. Todo o resto continua bloqueado.

**Tratamento de erro em um único lugar.** Toda chamada passa por `apiCall`, que traduz falhas em `DeviceError` (sem conexão, tempo esgotado, token inválido, não encontrado, duplicado). A documentação da API lista 403 para token inválido, mas o servidor responde 401; os dois são tratados como token inválido. Um 409 ao salvar marca o campo MAC ou número de série como duplicado.

**Um canal único de avisos.** Mensagens de sucesso e erro passam pelo `MessageCenter` e são exibidas acima de todas as telas, para o aviso "Dispositivo adicionado." aparecer mesmo depois de o formulário fechar.

**Injeção de dependência manual.** O `AppContainer` monta o grafo de objetos. Para um app deste tamanho é suficiente e não esconde nada; com mais telas eu trocaria por Koin.

### Interface e protótipo

As telas seguem o protótipo e o guia de estilo do Figma nas duas plataformas: paleta, hierarquia de texto, card único com a lista de dispositivos, busca em formato de pílula, chips, botão "Adicionar" na base, bottom sheet de ações, campos com contorno e Snackbar escuro. As cores do guia estão centralizadas em `AppColors` (um arquivo por plataforma, com os mesmos nomes do guia).

O protótipo define um único tema claro, então o app não acompanha o modo escuro do sistema.

Diferenças em relação ao protótipo:

| No protótipo | No app | Motivo |
|---|---|---|
| Fonte Open Sans | Fonte do sistema (Roboto / SF) | Ainda não embuti os arquivos da fonte; pesos e tamanhos seguem o guia |
| Ícones próprios do guia | Material Icons no Android, SF Symbols no iOS | Ícones equivalentes da plataforma, sem exportar os SVGs |
| "Ativo" abaixo do nome, em Informações | Não exibido | A API não informa status do dispositivo; mostrar um valor fixo seria inventar dado |
| Lista sem indicação de favorito | Igual ao protótipo | O favorito aparece pelo chip "Favoritos" e pelo texto da ação no bottom sheet |

### Adaptações de iOS

| No protótipo / Android | No iOS | Motivo |
|---|---|---|
| Snackbar | Banner próprio, com o mesmo visual | O iOS não tem Snackbar nativo |
| Bottom Sheet | `.sheet` com `presentationDetents` | Componente nativo equivalente |
| Modal "Remover dispositivo?" | `confirmationDialog` com os mesmos textos | Padrão da plataforma para ações destrutivas |
| Barra superior do formulário e de Informações | Barra de navegação nativa (fechar, título e "Salvar") | Mantém gestos e comportamento esperados no iOS |
| Aviso "Copiado" | Exibido no iOS e no Android até a versão 12 | A partir do Android 13 o sistema já mostra a própria confirmação de cópia |
| Estado observável | `ObservableObject` | A macro `@Observable` exige iOS 17; o alvo é iOS 16 |

## Testes e CI

Os testes ficam no módulo `shared` e cobrem as regras de negócio:

| Arquivo | O que verifica |
|---|---|
| `DeviceValidatorTest` | Campos obrigatórios, formato do MAC, senha do alarme com 4 ou 6 dígitos |
| `DeviceFilterTest` | Chips e busca por nome, ignorando maiúsculas e acentos |
| `DeviceApiTest` | Leitura da resposta, token no cabeçalho, tradução de cada tipo de erro |
| `DefaultDeviceRepositoryTest` | Sincronização com banco real: favoritos preservados, remoção no servidor, falha parcial, criar e remover |
| `DashboardViewModelTest` | Estados de carregamento, erro com dados em cache, filtros, favoritar, remover |
| `DeviceFormViewModelTest` | Validação, criação, edição pré-preenchida, conflito 409 |

Hoje são 44 testes no alvo iOS e 37 na JVM. A diferença são os testes do repositório, que usam o Room em memória e só rodam no simulador iOS.

O [workflow de CI](.github/workflows/ci.yml) roda a cada push e pull request:

- **Android (Ubuntu):** lint, testes do `shared` na JVM e build do APK de debug.
- **iOS (macOS):** testes do `shared` no simulador e build do app.

## O que ficou pendente e o que eu melhoraria

**Pendente em relação ao case**

- **Fonte Open Sans e ícones do guia de estilo.** O layout e as cores seguem o protótipo, mas a fonte é a do sistema e os ícones são os equivalentes de cada plataforma.
- **Validação em iOS 16.** O deployment target é 16.0, mas só testei em simuladores mais novos; não tinha o runtime 16 instalado.
- **Offline no iOS não foi testado manualmente**, só no Android (o simulador usa a rede do Mac). A regra é a mesma e está coberta por testes.

**O que eu faria com mais tempo**

- **Fila de operações pendentes.** Permitir criar, editar e remover sem conexão: a escrita passaria a ser local primeiro, com um estado de sincronização por dispositivo, e uma fila enviaria as operações quando a rede voltasse. Criações offline usariam um id temporário, trocado pelo id do servidor na mesma transação que migra o favorito.
- **Proteger as senhas dos dispositivos.** Hoje ficam em texto puro no banco local. Usaria Keystore/Keychain ou um banco criptografado.
- **Testes de UI** (Compose UI Test e XCUITest) para os fluxos principais, e teste de integração do banco também no Android.
- **Ferramentas de qualidade**: ktlint e detekt no Kotlin, SwiftLint no Swift, rodando no CI.
- **Koin** no lugar da composição manual.
- **Textos em arquivos de recursos** (`strings.xml` / `Localizable.strings`); hoje estão no código de cada app.
- **Acessibilidade**: revisar os fluxos com TalkBack e VoiceOver e com fontes ampliadas.
- **Modularização do `shared`** em módulos menores (rede, banco, domínio) se o app crescer.
