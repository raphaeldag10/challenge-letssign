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
| Base do projeto Android (Compose, Room, KSP) | Pronta |
| Persistência local (Room, 5 entidades, 2 DAOs) | Pronta, com bug no fluxo de envio (ver seção 5) |
| Autenticação e perfis (Admin / Signatário) | Lógica pronta, sem telas |
| Cadastro e edição de signatários | Lógica pronta, sem telas |
| Assinatura de documento (status) | Lógica pronta; falta capturar e salvar a assinatura |
| Telas do aplicativo | **Não implementadas** (`MainActivity` ainda é o template "Hello Android!") |
| Manipulação de PDF (seleção, validação, páginas, campos) | **Não implementada** |
| Testes | Apenas `PasswordHasherTest`; demais testes são templates |

Conclusão: a camada de dados está bem avançada, mas a experiência do usuário e o tratamento de PDF ainda não existem. Esses são os maiores itens pendentes.

---

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

## 3. O que já está implementado

### Projeto e configuração
- Projeto Android com Jetpack Compose e Material 3 (`app/build.gradle.kts`).
- `minSdk 28`, `targetSdk 37`, Java/Kotlin 17.
- Room 2.6.1 com KSP e Lifecycle ViewModel Compose.
- Tema da aplicação (`ui/theme/`).

### Banco de dados Room (`app/src/main/java/br/com/fiap/letssign/data/local/`)
Banco `letssign_database`, versão 1, com as entidades:

| Entidade (tabela) | Função |
| --- | --- |
| `UserEntity` (`users`) | Usuário com nome, e-mail (único), telefone, hash da senha e papel (`ADMIN` / `SIGNATORY`) |
| `DocumentEntity` (`documents`) | Documento com título, `filePath`, status (`PENDING`, `PARTIALLY_SIGNED`, `COMPLETED`), criador e data |
| `DocumentSignatoryEntity` (`document_signatories`) | Vínculo documento ↔ usuário com papel (`CONTRATANTE`, `CONTRATADO`, `TESTEMUNHA`), `hasSigned` e `signedAt` |
| `SignatureFieldEntity` (`signature_fields`) | Campo de assinatura: página e retângulo normalizado (x, y, largura, altura) |
| `DocumentHistoryEntity` (`document_history`) | Linha do tempo com tipo de evento, descrição e timestamp |

DAOs:
- `UserDao`: criação transacional do primeiro Admin (`createInitialAdmin`, recusa um segundo Admin), CRUD de usuário, busca por e-mail/ID, lista de signatários.
- `DocumentDao`: criação transacional do envio (`insertFullDocumentFlow`), consultas de documentos por status e por signatário (pendentes/assinados), campos, signatários e histórico.

Conversores de tipo em `Converters.kt` para os enums.

### Autenticação e cadastro (`data/repository/UserRepository.kt`)
- `login(email, senha)` retorna `LoggedUser` (id, nome, e-mail, papel) e guarda a sessão em memória; `logout()` encerra.
- `setupInitialAdmin(...)`: configuração inicial, sem senha padrão no código.
- `registerSignatory(...)` e `updateSignatory(...)`: exclusivos para sessão ADMIN.
- Validações: nome com ao menos 3 caracteres, e-mail válido (normalizado em minúsculas), telefone com 10 a 15 dígitos, senha com ao menos 8 caracteres; e-mail duplicado é recusado.
- Na edição, senha em branco mantém a anterior.

### Senhas (`PasswordHasher.kt`)
- PBKDF2-HMAC-SHA256, 210.000 iterações, salt aleatório de 16 bytes, comparação em tempo constante.
- Coberto por `PasswordHasherTest` (acerto, erro, salts diferentes, hash inválido).

### Fluxo de assinatura (`data/repository/RoomDocumentRepository.kt`)
- `signDocument(documentId, userId)` marca a participação como assinada, registra evento `SIGNED` e recalcula o status: `COMPLETED` quando todos assinam, `PARTIALLY_SIGNED` quando alguns assinam, com evento `COMPLETED` ao final.

### Contratos
- `domain/repository/DocumentRepository.kt` define a interface de documentos (base para a futura troca por API REST).
- Uma interface `UserRepository` também existe em `domain/` (ver seção 5, item sobre duplicidade).

### Documentação
- `design/`: escopo da Sprint 1 (`ESCOPO-SPRINT-1.md`), guia de telas com 16 telas propostas (`GUIA-DE-TELAS.md`), pranchas PNG e pacote de design.
- `docs/LOGIN-LOCAL.md`: como usar o login local.

---

## 4. O que falta implementar

Prioridade: **P1** = necessário para o fluxo mínimo da Sprint 1 funcionar; **P2** = requisito do enunciado com implementação parcial; **P3** = melhoria ou acabamento.

### 4.1 Interface e navegação (P1)
- [ ] Substituir `MainActivity` (template "Hello Android!") por navegação real.
- [ ] Tela de login com e-mail e senha, com mensagem "E-mail ou senha incorretos." e roteamento por perfil.
- [ ] Tela de configuração inicial do Admin (`setupInitialAdmin`).
- [ ] Admin: lista de documentos com filtros (Todos, Enviados, Parciais, Concluídos) e botão "Novo documento".
- [ ] Admin: lista de signatários, cadastro e edição (telas 03 e 04 do guia).
- [ ] Signatário: abas Pendentes e Assinados, com "Abrir documento".
- [ ] Conta: identificação, perfil e "Sair".
- [ ] ViewModels ligando telas aos repositórios (`LoggedUser` deve ser exposto ao restante da UI).

### 4.2 Novo documento, PDF e campos (P1)
- [ ] Seletor de arquivo PDF (Storage Access Framework).
- [ ] Validação do PDF (formato real, abertura, protegido/corrompido) antes de liberar "Continuar".
- [ ] Copiar o PDF para o armazenamento interno do app e gravar caminho/data/status (opção 1 do enunciado) **ou** gravar o conteúdo como BLOB (opção 2). Escolher uma; o `DocumentEntity.filePath` já existe.
- [ ] Renderização das páginas (miniaturas e visualização) — por exemplo, `PdfRenderer` do Android ou PDFBox-Android.
- [ ] Editor de campos: adicionar, arrastar, redimensionar e excluir campos por página; cor/identificação por signatário.
- [ ] Conversão entre coordenadas da tela e coordenadas normalizadas (0 a 1, relativas à página visível), conforme o contrato em `GUIA-DE-TELAS.md`.
- [ ] Validação: campos dentro dos limites da página e sem sobreposição (recomendado no guia).
- [ ] Tela de revisão com documento, signatários, funções, campos por página e mensagem do convite.
- [ ] Mensagem de convite (campo no documento; hoje não existe no `DocumentEntity`).

### 4.3 Regras de negócio no envio (P1)
- [ ] Impedir envio sem título.
- [ ] Impedir envio sem **pelo menos um** signatário (hoje `insertFullDocumentFlow` aceita lista vazia).
- [ ] Impedir o mesmo usuário duplicado como signatário no mesmo documento.
- [ ] Exigir ao menos um campo por signatário (proposta do guia; não é regra explícita do enunciado).
- [ ] Impedir campos sem signatário vinculado.
- [ ] Tratamento de falha no envio com "Tentar novamente" sem criar solicitações duplicadas (idempotência; hoje há apenas a transação do Room).

### 4.4 Assinatura pelo signatário (P1)
- [ ] Leitura do PDF completo pelo signatário, com destaque dos próprios campos.
- [ ] Área de desenho para a assinatura (toque/dedo), botões Limpar e Confirmar.
- [ ] Persistir a assinatura (imagem ou traço) e aplicá-la aos campos do signatário; hoje `signDocument` só altera status.
- [ ] Gerar o documento final com as assinaturas aplicadas (necessário para "documento final, quando concluído").
- [ ] Bloquear nova assinatura da mesma participação (ver bug 5.3).

### 4.5 Acompanhamento e histórico (P2)
- [ ] Tela de acompanhamento: situação geral, progresso, situação individual, datas e horários.
- [ ] Linha do tempo de criação, envio, cada assinatura e conclusão (queries prontas em `getDocumentTimeline`).
- [ ] Acesso ao documento final quando concluído.

### 4.6 Preservação e versionamento (P2)
- [ ] Documento enviado não editável; alterações criam **nova versão e novo fluxo**. Hoje `DocumentEntity` não tem campo de versão nem referência ao documento original.

### 4.7 Controle de acesso (P2)
- [ ] Verificar, antes de listar ou abrir cada documento, se o usuário logado é criador (Admin) ou signatário vinculado. `getDocumentById` e a abertura do PDF ainda não fazem essa checagem (ver `docs/LOGIN-LOCAL.md`).

### 4.8 Repositórios e persistência (P2)
- [ ] Interface de usuário com métodos reais: o `domain/UserRepository` e o `RoomUserRepository` não são usados (ver 5.2).
- [ ] Expor lista de signatários no `data.UserRepository` (hoje só existe no DAO).
- [ ] Decidir e documentar a estratégia de migração do Room (`fallbackToDestructiveMigration()` apaga dados a cada mudança de versão).

### 4.9 Testes e qualidade (P2)
- [ ] Testes de DAO/repositório com Room em memória (criação de envio, assinatura, transições de status, validações de cadastro).
- [ ] Testes de `UserRepository` (login, sessão, permissão do Admin, e-mail duplicado).
- [ ] Remover ou substituir `ExampleUnitTest` e `ExampleInstrumentedTest` (templates sem valor).
- [ ] Teste instrumentado do fluxo completo: Admin envia → signatário assina → status `COMPLETED`.

### 4.10 Entregáveis (P1)
- [ ] Gerar o APK de release/debug para entrega.
- [ ] Criar o `.zip` do código-fonte (sem `build/`, `.gradle/` e `local.properties`).
- [ ] Apresentação (PPT ou PDF) com nome do projeto, integrantes e RMs, problemática, solução, tecnologias, fluxo, diferenciais e link do vídeo; identidade visual da solução.
- [ ] Vídeo demonstrativo de até 5 minutos com as principais funcionalidades.

### 4.11 Build (P3)
- [ ] `app/build.gradle.kts` referencia `proguard-rules.pro`, que **não existe** no projeto. Criar o arquivo ou remover a referência antes de gerar build de release.

---

## 5. Problemas encontrados no código atual

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
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/br/com/fiap/letssign/
│       │       ├── MainActivity.kt                 # template, a ser substituído
│       │       ├── data/
│       │       │   ├── local/
│       │       │   │   ├── converter/Converters.kt
│       │       │   │   ├── dao/DocumentDao.kt, UserDao.kt
│       │       │   │   └── entity/                 # 5 entidades Room + AppDatabase.kt
│       │       │   └── repository/                 # UserRepository, RoomDocumentRepository,
│       │       │                                   # RoomUserRepository, PasswordHasher
│       │       ├── domain/repository/              # interfaces DocumentRepository, UserRepository
│       │       └── ui/theme/                       # tema Compose
│       └── test/                                   # PasswordHasherTest + templates
├── design/                                         # escopo, guia de telas, pranchas
├── docs/LOGIN-LOCAL.md                             # uso do login local
└── gradle/libs.versions.toml                       # versões das dependências
```

---

## 7. Como executar

Requisitos: Android Studio recente, JDK 17 e Android SDK com API 37.

```bash
./gradlew assembleDebug      # gera o APK de debug
./gradlew testDebugUnitTest  # roda os testes unitários
```

O arquivo `local.properties` (caminho do SDK) é local e não deve ser versionado.

Observação: o app ainda não tem telas de fluxo; a execução atual mostra apenas o template inicial.

---

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
