# Estratégia de desempenho do iOS

Data: 13/11/2026. Parte da [análise de desempenho](analise-de-desempenho.md) e de medições novas,
feitas no simulador iPhone 18 Pro (iOS 27.2) com um teste temporário que chama os Observables como a
UI chama.

## Resumo

O app iOS **já atualiza a interface só no main thread**: os Observables são `@MainActor`, as tarefas que
observam os ViewModels herdam esse isolamento, e a medição confirmou que 100% das publicações de estado
acontecem no main thread. O problema é o contrário: **trabalho pesado também roda no main thread, dentro
da própria tecla digitada**.

A estratégia segue uma regra só:

> **O main thread só faz o que toca a interface.** Texto do TextField, estado publicado e navegação ficam
> no `@MainActor`. Filtrar, normalizar texto e montar listas rodam fora dele, e só o resultado volta para
> o main thread.

Além disso, essa regra deixa de depender de cuidado manual e passa a ser **verificada pelo compilador**.

## Situação

| Fase | Estado |
|---|---|
| 0. Medições na suíte | **Aplicada**: `iosAppTests/PerformanceTests.swift` e signposts em `ViewModel/StatePublishing.swift` |
| 1. Contrato de threads verificado | **Aplicada**: Swift 6 com concorrência completa, zero avisos |
| 2. Trabalho pesado fora do main thread | **Aplicada** no `shared` (vale para Android e iOS) |
| 3. TextFields sem render extra | **Aplicada** nos três Observables |
| 4. Lista e fontes | Pendente |
| 5. Sincronização | Pendente |

### Resultado das fases 0, 2 e 3

Mesma medição do diagnóstico, no mesmo simulador, depois das mudanças:

| Métrica | Antes | Depois |
|---|---:|---:|
| Main thread por tecla na busca, 100 dispositivos | 2,5 ms | 0,03 ms |
| Main thread por tecla na busca, 1.000 dispositivos | 23,9 ms | **0,03 ms** |
| Main thread por tecla na busca, 5.000 dispositivos | 145,8 ms | **0,03 ms** |
| Tempo até a lista filtrada aparecer (1.000 dispositivos) | 35 ms | ~133 ms (120 ms são o `debounce`, de propósito) |
| Publicações por tecla no formulário | 2,0 | **1,0** |
| Publicações ao reenviar o mesmo texto | 1 | **0** |

O custo da tecla no main thread agora é constante: não depende mais da quantidade de dispositivos. O
tempo até a lista aparecer também ficou constante, e quase todo ele é a espera do `debounce` pela pausa na
digitação. O texto digitado aparece na hora, porque é estado local do campo.

O que mudou:

- **`shared`**: o nome normalizado e o `identifier` do alarme são calculados uma vez, na criação do
  `Device`. A normalização usa `StringBuilder` e `when`, sem boxing. O `DashboardViewModel` filtra em
  `Dispatchers.Default` (`flowOn`, despachante injetável nos testes) e aplica `debounce` de 120 ms só
  à busca. Limpar a busca e trocar de chip continuam instantâneos.
- **iOS**: os Observables só atribuem o `@Published` quando o estado mudou de fato (`equals` do Kotlin)
  e emitem um signpost em cada publicação, visível em *Points of Interest* no Instruments.
- **Testes novos**: `PerformanceTests` (tecla abaixo de 2 ms no main thread com 1.000 dispositivos, resultado
  publicado no main thread, um render por tecla, nenhum render ao reenviar o mesmo texto) e, no
  `shared`, testes de `debounce`, de normalização e de igualdade dos dispositivos.

Ainda falta medir em aparelho real e build Release, e passar a busca pelo Instruments.

### Resultado da fase 1

O contrato "TextField e atualização de view só no main thread" agora é verificado pelo compilador:

- **Swift 6** (`SWIFT_VERSION: "6.0"`, `SWIFT_STRICT_CONCURRENCY: complete`) nos dois targets. O app e os
  testes compilam sem nenhum aviso de concorrência; qualquer código que atualize a interface fora do
  main actor passa a ser erro de compilação.
- **Callbacks `@MainActor`** em todas as views e view controllers: `onChange` dos campos, busca, chips,
  ações da lista e do bottom sheet, `onRetry`, `onClose` e `onSaved`. O `onChange` dos campos também é
  `@Sendable`, porque o setter do `Binding` do SwiftUI exige.
- **Tarefas explícitas**: toda observação de flow é `Task { @MainActor ... }`, assim como o laço de
  avisos do `AppCoordinator` e o temporizador do `ToastPresenter`.
- **Ciclo de vida fora do `deinit`**: cada Observable guarda suas tarefas e o `clear()` do ViewModel em
  um `ObservationBag` (`nonisolated let`). O `DevicesCoordinator` é o delegate do
  `UINavigationController` e chama `stopObserving()` assim que uma tela sai da pilha (voltar, arrastar,
  fechar, salvar). O `deinit` só repete o `stop()`, que é idempotente, e não toca em estado isolado.
- **Verificação em tempo de execução** (debug): toda publicação de estado passa por
  `dispatchPrecondition(condition: .onQueue(.main))`, cobrindo o que o compilador não vê, como um
  callback do Kotlin.
- **`@preconcurrency import Shared`**: os tipos gerados do Kotlin não declaram `Sendable`. Eles só são
  usados no main actor, e o import evita avisos que não indicam problema real.
- **Testes novos**: `LifecycleTests` (sair da tela encerra a observação, inclusive pelo delegate da
  navegação; telas ainda na pilha continuam observando; `stop()` idempotente).

Não foi usado `SWIFT_DEFAULT_ACTOR_ISOLATION: MainActor`: as anotações explícitas já cobrem o app, e a
opção exige Xcode 26 ou mais novo também no CI.

## Diagnóstico

Medições feitas **antes** das mudanças.

### Onde cada coisa roda hoje

| Peça | Isolamento hoje | Situação |
|---|---|---|
| `DashboardObservable`, `DeviceFormObservable`, `DeviceDetailsObservable` | `@MainActor` | Correto |
| Laços `for await` que recebem o estado do Kotlin | Herdam o `@MainActor` do `init` | Correto, mas implícito |
| Coordinators, `ToastPresenter` | `@MainActor` | Correto |
| Views SwiftUI e `UIHostingController` | `@MainActor` pelo SDK | Correto |
| Closures de callback (`onChange`, `onQueryChange`, `onSaved`, `onOpenDevice`…) | Sem anotação | Funciona por acaso: ninguém chama de outra thread hoje, mas nada impede |
| `deinit` dos Observables | Não isolado; acessa `observation`/`tasks` | Aceito no Swift 5, vira aviso com concorrência estrita |
| Filtro da busca (`DashboardViewModel`, Kotlin) | `Dispatchers.Main.immediate` | **Problema**: roda dentro da chamada da tecla |
| Verificação pelo compilador | `SWIFT_VERSION 5.0`, concorrência estrita desligada | Nada garante as regras acima |

### O que foi medido

**Busca: custo de uma tecla no main thread** (`DashboardObservable.onQueryChange`, digitando "camera"):

| Dispositivos | Main thread bloqueado na tecla | Até a lista nova ser publicada |
|---:|---:|---:|
| 100 | 2,5 ms | 13,5 ms |
| 1.000 | **23,9 ms** | 35,3 ms |
| 5.000 | **145,8 ms** | 156,7 ms |

Como o ViewModel usa `Dispatchers.Main.immediate` e a chamada já chega pelo main thread, o `combine`
roda o filtro **dentro** de `onQueryChange`, antes de a tecla terminar de ser tratada. Com 1.000
dispositivos, cada letra trava a tela por mais de um frame (16,6 ms). Com 5.000, o atraso fica visível ao
digitar.

**Formulário: custo de uma tecla** (`DeviceFormObservable.onNameChange`, digitando como uma pessoa, a ~16
teclas/s):

| Métrica | Valor |
|---|---:|
| Main thread por tecla | 0,1 ms |
| Publicações de estado por tecla | **2,0** |

O formulário é barato em CPU, mas cada tecla gera **dois renders** do SwiftUI: um da publicação imediata
de `apply` (que existe de propósito, para o cursor não pular) e outro, com o mesmo estado, que chega pelo
`StateFlow` logo depois.

**Publicação fora do main thread: nenhuma.** Todas as publicações observadas aconteceram no main thread.

## Estratégia

São cinco fases, em ordem de risco crescente. Cada fase tem um critério para dizer se funcionou.

### Fase 0: transformar as medições em teste

Antes de mudar código, a medição precisa virar parte da suíte, para provar o ganho e evitar regressões.

- Criar `PerformanceTests` no `iosAppTests` com `measure(metrics: [XCTClockMetric()])`, usando 1.000
  dispositivos, para:
  - o tempo de `onQueryChange` no main thread;
  - as publicações por tecla no formulário (contando `objectWillChange`).
- Marcar com `os_signpost` (`OSSignposter`, iOS 15+) a publicação de estado em cada Observable, para ver
  no Instruments quanto tempo passa entre a tecla e o render.
- Medir em **aparelho real e build Release** com o Instruments (templates *Time Profiler*, *Hangs* e
  *SwiftUI*). O simulador serve para comparar, não para dar o número final.

**Metas** (com 1.000 dispositivos):

| Métrica | Hoje | Meta |
|---|---:|---:|
| Main thread por tecla na busca | 23,9 ms | < 1 ms |
| Publicações por tecla no formulário | 2,0 | 1,0 |
| Rolagem do dashboard | Não medida | Sem *hitches* no Instruments |

### Fase 1: deixar o contrato de threads explícito e verificado

Objetivo: tudo que atualiza view ou trata TextField é `@MainActor` **e o compilador garante isso**.

1. **Ligar a verificação de concorrência** no `project.yml`, nos dois targets:
   - primeiro `SWIFT_STRICT_CONCURRENCY: complete` ainda no Swift 5, para os problemas aparecerem
     como avisos sem quebrar o build;
   - depois `SWIFT_VERSION: "6.0"`, quando os avisos zerarem.
   Com Xcode 26 ou mais novo também dá para usar `SWIFT_DEFAULT_ACTOR_ISOLATION: MainActor`: todo tipo
   do app passa a ser `@MainActor` por padrão, e só o que precisar sair do main thread é marcado
   `nonisolated`. É exatamente o modelo desta estratégia. A opção é de compilação, então não depende do
   deployment target (iOS 16).
2. **Anotar os callbacks que vêm da UI** como `@MainActor`:
   - `OutlinedField.onChange: @MainActor (String) -> Void`
   - `SearchField.onQueryChange`, `FilterChipsRow.onSelected`
   - closures de `DashboardView`, `DeviceListCard`, `DeviceActionsSheet`
   - `onSaved` e `onClose` do formulário, e as closures criadas nos coordinators
3. **Deixar explícito o isolamento das tarefas de observação**:
   `Task { @MainActor [weak self] in for await state in viewModel.uiState { self?.publish(state) } }`.
4. **Tirar o ciclo de vida do `deinit`**: dar a cada Observable um `stop()` `@MainActor` que cancela as
   tarefas e chama `viewModel.clear()`, e chamá-lo pelo coordinator quando a tela sai da pilha
   (`UINavigationControllerDelegate`). O `deinit` só cancela o que sobrar, sem tocar em estado isolado.
5. **Afirmar o main thread em debug** nos pontos de publicação, com
   `dispatchPrecondition(condition: .onQueue(.main))` (disponível no iOS 16). Uma regressão vira crash
   no desenvolvimento, não *glitch* em produção.

**Critério:** build sem avisos de concorrência e todos os testes passando.

### Fase 2: tirar o trabalho pesado do main thread

É a fase com maior ganho. Como a lógica está no `shared`, a mudança é feita uma vez e vale também para o
Android, que precisa ser verificado junto.

1. **Normalizar o nome uma vez só**: guardar o nome sem acentos e em minúsculas no mapeamento do banco
   para o domínio, em vez de recalcular a cada busca. Medido: 21 ms → 0,25 ms com 1.000 dispositivos.
2. **Filtrar fora do main thread**: no `DashboardViewModel`, aplicar `flowOn(Dispatchers.Default)` ao
   `combine` antes do `stateIn`. `onQueryChange` passa a só gravar um `MutableStateFlow` (custo
   constante), e o resultado volta pelo `StateFlow` para o Observable, que publica no main thread como já
   faz hoje.
3. **`debounce` curto (~120 ms) só no fluxo da busca.** O texto do TextField continua sendo atualizado na
   hora, porque é estado local da view (`@State` no `SearchField`). Só o filtro espera a pessoa parar de
   digitar.
4. **Calcular o `identifier` do alarme uma vez** (`val` no construtor), em vez de rodar a regex a cada
   linha desenhada.

**Critério:** main thread por tecla abaixo de 1 ms com 1.000 dispositivos (Fase 0), e os testes do
`shared` passando na JVM e no simulador.

### Fase 3: TextFields no main thread, sem render extra

Os TextFields já são tratados no main thread. O que falta é tirar o trabalho repetido, sem perder o
comportamento que hoje evita o cursor pular.

- **Manter a atualização síncrona** de `DeviceFormObservable.apply`. Ela é o motivo de o texto não
  "voltar" enquanto se digita. Nenhum texto de campo deve ser publicado de forma assíncrona (por
  exemplo com `DispatchQueue.main.async`), porque isso reintroduz o cursor pulando.
- **Ignorar o estado repetido** no laço de observação: `if state != self.state { self.state = state }`.
  Os tipos vindos do Kotlin comparam com o `equals` da data class. Resultado esperado: 2 → 1 publicação
  por tecla.
- **`SearchField`**: já mantém o texto em `@State` local e só repassa a busca. Com a Fase 2 o repasse
  fica barato. Não precisa de mudança além da anotação `@MainActor`.
- **Validação continua no submit**, como hoje, e não a cada tecla. Isso mantém o trabalho por tecla mínimo.

**Critério:** 1,0 publicação por tecla no teste da Fase 0, e os testes de formulário passando.

### Fase 4: render da lista e das fontes

Trabalho de UI que **precisa** ficar no main thread, então a solução é fazer menos dele.

1. **Lista preguiçosa**: trocar `ScrollView` + `VStack` por `LazyVStack`, e `ForEach(Array(devices.enumerated()))`
   por `ForEach(devices, id: \.id)`, com o divisor dentro da linha. Só as linhas visíveis passam a ser
   montadas.
2. **Linha como struct `Equatable`** (`DeviceRow`, comparando `id`, `name`, `identifier` e
   `isFavorite`) com `.equatable()`. Quando a lista muda, só as linhas alteradas são redesenhadas.
3. **Cache de fontes** no `@MainActor`: um dicionário por (tamanho, peso) para `UIFont.app` e
   `Font.app`, limpo em `UIContentSizeCategory.didChangeNotification`. Como o cache vive no main actor,
   não precisa de lock.
4. **Opcional**: separar o `@Published` do dashboard em lista e status. Hoje, mudar só `isRefreshing`
   redesenha o cabeçalho e a lista. Só vale se o Instruments mostrar custo depois dos itens 1 e 2.

**Critério:** rolagem sem *hitches* no Instruments com 1.000 dispositivos, em aparelho.

### Fase 5: menos emissões vindas da sincronização (`shared`)

Cada refresh hoje emite a lista inteira duas vezes, mesmo sem mudanças, e cada emissão chega ao main
thread e provoca um render.

- Gravar alarmes e vídeos em **uma transação só**.
- `distinctUntilChanged()` em `observeDevices()`.

**Critério:** um refresh sem mudanças no servidor não gera nenhuma publicação no dashboard.

## Ordem e esforço

| Fase | Onde | Esforço | Ganho esperado |
|---|---|---|---|
| 0. Medições na suíte | iOS | Pequeno | Base para tudo |
| 1. Contrato de threads | iOS | Médio (muitos arquivos, mudanças simples) | Segurança: o compilador garante o main thread |
| 2. Trabalho pesado fora do main | `shared` | Pequeno | **Maior ganho**: 24 ms → < 1 ms por tecla |
| 3. TextFields sem render extra | iOS | Pequeno | Metade dos renders do formulário |
| 4. Lista e fontes | iOS | Médio | Rolagem e primeiro render com listas grandes |
| 5. Sincronização | `shared` | Pequeno | Sem renders inúteis a cada refresh |

Sugestão: **0 → 2 → 3** primeiro (pouco código, ganho medido), depois **1** (pode ser feita aos poucos,
arquivo por arquivo, com os avisos ligados), e **4 → 5** por último.

## Riscos

- **`flowOn` e `debounce` mudam o tempo** em que o estado chega. Os testes do `shared` já esperam com
  `first { … }` e os do iOS com `waitUntil`, então devem continuar válidos, mas é preciso rodar as
  três suítes.
- **Swift 6** pode exigir ajustes na interop com o framework Kotlin (tipos do SKIE não marcados como
  `Sendable`). Por isso a Fase 1 começa no Swift 5 com avisos.
- **Mudanças no `shared` afetam o Android.** Mesma regra nas duas plataformas: validar o Android junto
  (testes e uma rolagem com 1.000 dispositivos).
- **`debounce` muda a sensação da busca.** 120 ms costuma passar despercebido; ajustar depois de usar
  em aparelho.
