# LetsSign — Challenge 2TDS (Sprint 1)

Aplicativo Android nativo para preparar, enviar, acompanhar e assinar documentos PDF, desenvolvido como Sprint 1 do Challenge LetsSign (FIAP, 1º semestre de 2026, turma 2TDS).

Este README foi gerado a partir do código local (branch `feature/bfcs-dados`) e do enunciado em `2TDS_RS_Challenge LetsSign_2027.pptx_RevFinal.pdf`. Ele descreve o que é exigido, o que já existe no código e o que falta implementar.

> **Prazo da entrega:** 05/11 às 23h50 (conforme slide da agenda), valendo 10 pontos.
> **Integrantes e RM:** [preencher]
> **Nome do projeto:** [preencher]
> **Link do vídeo demonstrativo (até 5 min):** [preencher]

---

## 1. Resumo da situação

| Área | Situação |
| --- | --- |
| Build (Gradle 9.6, AGP 9.5, Kotlin 2.2.10, KSP, Room 2.8.5) | Compila e gera APK de debug |
| Persistência local (Room v2, entidades, DAOs, transações) | Pronta; bugs do envio corrigidos |
| Autenticação e perfis (Admin / Signatário) | Pronta, com telas |
| Cadastro e edição de signatários | Pronto, com telas |
| Novo documento (PDF, pessoas, campos, revisão, envio) | Pronto, com telas (ver seção 11 sobre limites) |
| Assinatura pelo signatário e PDF final | Pronta, com telas |
| Acompanhamento, histórico e nova versão | Prontos, com telas |
| Controle de acesso | Aplicado antes de abrir documento ou PDF |
| Testes | 10 unitários passam; 5 instrumentados compilam, mas **não foram executados** (sem dispositivo) |

Não foi feito teste manual de ponta a ponta em aparelho. Veja a seção 11.

## 2. O que a atividade exige (Sprint 1)

Resumo dos requisitos obrigatórios do enunciado. Os números entre parênteses são as páginas do PDF.

### Plataforma
- Aplicativo **nativo** para Android ou iOS (p. 15, 21). O grupo escolheu **Android**.
- Entregáveis: código-fonte completo em `.zip`, **APK**, apresentação (PPT ou PDF) e vídeo demonstrativo de até 5 minutos (p. 23).

### Acesso e perfis
- Login para dois perfis: **Admin** e **Signatário** (p. 16).
- Admin: iniciar documento por upload de PDF, visualizar documentos (enviados, parcialmente assinados, concluídos) e cadastrar signatários.
- Signatário: visualizar documentos pendentes e assinados, e efetuar a assinatura.

### Signatários
- Admin cadastra e edita signatários com **nome completo, e-mail, telefone e senha**. E-mail e senha são as credenciais de login (p. 16).

### Fluxo de envio de documento
1. Cadastrar um novo fluxo de documento para assinatura.
2. Informar o título.
3. Selecionar um arquivo PDF no celular.
4. Selecionar e classificar os signatários (Contratante, Contratado ou Testemunha).
5. Visualizar as páginas do PDF.
6. Posicionar os campos de assinatura nas páginas.
7. Revisar e confirmar o envio (p. 17–19).

Regras associadas (p. 17, 22):
- O app deve informar quando o arquivo for inválido, estiver corrompido ou não puder ser processado.
- Um documento precisa de **pelo menos um signatário**.
- Cada campo de assinatura deve estar associado a um participante.
- Campos devem permanecer **dentro dos limites da página**.
- Dados obrigatórios devem ser validados antes do envio.
- Documento enviado e suas configurações são preservados; alterações geram **nova versão e novo fluxo**.
- O documento só é concluído quando **todos os signatários obrigatórios** assinarem.
- Dados e documentos de um usuário não podem ser acessados por outro sem terem sido enviados a ele.

### Posicionamento dos campos (p. 18)
- Adicionar campo para o signatário, escolher a página e arrastar até a posição.
- Mover ou excluir campos antes do envio; mais de um campo por signatário.
- **Requisito técnico:** salvar página, posição e dimensões **em relação ao tamanho da página**, para que o posicionamento se preserve em outro dispositivo.

### Revisão, envio e acompanhamento (p. 19)
- Revisão mostra documento final, lista de signatários, campos/páginas e mensagem do convite.
- Em caso de falha no envio, orientar o usuário e permitir nova tentativa **sem duplicar solicitações**.
- Acompanhamento: situação geral, situação individual dos signatários, datas e horários, histórico de criação/envio/assinatura e documento final quando concluído.

### Assinatura (p. 20)
- Signatário abre o documento, assina e salva no app, atualizando o status do registro para concluído.

### Persistência (p. 21)
- Registros em banco local: **Room**, Core Data ou SQLite.
- Estruturar repositórios para permitir, no futuro, persistência por APIs REST.
- PDFs podem ser salvos de **uma de duas formas**: (a) arquivo no sistema de arquivos do aparelho com nome, caminho, data e status no banco; ou (b) conteúdo binário em campo BLOB no banco.

### Bibliotecas de PDF (p. 22)
- Exemplos citados: Android (Canvas + PDFBox-Android) ou iOS (PDFKit + PDFAnnotation). São exemplos, não exigências.

### Fora do escopo da Sprint 1
- Backend real (Java/Spring ou C#/.NET), banco NoSQL, APIs REST e envio de arquivos por endpoint são da **Sprint 2** (p. 29–30).
- Assinatura criptográfica com certificado, CPF, geolocalização e integração com SignPad não foram definidos como requisitos da Sprint 1 (ver `design/ESCOPO-SPRINT-1.md`).

### Critérios de avaliação (Sprint 1, p. 25)
| Critério | Peso |
| --- | --- |
| Adequação ao problema proposto | 20% |
| Implementação técnica funcional | 30% |
| Qualidade do código | 20% |
| Apresentação e documentação | 20% |
| Criatividade e inovação | 10% |

---

## 3. O que está implementado

Tudo abaixo foi implementado e compila (`./gradlew :app:assembleDebug`). Os testes unitários passam; o fluxo não foi testado manualmente em aparelho.

### Base do projeto
- Android com Jetpack Compose e Material 3, `minSdk 28`, `targetSdk 37`, Java/Kotlin 17.
- Room 2.8.5 com KSP 2.2.10-2.0.2 e Lifecycle ViewModel Compose.
- Injeção manual de dependências em `AppContainer.kt`, criada pelo `LetsSignApp`.
- `FileProvider` (`res/xml/file_paths.xml`) para abrir PDFs em outro aplicativo.

### Persistência (`data/local/`)
Banco `letssign_database`, versão 2:

| Entidade (tabela) | Função |
| --- | --- |
| `UserEntity` (`users`) | Nome, e-mail (único), telefone, hash da senha e papel (`ADMIN` / `SIGNATORY`) |
| `DocumentEntity` (`documents`) | Título, `filePath`, status, criador, mensagem do convite, `versionNumber`, `parentDocumentId` e `finalFilePath` |
| `DocumentSignatoryEntity` (`document_signatories`) | Participante do documento, função, `hasSigned`, `signedAt` e `signaturePath` (PNG da assinatura) |
| `SignatureFieldEntity` (`signature_fields`) | Campo: página e retângulo normalizado (0 a 1) |
| `DocumentHistoryEntity` (`document_history`) | Linha do tempo: criação, assinaturas, situação parcial e conclusão |

Projeções em `data/local/model/Projections.kt`: `DocumentSummary` (lista, com progresso) e `SignatoryDetail` (participantes).

- `UserDao`: primeiro Admin de forma transacional, CRUD, lista de signatários.
- `DocumentDao`: envio transacional (`insertFullDocumentFlow`), listas por situação e por participante, detalhe de participantes, acesso, histórico.

### Regras de negócio
- **Envio** (`RoomDocumentRepository.createDocumentFlow`, transacional): título obrigatório, PDF obrigatório, ao menos um signatário, sem signatário repetido, ao menos um campo por signatário, campos dentro da página.
- **Campos**: referenciam o participante pelo `userId`; o DAO troca pelo ID gerado na mesma transação.
- **Assinatura** (`signDocument`, transacional): bloqueia reassinatura; atualiza a participação, salva o caminho do PNG, registra o evento e recalcula a situação: Enviado → Parcialmente assinado → Concluído.
- **Acesso** (`canAccess`): criador ou participante. Checado antes de abrir o documento ou o PDF.
- **Usuários** (`UserRepository`): login PBKDF2, sessão observável (`StateFlow`), somente Admin cadastra ou edita signatários, e-mail duplicado recusado, senha em branco na edição mantém a atual.
- **Senhas** (`PasswordHasher`): PBKDF2-HMAC-SHA256, 210.000 iterações, salt de 16 bytes.

### PDF e arquivos (`data/file/`)
- `PdfPages`: valida o PDF (cabeçalho `%PDF-`, abertura, páginas, protegido) com mensagens para o usuário; renderiza páginas com `PdfRenderer`.
- `FileStorage`: copia o PDF para `filesDir/documents/`, salva a assinatura desenhada como PNG recortado em `filesDir/signatures/` e gera o PDF final em `filesDir/final/`, desenhando cada assinatura no campo com a proporção preservada.
- `DocumentService.ensureFinalPdf`: gera o PDF final quando o documento está concluído e o reaproveita depois.

### Telas (`ui/`)
- **Entrada** (`AppRoot.kt`): navegação por pilha de rotas. Sem Admin, abre a configuração inicial; sem sessão, abre o login.
- **Autenticação** (`auth/`): configuração do Admin e login com "E-mail ou senha incorretos.".
- **Admin** (`admin/`): abas Documentos, Signatários e Conta.
  - Documentos: filtros Todos, Enviados, Parciais e Concluídos, com progresso e estados vazios.
  - Detalhe: participantes, situação, histórico, abrir PDF original e final, criar nova versão.
  - Signatários: busca, cadastro e edição com validações.
- **Novo documento** (`newdoc/`): assistente em quatro passos, com a barra inferior oculta.
  - Documento: título e seleção de PDF, com validação antes de continuar.
  - Pessoas: seleção múltipla, função por pessoa, confirmação ao remover quem tem campos e **substituição de signatário** (troca a pessoa, mantém a função e reassocia os campos já posicionados).
  - Campos: página por página, com adicionar, arrastar, redimensionar e excluir. Detecta sobreposição.
  - Revisão: resumo, mensagem do convite e "Confirmar envio", com "Enviando…" para evitar toques repetidos.
  - Nova versão: pré-preenchida a partir do documento original, com novo arquivo.
- **Signatário** (`signatory/`): abas Pendentes e Assinados. A leitura mostra o PDF com os próprios campos em destaque. A tela de assinatura tem desenho com dedo, Limpar e Confirmar, e mostra a conclusão da participação. Se a assinatura já estiver registrada (outro acesso ou toque repetido), o app recarrega o registro e informa isso, sem assinar de novo.
- **Comuns** (`common/`): barra de topo, renderização de página com sobreposição (`PdfPageImage`), campos somente leitura (`ReadOnlyFields`), status em português e abertura de PDF.

### Geometria
- `FieldGeometry`: mover, redimensionar (tamanho mínimo e borda da página) e detectar sobreposição. Funções puras, cobertas por teste unitário.

### Testes
- `PasswordHasherTest` (3) e `FieldGeometryTest` (7): passam na JVM.
- `DocumentFlowInstrumentedTest` (5): envio com campos, rejeição sem campo, transições de situação, reassinatura bloqueada e acesso. Compila; **não foi executado**.

### Documentação
- `design/` (escopo da Sprint 1, guia de telas, pranchas) e `docs/LOGIN-LOCAL.md`.

---

## 4. O que ainda falta

Esta seção lista só o que não está pronto. Os limites de implementação também estão na seção 11.

### 4.1 Para entregar (P1)
- [ ] Validar o fluxo completo em aparelho ou emulador (roteiro na seção 11).
- [ ] Executar `./gradlew :app:connectedDebugAndroidTest` em um dispositivo.
- [ ] Apresentação (PPT ou PDF) com nome do projeto, integrantes e RMs, problemática, solução, tecnologias, fluxo, diferenciais e link do vídeo.
- [ ] Vídeo demonstrativo de até 5 minutos.
- [ ] Preencher nome do projeto, integrantes, RMs e link do vídeo no topo deste README.

### 4.2 Aderência ao guia de telas (P2)
- [ ] Cadastro de signatário dentro da etapa Pessoas (hoje é preciso cadastrar antes, na aba Signatários).
- [x] "Substituir" signatário: troca a pessoa, mantém a função e reassocia os campos já posicionados.
- [ ] Zoom no editor de campos e alternativa sem arrastar (mover para cima, baixo, esquerda e direita).
- [ ] Mensagens de erro junto ao campo, com o foco no primeiro erro (hoje aparecem em um texto no fim da tela).
- [ ] Estado "Envio não confirmado" com contagem de convites registrados e pendentes (hoje o envio é atômico, então não há resultado parcial).
- [x] Mensagem de "assinatura já registrada": recarrega o registro existente e não assina de novo.
- [ ] Ao trocar o PDF com campos, o aviso de descarte é mostrado depois da troca, não antes de confirmar.

### 4.3 Acessibilidade e acabamento (P3)
- [ ] Rótulos de conteúdo para leitores de tela nos campos e na área de assinatura.
- [ ] Verificação de contraste e de aumento de fonte.
- [ ] Miniaturas de páginas no passo Documento (hoje mostra só a primeira página em tamanho grande).

### 4.4 Dados e persistência (P3)
- [ ] Migrações do Room reais. Hoje a troca de versão recria o banco (`fallbackToDestructiveMigration`).
- [ ] Limpeza de arquivos órfãos em `filesDir` (PDFs de rascunhos descartados e PNGs de assinaturas de envios não concluídos).
- [ ] Camada de API REST, prevista para a Sprint 2 (o contrato `DocumentRepository` já existe).

### 4.5 Build de release (P3)
- [ ] Assinatura do APK de release (keystore). Só o APK de debug foi gerado.
- [ ] Revisar as regras do ProGuard (`app/proguard-rules.pro` está vazio, com comentário) antes de habilitar a minificação.

---

## 5. Problemas encontrados no código atual

> Os itens 1 a 6 foram corrigidos na execução do plano (seção 11). Mantidos aqui como registro.

1. **Envio com campos quebra por chave estrangeira (bug, P1).** `DocumentDao.insertFullDocumentFlow` insere os signatários com `insertSignatories`, que não retorna os IDs gerados, e depois grava os campos com o `signatoryId` que veio do chamador sem reatribuí-lo. Como o Room ativa chaves estrangeiras, a inserção de campos com `signatoryId` inexistente deve falhar. **Status: corrigido em `DocumentDao.insertFullDocumentFlow`.** `insertSignatories` agora retorna `List<Long>`; cada campo referencia o signatário pelo `userId` (único no documento, validado) e recebe o ID gerado. Campos de signatário fora do documento e signatários repetidos geram erro. Ainda falta um teste instrumentado que comprove a correção (ver F6).

2. **Duas interfaces `UserRepository` (P2).** Existe `domain/repository/UserRepository` (interface, com `authenticate(email, passwordHash)`) e `data/repository/UserRepository` (classe com login, cadastro e sessão). A implementação `RoomUserRepository` não é usada, e o `authenticate` compara o hash armazenado com um valor informado; como o PBKDF2 usa salt aleatório, essa comparação nunca daria certo. **Sugestão:** remover `RoomUserRepository` e a interface de domínio, ou fazer a classe `data` implementar a interface.

3. **Assinatura repetida não é bloqueada (P2).** `signDocument` não verifica se a participação já está assinada: uma segunda chamada regrava `signedAt`, cria outro evento `SIGNED` no histórico e não altera o status.

4. **Sem validação de envio (P1).** `createDocumentFlow` não exige título, signatário mínimo nem campos válidos; a validação prevista no enunciado ainda não existe.

5. **Sem verificação de acesso ao PDF (P2).** Consultas por signatário filtram por `userId`, mas `getDocumentById` e `getSignatoriesForDocument` não checam quem está logado.

6. **Build não sincroniza (P0).** Ao tentar compilar, encontrei, em ordem: (a) `build.gradle.kts` raiz referencia `libs.plugins.kotlin.android`, ausente do `libs.versions.toml`; (b) AGP 9.5 exige Gradle 9.6, e o wrapper está em 8.11.1; (c) AGP 9 não aceita a DSL `kotlinOptions` nem `kotlin.sourceSets` com Kotlin embutido; (d) KSP `2.0.21-1.0.28` não é compatível com Kotlin 2.2.10, e a tentativa com `2.2.10-2.0.2` ainda falhou com `unexpected jvm signature V` ao processar o Room. Essas mudanças foram testadas e **revertidas**, para não deixar o projeto em estado intermediário. Próximo passo: alinhar as versões do conjunto (AGP, Gradle, Kotlin, KSP e Room) em uma combinação oficialmente compatível, ou usar a mesma versão do Android Studio do grupo para descobrir a combinação que já compila.
7. **Build de release.** `proguard-rules.pro` referenciado e inexistente (ver 4.11).

---

## 6. Estrutura do projeto

```
challenge-letssign/
├── app/src/main/java/br/com/fiap/letssign/
│   ├── LetsSignApp.kt, AppContainer.kt      # Application e injeção manual de dependências
│   ├── MainActivity.kt                      # Entrada; chama AppRoot
│   ├── data/
│   │   ├── local/AppDatabase.kt             # Room v2
│   │   ├── local/entity/                    # Entidades (usuário, documento, participante, campo, histórico)
│   │   ├── local/dao/                       # UserDao, DocumentDao (transação de envio)
│   │   ├── local/model/Projections.kt       # Resumos para listas e detalhes
│   │   ├── file/PdfPages.kt                 # Validação e renderização de PDF (PdfRenderer)
│   │   ├── file/FileStorage.kt              # Importação, assinatura PNG, PDF final
│   │   └── repository/                      # UserRepository, RoomDocumentRepository, DocumentService
│   ├── domain/repository/DocumentRepository.kt  # Contrato usado pela futura API REST
│   └── ui/
│       ├── AppRoot.kt                       # Rotas e navegação por pilha
│       ├── auth/                            # Login e configuração inicial do Admin
│       ├── admin/                           # Documentos, detalhe, signatários, conta
│       ├── newdoc/                          # Assistente de envio e editor de campos
│       ├── signatory/                       # Pendentes/assinados, leitura, assinatura
│       └── common/                          # Componentes, overlay de campos, helpers
├── app/src/test/                            # Testes unitários (JVM)
├── app/src/androidTest/                     # Testes instrumentados (Room em memória)
├── design/                                  # Escopo, guia de telas e pranchas
└── docs/LOGIN-LOCAL.md                      # Login local
```

## 7. Como executar

Requisitos: Android Studio recente, JDK 17 e Android SDK com API 37 (o Gradle instala o que faltar).

```bash
./gradlew :app:assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # testes unitários (JVM, sem emulador)
./gradlew :app:connectedDebugAndroidTest   # testes instrumentados (requer dispositivo/emulador)
```

`local.properties` (caminho do SDK) é local e não deve ser versionado.

## 8. Fluxo local planejado para a demonstração

Conforme `design/ESCOPO-SPRINT-1.md`, sem backend nesta etapa:

1. Admin faz login e envia um PDF com título, signatários e campos.
2. O envio é gravado no banco local e fica disponível para os signatários **neste mesmo aparelho**.
3. Admin sai; um Signatário entra, vê o documento pendente, abre e assina.
4. O status é atualizado (Parcialmente assinado ou Concluído) e persistido.
5. Admin entra novamente e acompanha o documento com histórico.

Limitações assumidas: não há sincronização entre aparelhos e nenhum e-mail, SMS ou push é enviado.

---

## 9. Referências

- Enunciado: `2TDS_RS_Challenge LetsSign_2027.pptx_RevFinal.pdf` (fora do repositório).
- Escopo da Sprint 1: [`design/ESCOPO-SPRINT-1.md`](design/ESCOPO-SPRINT-1.md)
- Guia de telas: [`design/GUIA-DE-TELAS.md`](design/GUIA-DE-TELAS.md)
- Login local: [`docs/LOGIN-LOCAL.md`](docs/LOGIN-LOCAL.md)
- Referências citadas no enunciado: gov.br (assinatura eletrônica), blog da LetsSign sobre validade jurídica e Lei 14.620/23.

---

## 10. Plano de implementação

### 10.1 Premissas e decisões padrão

Prazo: **05/11 às 23h50**. Hoje é 04/11, então o plano precisa caber em um dia de trabalho. Por isso as fases estão em ordem de prioridade, com um corte definido no fim.

Decisões técnicas adotadas (podem ser trocadas, mas evitam dependências novas):

| Tema | Decisão | Motivo |
| --- | --- | --- |
| Armazenamento do PDF | Opção 1 do enunciado: arquivo copiado para `filesDir/documents/` + metadados no Room (`DocumentEntity.filePath`) | O campo já existe; evita BLOB com arquivos grandes |
| Renderização de páginas | `android.graphics.pdf.PdfRenderer` (API 21+) | Nativo; não exige PDFBox |
| Validação do PDF | Tentar abrir com `PdfRenderer`; exceção = inválido/corrompido/protegido | Atende a regra "extensão .pdf não basta" |
| Assinatura desenhada | Compose `Canvas` + `Path`, salvo como PNG em `filesDir/signatures/` | Nativo |
| Documento final | Gerar com `android.graphics.pdf.PdfDocument`, desenhando página renderizada + assinaturas nas coordenadas normalizadas | Nativo; sem nova dependência |
| Navegação | Estado simples com `sealed class Screen` no `MainActivity` | Evita adicionar `navigation-compose`; trocar se sobrar tempo |
| Mudanças no banco | Versão 1 → 2 com `fallbackToDestructiveMigration` (aceitável antes da entrega) | Simplifica; app ainda não tem dados reais |

### 10.2 Mudanças de schema (versão 2)

- `DocumentSignatoryEntity`: adicionar `signaturePath: String?`.
- `DocumentEntity`: adicionar `message: String?` (mensagem do convite), `versionNumber: Int = 1` e `parentDocumentId: Long?` (nova versão).
- Adicionar `DocumentDao.getAllSignatoriesWithUsers` (ou equivalente) para a tela de acompanhamento.

### 10.3 Fases e dependências

```
F0 Correções ─► F1 Sessão e telas base ─► F2 Novo documento ─► F3 Assinatura ─► F4 Acompanhamento e PDF final
                                              │                                       │
                                              └──────────► F5 Acesso e versões ◄──────┘
F6 Testes (contínuo, a partir de F2)          F7 Entregáveis (último)
```

### 10.4 Detalhamento

#### F0 — Correções da base (≈1 h)
- [ ] Corrigir `insertFullDocumentFlow`: usar IDs retornados ao inserir signatários e mapear os campos para eles (bug 5.1).
- [ ] Validar no próprio DAO: título, pelo menos um signatário, sem signatário repetido, campos apenas para signatários do documento.
- [ ] Bloquear `signDocument` quando a participação já estiver assinada (bug 5.3).
- [ ] Remover `RoomUserRepository` e a interface `domain/UserRepository`, ou unificar em uma só (bug 5.2).
- [ ] Adicionar `getAllSignatories()` ao `data.UserRepository`.
- [ ] Criar `app/proguard-rules.pro` (vazio, com comentário) ou remover a referência.
- [ ] Substituir os testes de template por testes reais (ver F6).

**Pronto quando:** `./gradlew assembleDebug` passa e o teste de envio com campos grava sem erro de FK.

#### F1 — Sessão, navegação e telas base (≈2 h)
- [ ] `ViewModel`s: `AuthViewModel` (login, logout, setup inicial), `SignatoriesViewModel`, `DocumentsViewModel`.
- [ ] Rotas: Login → (Admin: Documentos / Signatários / Conta) | (Signatário: Recebidos / Conta). Setup inicial se não houver Admin.
- [ ] Tela de login com mensagem "E-mail ou senha incorretos." mantendo o e-mail.
- [ ] Tela de setup do Admin.
- [ ] Admin: lista de signatários (busca, Cadastrar) e formulário de cadastro/edição com validações inline.
- [ ] Admin: lista de documentos com filtros Todos/Enviados/Parciais/Concluídos e estado vazio.
- [ ] Signatário: abas Pendentes e Assinados.
- [ ] Conta: nome, perfil e Sair.
- [ ] Banco de dados de teste opcional: criar Admin de demonstração pela tela de setup, sem senha no código.

**Pronto quando:** é possível criar o Admin, cadastrar um signatário, sair e entrar com ambos.

#### F2 — Novo documento: PDF, pessoas e campos (≈4 h)
- [ ] Seletor de arquivo com `ActivityResultContracts.OpenDocument` (`application/pdf`).
- [ ] Validação do PDF com `PdfRenderer`; mensagens de inválido, corrompido e protegido, sem apagar os dados já digitados.
- [ ] Copiar o PDF para `filesDir/documents/<uuid>.pdf` e gravar o caminho.
- [ ] Etapa **Documento**: título obrigatório, arquivo, total de páginas e miniatura.
- [ ] Etapa **Pessoas**: seleção múltipla de signatários, função por pessoa (Contratante/Contratado/Testemunha), remover, substituir, mínimo de 1 e sem duplicados. Mensagem do convite opcional.
- [ ] Etapa **Campos**: visualizar páginas renderizadas, navegar entre elas, selecionar pessoa, adicionar campo, arrastar, redimensionar e excluir.
  - Armazenar `pageNumber` e retângulo normalizado (0–1) relativo à página, conforme `GUIA-DE-TELAS.md`.
  - Limitar o campo aos limites da página no gesto.
  - Exigir ao menos um campo por pessoa (proposta do guia).
- [ ] Etapa **Revisão**: documento, pessoas, funções, campos por página, mensagem; botão Confirmar envio.
- [ ] Ao confirmar: botão desabilitado com "Enviando…"; chamar `createDocumentFlow`; em falha, manter os dados e oferecer "Tentar novamente". Não criar nova operação se a primeira já foi gravada (checar o documento criado antes de reenviar).
- [ ] Ocultar a barra inferior durante o fluxo.

**Pronto quando:** um PDF real é enviado com 2 signatários e campos, e os dados aparecem após reiniciar o app.

#### F3 — Assinatura pelo signatário (≈3 h)
- [ ] Abrir o documento de Pendentes: renderizar todas as páginas, destacar os campos do próprio usuário e bloquear edição de campos alheios.
- [ ] Tela de assinatura: `Canvas` com Limpar, prévia e Confirmar; Confirmar desabilitado com traço vazio.
- [ ] Ao confirmar: salvar o PNG em `filesDir/signatures/<docId>_<userId>.png`, gravar `signaturePath` e chamar `signDocument`.
- [ ] Bloquear reassinatura na UI (ver F0).
- [ ] Mensagem de falha "Não foi possível confirmar o salvamento." com nova tentativa, preservando o desenho.
- [ ] Após salvar: item sai de Pendentes e aparece em Assinados; status do documento atualizado.

**Pronto quando:** o signatário assina, o status muda para Parcialmente assinado ou Concluído, e a mudança persiste após reiniciar.

#### F4 — Acompanhamento e documento final (≈2 h)
- [ ] Admin: tela de detalhe com situação geral, progresso, situação de cada signatário e datas de assinatura.
- [ ] Linha do tempo com `getDocumentTimeline` (criação, envio, cada assinatura e conclusão).
- [ ] Gerar o PDF final ao concluir: renderizar cada página, desenhar as assinaturas PNG nas coordenadas normalizadas e salvar em `filesDir/final/`.
- [ ] Botões "Abrir PDF final" e "Salvar cópia" (`FileProvider` + `Intent.ACTION_VIEW`/`ACTION_CREATE_DOCUMENT`). Adicionar o `FileProvider` ao manifesto.
- [ ] Signatário: acesso ao PDF do próprio documento apenas depois de assinar ou nos Assinados.

**Pronto quando:** um documento concluído mostra a linha do tempo completa e abre um PDF com as assinaturas no lugar certo.

#### F5 — Acesso e versionamento (≈1,5 h)
- [ ] Na camada de repositório, checar acesso antes de devolver o documento ou o caminho do PDF: Admin criador ou signatário vinculado (ver 4.7).
- [ ] Documento enviado é somente leitura: a tela de detalhe não oferece edição.
- [ ] "Criar nova versão": abre o assistente pré-preenchido a partir do original, com `parentDocumentId` e `versionNumber + 1`, e novo fluxo de envio.
- [ ] Exibir a versão no detalhe e na lista.

**Pronto quando:** um signatário não consegue abrir documento que não recebeu, e alterar um documento enviado gera uma nova versão sem modificar a original.

#### F6 — Testes (contínuo; ≈2 h)
- [ ] `androidTest` com Room in-memory: criação de envio com campos (regressão do bug 5.1), `signDocument` nos cenários 1 de 2, 2 de 2 e reassinatura bloqueada.
- [ ] Testes de `UserRepository` (login, sessão, Admin-only, e-mail duplicado, senha mantida na edição).
- [ ] Testes unitários de funções puras: normalização de coordenadas e clamp dos campos dentro da página.
- [ ] Manter `PasswordHasherTest`.
- [ ] Remover `ExampleUnitTest` e `ExampleInstrumentedTest`.
- [ ] Checklist manual do fluxo completo (seção 8), executado em um aparelho ou emulador antes da gravação do vídeo.

#### F7 — Entregáveis (≈2 h, último bloco)
- [ ] Gerar o APK de debug (ou release assinado, se houver keystore).
- [ ] Gerar o `.zip` do código sem `build/`, `.gradle/`, `.idea/` e `local.properties`.
- [ ] Preencher os campos `[preencher]` no topo deste README.
- [ ] Apresentação (PPT ou PDF) com a identidade visual da solução: problemática, solução, tecnologias, fluxo, diferenciais e link do vídeo.
- [ ] Roteiro do vídeo (até 5 min) seguindo o fluxo da seção 8.

### 10.5 Estimativa e corte

| Fase | Estimativa | Obrigatória para a entrega |
| --- | --- | --- |
| F0 Correções | 1 h | Sim |
| F1 Sessão e telas base | 2 h | Sim |
| F2 Novo documento | 4 h | Sim |
| F3 Assinatura | 3 h | Sim |
| F4 Acompanhamento e PDF final | 2 h | Sim (versão mínima: status + linha do tempo, sem PDF final se faltar tempo) |
| F5 Acesso e versões | 1,5 h | Parcial: checagem de acesso sim; "nova versão" pode ser cortada |
| F6 Testes | 2 h | Parcial: DAO e envio com campos são prioritários |
| F7 Entregáveis | 2 h | Sim |
| **Total** | **≈17,5 h** | |

Se o tempo acabar, cortar nesta ordem e registrar o corte na apresentação: (1) "Criar nova versão"; (2) PDF final, mantendo apenas o status; (3) testes além do DAO de envio. Não cortar F0, F2, F3 nem a checagem de acesso, porque sem eles o fluxo principal não funciona ou a regra de acesso do enunciado fica descumprida.

### 10.6 Definição de pronto da Sprint 1

- Admin faz login, cadastra signatários e envia um PDF com título, pelo menos um signatário e campos.
- O envio sobrevive ao fechamento do app e não duplica ao tentar novamente.
- Signatário vê o documento pendente, assina e o status muda no banco.
- Admin acompanha o documento e vê o histórico.
- Campos ficam dentro da página e são gravados em coordenadas relativas.
- APK gerado, código zipado e apresentação com o link do vídeo.
## 11. Execução do plano: estado atual

O plano da seção 10 foi executado. Abaixo, o que foi feito em cada fase e o que ficou de fora.

| Fase | Situação | Observações |
| --- | --- | --- |
| F0 Correções | Feito | Envio com campos corrigido; duplicidade de `UserRepository` removida; assinatura repetida bloqueada; `proguard-rules.pro` criado |
| F1 Sessão e telas base | Feito | Setup do Admin, login, navegação por perfil, signatários, documentos com filtros, conta |
| F2 Novo documento | Feito, com ressalvas | PDF validado com `PdfRenderer`; pessoas, funções, campos, revisão e envio; ver limites abaixo |
| F3 Assinatura | Feito | Leitura com campos do próprio usuário, desenho, PNG salvo, status persistido |
| F4 Acompanhamento e PDF final | Feito | Participantes, histórico, PDF final com assinaturas nas coordenadas normalizadas |
| F5 Acesso e versões | Feito | Checagem de acesso antes de abrir documento/PDF; nova versão pré-preenchida |
| F6 Testes | Parcial | Unitários passam; instrumentados compilam e não foram executados |
| F7 Entregáveis | Parcial | APK de debug gerado (regenerado com a substituição de signatário e a assinatura já registrada); zip do código gerado; apresentação, vídeo e dados do grupo são do grupo |

### Mudanças de build necessárias para compilar

- `build.gradle.kts` raiz: removido o plugin `kotlin.android` (não existe no catálogo e o módulo usa `kotlin.compose`).
- `gradle-wrapper.properties`: Gradle 9.6.0 (exigido pelo AGP 9.5).
- `app/build.gradle.kts`: removido `kotlinOptions` (não suportado pelo AGP 9).
- `gradle.properties`: `android.disallowKotlinSourceSets=false`, porque o KSP adiciona fontes geradas ao `kotlin.sourceSets`.
- `libs.versions.toml`: KSP `2.2.10-2.0.2` (compatível com Kotlin 2.2.10) e Room `2.8.5` (a 2.6.1 falhava no KSP com `unexpected jvm signature V`).
- `AndroidManifest.xml`: `LetsSignApp` e `FileProvider` para abrir PDFs no app de visualização.

### Entregas recentes

- **Substituir signatário** na etapa Pessoas, com reassociação dos campos já posicionados (`NewDocumentViewModel.replaceSignatory`).
- **Assinatura já registrada**: a tela de assinatura recarrega o registro existente em vez de mostrar erro (`SignatureViewModel.save`).
- APK de debug regenerado com `./gradlew :app:assembleDebug`; testes unitários passando.

### Limites conhecidos (não implementados)

- Na etapa **Pessoas** não há cadastro de signatário no meio do fluxo; é preciso cadastrar antes, na aba Signatários.
- O editor de campos não tem zoom nem alternativa de teclado para mover campos (o guia pede as duas coisas por acessibilidade).
- O reenvio após falha não consulta uma operação anterior: a gravação é uma transação, então uma falha não deixa dados parciais e pode ser repetida.
- Migrações do Room são destrutivas (`fallbackToDestructiveMigration`); aceitável antes da entrega, não para produção.
- Nenhum fluxo foi testado manualmente em aparelho ou emulador. A compilação e os testes unitários passaram; os instrumentados precisam de um dispositivo.

### Como validar antes da entrega

1. Instalar o APK de debug.
2. Configurar o Admin, cadastrar dois signatários.
3. Enviar um PDF real com título, dois signatários, um campo para cada um.
4. Sair, entrar como um signatário, assinar. Confirmar "Parcialmente assinado".
5. Entrar com o outro signatário e assinar. Confirmar "Concluído" e abrir o PDF final.
6. Reiniciar o app e conferir que os dados continuam.
