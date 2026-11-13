# Análise de desempenho

Data: 13/11/2026. Escopo: módulo `shared` (KMP), app Android (Compose) e app iOS (SwiftUI/UIKit).

## Como foi feita

- Leitura de todo o código procurando trabalho repetido no thread principal, alocações por
  frame, listas não virtualizadas e escritas desnecessárias no banco.
- Microbenchmark do caminho mais quente (busca e filtro do dashboard), rodado na JVM
  (mesmo runtime do Android) e no Kotlin/Native do simulador iOS (iPhone, Apple Silicon).
  Os números são por emissão de estado, média de 20 a 50 execuções após aquecimento.

Não foi feito profiling em aparelho real (Android Macrobenchmark / Instruments). Os números
abaixo servem para comparar e priorizar, não como tempo absoluto em produção. Em celulares
Android intermediários, espere tempos de 3 a 10 vezes maiores que os da JVM do Mac.

## Resultado do benchmark: busca no dashboard

`List<Device>.filteredBy(query, filter)`, buscando "camera portao":

| Dispositivos | JVM (Android) | Kotlin/Native (iOS) |
|---:|---:|---:|
| 100 | 0,18 ms | 2,0 ms |
| 1.000 | 0,42 ms | **21 ms** |
| 10.000 | 3,5 ms | **217 ms** |

No iOS, com 1.000 dispositivos, cada tecla digitada na busca já custa mais que um frame
(16,6 ms a 60 Hz). Esse trabalho roda no `viewModelScope`, ou seja, no **thread principal**.

Medido depois pelo Observable, do jeito que a UI chama: com `Dispatchers.Main.immediate`, o filtro roda
**dentro** da chamada `onQueryChange`, antes de a tecla terminar de ser tratada. São 23,9 ms com 1.000
dispositivos e 145,8 ms com 5.000. O plano para o iOS está em
[estrategia-desempenho-ios.md](estrategia-desempenho-ios.md).

Alternativas medidas com os mesmos dados:

| 1.000 dispositivos | JVM | iOS |
|---|---:|---:|
| Atual (`map { accents[it] }.joinToString`) | 0,50 ms | 20,9 ms |
| `StringBuilder` + `when` (sem boxing) | 0,38 ms | 5,6 ms |
| Nome normalizado calculado uma vez | 0,09 ms | **0,25 ms** (~85x mais rápido) |

## Achados, por prioridade

> **Atualização:** os itens 1, 4 e 6 já foram aplicados (fases 0, 2 e 3 da
> [estratégia do iOS](estrategia-desempenho-ios.md#situação)). A tecla na busca passou de 23,9 ms
> para 0,03 ms no main thread do iOS com 1.000 dispositivos, e o formulário renderiza uma vez por tecla.

### Alta

**1. Busca e filtro no thread principal, com alocação por caractere** — *aplicado* (`shared/.../models/DeviceFilter.kt`)
`searchable()` converte cada nome em uma `List<Char>` com caracteres boxed, consulta um
`Map<Char, Char>` e junta tudo de novo, para todos os dispositivos, a cada emissão. O
`combine` do `DashboardViewModel` refaz isso sempre que qualquer fonte muda: cada tecla e
também cada mudança do estado de sync.
Correção sugerida, em ordem de ganho:
- normalizar o nome uma única vez, no mapeamento do banco para o domínio (ou em um
  `val` calculado no `Device`), em vez de a cada busca;
- trocar o `Map` + `joinToString` por `StringBuilder` + `when`;
- `flowOn(Dispatchers.Default)` no `combine` e `debounce(150)` só no fluxo da busca.

**2. Listas sem virtualização nas duas plataformas**
- Android (`DashboardScreen.kt`): a `LazyColumn` tem **um único item** (`item(key = "devices")`)
  contendo o `DeviceListCard`, que monta todas as linhas com `Column` + `forEach`. Na prática a
  lista é preguiçosa só no nome: todas as linhas são compostas, medidas e mantidas em memória.
- iOS (`DashboardView.swift` / `DeviceListCard.swift`): `ScrollView` + `VStack` (não
  `LazyVStack`), e `Array(devices.enumerated())` copia a lista a cada `body`.
Com poucas dezenas de dispositivos não se nota; com centenas, o primeiro render, a rolagem e
o uso de memória crescem de forma linear. Correção: no Android, `items(devices, key = { it.id })`
desenhando o fundo do card por item (cantos arredondados só no primeiro e no último); no iOS,
`LazyVStack` com `ForEach(devices, id: \.id)` e o divisor dentro da linha.

### Média

**3. Fonte recriada em todo `body` no iOS** (`Theme/AppTheme.swift`)
`Font.app` / `UIFont.app` cria um `UIFontDescriptor` com eixo de variação e um `scaledFont`
a cada chamada, e isso acontece em todos os `Text` sempre que um `body` é reavaliado. Correção:
cache por (tamanho, peso), limpo quando chegar `UIContentSizeCategory.didChangeNotification`.
(Não medido; é o padrão típico de custo escondido em SwiftUI.)

**4. `identifier` do alarme roda uma regex a cada acesso** — *aplicado* (`models/Device.kt`)
`Device.Alarm.identifier` chama `formatMac` (regex + `chunked` + `joinToString`) toda vez que
uma linha é desenhada. Medido: 3,1 ms para 1.000 alarmes no iOS, 0,3 ms na JVM. Correção:
calcular uma vez (`val` inicializado no construtor, ou `by lazy`).

**5. Cada sync reescreve e re-emite a lista inteira** (`DefaultDeviceRepository.refresh`, `DeviceDao`)
`replaceAllOfType` faz upsert de todos os registros e roda em **duas transações** (uma para
alarmes, outra para vídeos). O Room invalida a tabela em cada uma, então `observeAll()` emite a
lista completa duas vezes por refresh, mesmo quando nada mudou. Cada emissão refaz o filtro (item 1)
e redesenha a lista (item 2). Correção: gravar os dois tipos em uma única transação e aplicar
`distinctUntilChanged()` em `observeDevices()`.
Observação de escala: `DELETE ... WHERE id NOT IN (:ids)` passa um parâmetro por id; o SQLite
empacotado aceita até 32.766, folga grande para o caso de uso.

**6. Formulário iOS publica o estado duas vezes por tecla** — *aplicado* (`DeviceFormObservable.swift`)
`apply` publica o estado na hora (de propósito, para o `TextField` não brigar com a digitação) e a
observação do `StateFlow` publica o mesmo estado logo depois, o que gera dois renders do SwiftUI por
tecla. Medido digitando a ~16 teclas/s: **2,0 publicações por tecla**, com só 0,1 ms de CPU cada, ou seja,
o custo está no render extra e não no ViewModel. Correção: na observação, ignorar o estado se for igual
ao atual (`if state != self.state`, que usa o `equals` do Kotlin).

### Baixa / verificar antes de mexer

**7. Inicialização no thread principal.** `initializeApp` cria o `HttpClient` do Ktor e o builder
do Room em `Application.onCreate` / `didFinishLaunching`. O banco só abre na primeira query, então
o custo deve ser pequeno. Medir o cold start (Macrobenchmark `StartupTimingMetric` / Instruments
"App Launch") antes de mudar.

**8. Log HTTP completo em debug.** `LogLevel.ALL` serializa os corpos para o console; só vale para
builds de debug, mas qualquer medição de desempenho precisa ser feita em build de release.

## O que já está bom

- O refresh busca alarmes e vídeos **em paralelo** (`async`), e uma falha não apaga o outro tipo.
- O banco local é a fonte da verdade: a lista aparece na hora, inclusive offline.
- `SharingStarted.WhileSubscribed(5_000)` evita trabalho quando nenhuma tela observa.
- A busca guarda o texto localmente (Android e iOS), então a digitação não espera o ciclo do ViewModel.
- Chaves estáveis por `id` nas linhas, o que preserva o estado ao reordenar.

## Isolamento de threads no iOS

Toda atualização de interface já acontece no main thread: os Observables, coordinators e o
`ToastPresenter` são `@MainActor`, e a medição não encontrou nenhuma publicação de estado fora do main
thread. O que falta é o compilador garantir isso: o projeto está em Swift 5 com a verificação de
concorrência desligada, e os callbacks das views não são anotados com `@MainActor`. A
[estratégia do iOS](estrategia-desempenho-ios.md) cobre os dois pontos.

## Ordem sugerida

1. Itens 1 e 4 (shared, pequenos, com o maior ganho medido e beneficiando as duas plataformas).
2. Item 2 (virtualização), validado com uma lista de 1.000 dispositivos nas duas plataformas.
3. Itens 5 e 6.
4. Instrumentar o startup (item 7) e só então decidir se vale mexer.
