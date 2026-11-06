# LetsSign

Aplicativo Android nativo para **preparar, enviar, acompanhar e assinar documentos PDF** diretamente no celular. Um Admin monta o fluxo de assinatura (PDF, participantes, campos de assinatura), e os signatários assinam pelo mesmo app, com desenho, nome digitado ou certificado digital ICP-Brasil.

Desenvolvido como Sprint 1 do Challenge LetsSign (FIAP, 1º semestre de 2026, turma 2TDS).

> **Integrantes e RM:** [preencher]
> **Link do vídeo demonstrativo (até 5 min):** [preencher]

---

## Sumário

1. [Funcionalidades](#1-funcionalidades)
2. [Tecnologias](#2-tecnologias)
3. [Arquitetura](#3-arquitetura)
4. [Modelo de dados](#4-modelo-de-dados)
5. [Fluxos principais](#5-fluxos-principais)
6. [Estrutura do projeto](#6-estrutura-do-projeto)
7. [Como executar e testar](#7-como-executar-e-testar)
8. [Limitações conhecidas](#8-limitações-conhecidas)
9. [Documentação complementar](#9-documentação-complementar)

---

## 1. Funcionalidades

### 1.1 Acesso e perfis

| Perfil | O que faz |
| --- | --- |
| **Admin** | Configura o primeiro acesso (cria a conta Admin), cadastra e edita signatários, envia documentos, acompanha o andamento e cria novas versões |
| **Signatário** | Vê documentos pendentes e assinados, lê o PDF com os próprios campos destacados, assina e pede resumo/respostas sobre o documento |

- **Login por e-mail e senha** (PBKDF2-HMAC-SHA256, 210.000 iterações, salt de 16 bytes).
- **Login por código de uso único** para signatários cadastrados nesse modo. O código é exibido na tela, sem envio de e-mail, e é invalidado após o primeiro uso.
- Sessão observável em `StateFlow`. Somente o Admin cadastra ou edita signatários.
- Cada signatário pode atualizar o próprio perfil e cadastrar o CPF. Se o Admin travar o CPF no cadastro, o signatário não consegue alterá-lo.
- Tema claro e escuro.

### 1.2 Cadastro de signatários (Admin)

- Busca, cadastro e edição com nome, e-mail, telefone e forma de login (senha ou código).
- Validações com mensagens na tela. E-mail duplicado é recusado.
- Em edição, senha em branco mantém a senha atual.
- CPF validado pelo dígito verificador (`Cpf.kt`), com máscara na tela.

### 1.3 Novo documento (Admin)

Assistente em quatro passos, com a barra inferior oculta:

1. **Documento**: título obrigatório e PDF de origem. O PDF pode vir de:
   - **Arquivo**: seletor de arquivos limitado a PDFs.
   - **Foto**: scanner do Google ML Kit (recorte, correção de perspectiva e filtros) gera um PDF a partir de uma ou mais fotos de páginas.
   - PDFs inválidos, protegidos ou corrompidos são recusados com mensagem. Um arquivo inválido não substitui o anterior nem os campos já posicionados.
2. **Pessoas**: seleção múltipla de signatários, com função por pessoa (**Contratante**, **Contratado** ou **Testemunha**). Remover quem tem campos pede confirmação. **Substituir** troca a pessoa, mantém a função e reassocia os campos já posicionados. Mensagem do convite opcional.
3. **Campos**: visualização página a página. Adicionar, arrastar, redimensionar e excluir campos, cada um associado a um participante. Campos não saem da página e sobreposições são detectadas.
4. **Revisão**: resumo do documento, participantes, campos por página e mensagem, com o botão **Confirmar envio**. Enquanto o envio roda, o botão fica desabilitado ("Enviando…") para evitar toques repetidos.

Regras de envio, validadas no repositório dentro de uma transação:
- título e PDF obrigatórios;
- pelo menos um signatário, sem repetição;
- pelo menos um campo por signatário;
- campos dentro dos limites da página;
- modos de assinatura aceitos pelo documento (desenho, nome digitado e/ou certificado) pelo menos um.

### 1.4 Acompanhamento (Admin)

- Lista de documentos com filtros **Todos**, **Enviados**, **Parciais** e **Concluídos**, progresso e estados vazios.
- Detalhe com situação geral, situação individual de cada participante, datas de assinatura e **linha do tempo** (criação, envio, cada assinatura e conclusão).
- Abertura do PDF original e do PDF final.
- **Nova versão**: cria um novo fluxo a partir de um documento enviado, pré-preenchido, com novo arquivo. O documento original não é alterado. Cada versão aponta para a anterior (`parentDocumentId`) e incrementa `versionNumber`.

### 1.5 Assinatura (Signatário)

- Abas **Pendentes** e **Assinados**.
- Leitura do PDF com os campos do próprio usuário em destaque.
- Três modos de assinatura, liberados pelo Admin por documento:
  - **Desenho**: assinatura com o dedo, com Limpar e Confirmar. Salva como PNG recortado.
  - **Nome digitado**: nome estilizado em três tipografias, salvo como PNG.
  - **Certificado**: certificado **A1 ICP-Brasil** (`.p12` ou `.pfx`) no CPF cadastrado. O arquivo não é guardado no app. Veja a seção 3.4.
- Bloqueio de reassinatura. Se a assinatura já estiver registrada (outro acesso ou toque repetido), o app recarrega o registro e informa isso, sem assinar de novo.
- Quando todos os participantes assinam, o status passa a **Concluído** e o **PDF final** é gerado com as assinaturas desenhadas nos campos, proporcionais e na posição normalizada.

### 1.6 Resumo e dúvidas com IA (Signatário)

- Tela **Resumo e dúvidas** para cada documento: gera um resumo e responde perguntas sobre o conteúdo do PDF.
- O resumo fica em cache e só é regenerado se o texto do PDF mudar (comparação por SHA-256).
- O chat é salvo no banco local, por documento e por usuário, e envia até as últimas 10 mensagens como contexto.
- Só funciona com PDFs que têm texto selecionável. PDFs escaneados retornam aviso.
- **Atenção:** nesta versão o motor é **simulado** (`MockDocumentAssistant`, sem rede). Ele devolve as primeiras frases como resumo e trechos que contêm palavras da pergunta. A interface `DocumentAssistant` está pronta para receber um motor real, como a API da Claude.

### 1.7 Acesso a documentos

- Um usuário só acessa documentos que criou ou dos quais é participante. A checagem (`canAccess`) é feita antes de abrir o documento, o PDF, o resumo ou o chat.

---

## 2. Tecnologias

| Área | Tecnologia |
| --- | --- |
| Linguagem | Kotlin 2.2.10, Java 17 (target) |
| UI | Jetpack Compose, Material 3, Activity Compose |
| Estado | ViewModel (Lifecycle 2.8.7), `StateFlow` e `Flow` |
| Persistência | Room 2.8.5 (SQLite), KSP 2.2.10-2.0.2 |
| PDF: renderização e validação | `android.graphics.pdf.PdfRenderer` |
| PDF: texto (assistente) | PDFBox-Android 2.0.27.0 (`com.tom-roush`) |
| PDF: geração do final | `android.graphics.pdf.PdfDocument` |
| Assinatura digital | BouncyCastle 1.86 (CAdES-BES destacado, PKCS#12) |
| Digitalização | Google ML Kit Document Scanner 16.0.0 (depende do Google Play Services) |
| Build | Gradle 8.11.1 (wrapper), AGP 8.10.0, Kotlin DSL |
| Plataforma | `minSdk 28`, `compileSdk 35`, `targetSdk 35` |

Não há backend. Todos os dados ficam no aparelho.

---

## 3. Arquitetura

O app segue uma arquitetura em camadas, com injeção manual de dependências.

```
┌──────────────────────────────────────────────────────────────┐
│ UI (Compose)                                                 │
│ AppRoot (navegação em pilha) · auth · admin · newdoc ·       │
│ signatory · common                                           │
└───────────────┬──────────────────────────────┬───────────────┘
                │ ViewModels / chamadas        │
┌───────────────▼──────────────────────────────▼───────────────┐
│ Serviços de aplicação (data/repository)                      │
│ UserRepository · RoomDocumentRepository · DocumentService ·  │
│ AiAssistantService                                           │
└───────┬─────────────────────┬────────────────────┬───────────┘
        │ contrato            │                    │
┌───────▼──────────┐  ┌───────▼────────────┐  ┌────▼──────────────────┐
│ domain/          │  │ data/file          │  │ data/signature        │
│ DocumentRepository│ │ PdfPages           │  │ IcpBrasilSigner       │
│ DocumentAssistant│  │ PdfTextExtractor   │  │ (CAdES, PKCS#12)      │
└───────┬──────────┘  │ FileStorage        │  └───────────────────────┘
        │             └──────┬─────────────┘
┌───────▼─────────────────────▼────────────────────────────────┐
│ data/local (Room v9)                                         │
│ AppDatabase · DAOs · entidades · projeções                   │
└──────────────────────────────────────────────────────────────┘
        Armazenamento privado do app (filesDir):
        documents/ (PDFs) · signatures/ (PNGs) · final/ (PDFs finais)
```

### 3.1 Composição

`LetsSignApp` cria um `AppContainer` (preguiçoso, uma instância por processo). Ele monta as dependências na ordem:

- `AppDatabase` (singleton de Room, nome `letssign_database`);
- `UserRepository` sobre `UserDao`;
- `RoomDocumentRepository` sobre o banco, implementando `DocumentRepository`;
- `FileStorage` (PDFs, assinaturas e PDF final);
- `DocumentService` (combina repositório e arquivos);
- `AiAssistantService`, com o extrator de texto e o `MockDocumentAssistant`.

### 3.2 Camadas

- **`ui/`**: telas Compose e ViewModels. `AppRoot` mantém uma pilha de rotas: sem Admin, abre a configuração inicial; sem sessão, abre o login; depois, a home do perfil.
- **`data/repository/`**: regras de negócio.
  - `UserRepository`: login, sessão, perfis, CPF, validações de signatário.
  - `RoomDocumentRepository`: envio atômico, assinatura, acesso e consultas.
  - `DocumentService`: gera o PDF final quando o documento conclui e reaproveita o arquivo depois.
  - `AiAssistantService`: confere acesso, extrai o texto, usa o cache de resumo e grava o chat.
- **`domain/`**: contratos independentes de Room.
  - `DocumentRepository`: contrato de documentos. Uma futura API REST (Sprint 2) pode implementá-lo sem mudar as telas.
  - `DocumentAssistant`: contrato do motor de IA.
- **`data/local/`**: Room, com entidades, DAOs e projeções para listas e detalhes.
- **`data/file/`**: manipulação de PDF e arquivos.
- **`data/signature/`**: assinatura ICP-Brasil.

### 3.3 Decisões de arquitetura

- **PDF no sistema de arquivos, metadados no banco.** O PDF é copiado para `filesDir/documents/` e o caminho fica em `DocumentEntity.filePath`. O enunciado permite essa opção, e ela evita guardar arquivos grandes em BLOB.
- **Coordenadas normalizadas.** Cada campo guarda página e retângulo entre 0 e 1, relativos à página. Assim a posição se mantém em telas de tamanhos diferentes. O PDF final e a tela usam essas mesmas coordenadas.
- **Campo referencia o participante pelo `userId`.** No envio, o DAO troca o `userId` pelo ID gerado do participante dentro da mesma transação. Isso evita violação de chave estrangeira.
- **Transações.** Envio (`createDocumentFlow`) e assinatura (`signDocument`) são transacionais. Uma falha não deixa dados parciais e pode ser repetida sem duplicar o fluxo.
- **Situação calculada, não digitada.** A situação do documento (Enviado, Parcialmente assinado, Concluído) é recalculada a cada assinatura, a partir dos participantes.
- **Migrações do Room.** As versões 3 a 9 têm migrações reais, que preservam os dados. Versões anteriores à 3 são recriadas (`fallbackToDestructiveMigration`).

### 3.4 Assinatura com certificado ICP-Brasil

`IcpBrasilSigner` gera uma assinatura **CAdES-BES destacada** sobre o PDF original:

1. Carrega o certificado A1 (PKCS#12) com a senha informada.
2. Valida o certificado: validade na data atual, uso para assinatura, política ICP-Brasil (OID `2.16.76.1.2.*`) e CPF do certificado, que deve ser o CPF cadastrado do signatário.
3. Gera o CMS destacado e confere a assinatura com a chave pública antes de devolvê-lo.
4. O banco guarda o CMS em Base64, o número de série e o emissor. O arquivo `.p12` e a senha não são salvos.

**Escopo do MVP:** não valida a cadeia até a AC Raiz, não consulta LCR nem OCSP e não inclui carimbo do tempo. O PDF original não é alterado pela assinatura. Ela fica separada, no banco.

### 3.5 Digitalização

A foto de documento usa o scanner do ML Kit em modo FULL, com saída em PDF e sem importação da galeria. O scanner abre a câmera pelo Google Play Services, e o app não pede permissão de câmera própria. O resultado é copiado para o armazenamento privado. Não há OCR; o texto só é extraído quando o PDF já tem texto selecionável.

Detalhes: [`docs/DIGITALIZACAO.md`](docs/DIGITALIZACAO.md).

---

## 4. Modelo de dados

Banco `letssign_database`, **versão 9**. Chaves estrangeiras com `CASCADE` ou `RESTRICT`, conforme o caso.

| Tabela (entidade) | Conteúdo principal |
| --- | --- |
| `users` (`UserEntity`) | Nome, e-mail (único), telefone, CPF, papel (`ADMIN` / `SIGNATORY`), forma de login (`PASSWORD` / `EMAIL_CODE`), hash da senha, código de login temporário e flag de CPF travado pelo Admin |
| `documents` (`DocumentEntity`) | Título, `filePath`, `fileName`, status, criador, mensagem do convite, `versionNumber`, `parentDocumentId`, `finalFilePath`, modos de assinatura aceitos |
| `document_signatories` (`DocumentSignatoryEntity`) | Participante (`documentId`, `userId`), função, `hasSigned`, `signedAt`, `signaturePath` (PNG), `signatureMethod`, `signerName`, `signatureCms`, `certificateSerial`, `certificateIssuer` |
| `signature_fields` (`SignatureFieldEntity`) | Campo: participante, página e retângulo normalizado (0 a 1) |
| `document_history` (`DocumentHistoryEntity`) | Linha do tempo: criação, envio, assinaturas, situação parcial e conclusão |
| `document_ai_summaries` (`DocumentAiSummaryEntity`) | Resumo em cache, com hash do texto de origem |
| `ai_messages` (`AiMessageEntity`) | Chat com o assistente, por documento e usuário |

Enums principais:
- `DocumentStatus`: `PENDING`, `PARTIALLY_SIGNED`, `COMPLETED`.
- `SignatoryRoleType`: `CONTRATANTE`, `CONTRATADO`, `TESTEMUNHA`.
- `SignatureMethod`: `DRAW`, `TYPED`, `CERTIFICATE`.
- `UserRole`: `ADMIN`, `SIGNATORY`.

---

## 5. Fluxos principais

### 5.1 Envio de documento

```
Admin → Novo documento
  → Documento: título + PDF (arquivo ou foto → ML Kit → PDF)
       PdfPages valida o PDF (cabeçalho, abertura, páginas, proteção)
  → Pessoas: participantes + função + modos de assinatura
  → Campos: página, posição e tamanho (normalizados)
  → Revisão → Confirmar envio
       RoomDocumentRepository.createDocumentFlow (transação):
         valida regras → grava documento, participantes, campos e histórico
       FileStorage copia o PDF para filesDir/documents/
```

### 5.2 Assinatura

```
Signatário → Pendentes → abre o documento (canAccess)
  → Campos próprios em destaque
  → Assinar: Desenho | Nome digitado | Certificado ICP-Brasil
       PNG salvo em filesDir/signatures/   (ou CMS destacado para o certificado)
  → RoomDocumentRepository.signDocument (transação):
       bloqueia reassinatura → grava participação → registra evento
       → recalcula situação: PENDING → PARTIALLY_SIGNED → COMPLETED
  → Se COMPLETED: DocumentService.ensureFinalPdf gera o PDF final
```

### 5.3 Acompanhamento e nova versão

- O Admin vê situação, participantes, histórico e PDFs (original e final).
- "Nova versão" abre o assistente com os dados do documento original e um novo arquivo. O envio gera um novo registro com `parentDocumentId` e `versionNumber + 1`. O original não muda.

---

## 6. Estrutura do projeto

```
challenge-letssign/
├── app/
│   ├── src/main/java/br/com/fiap/letssign/
│   │   ├── LetsSignApp.kt, AppContainer.kt   # Application e injeção manual
│   │   ├── MainActivity.kt                   # Entrada; chama AppRoot
│   │   ├── data/
│   │   │   ├── local/                        # Room: AppDatabase, entity/, dao/, model/, converter/
│   │   │   ├── repository/                   # UserRepository, RoomDocumentRepository,
│   │   │   │                                 # DocumentService, AiAssistantService, Cpf, PasswordHasher
│   │   │   ├── file/                         # PdfPages, PdfTextExtractor, FileStorage
│   │   │   ├── signature/                    # IcpBrasilSigner (CAdES/PKCS#12)
│   │   │   └── ai/                           # MockDocumentAssistant
│   │   ├── domain/
│   │   │   ├── repository/DocumentRepository.kt   # Contrato de documentos
│   │   │   └── ai/DocumentAssistant.kt            # Contrato do assistente
│   │   └── ui/
│   │       ├── AppRoot.kt                    # Rotas e navegação em pilha
│   │       ├── auth/                         # Login, configuração inicial, perfil
│   │       ├── admin/                        # Documentos, detalhe, signatários, conta
│   │       ├── newdoc/                       # Assistente de envio, campos, origem do PDF
│   │       ├── signatory/                    # Pendentes/assinados, leitura, assinatura, resumo com IA
│   │       ├── common/                       # Componentes, sobreposição de campos, formulários
│   │       └── theme/                        # Cores, tipografia, tema claro/escuro
│   ├── src/test/                             # Testes unitários (JVM)
│   ├── src/androidTest/                      # Testes instrumentados
│   └── src/main/res/                         # Ícones, strings, temas, file_paths.xml
├── design/                                   # Escopo da Sprint 1, guia de telas, pranchas
├── docs/                                     # LOGIN-LOCAL.md, DIGITALIZACAO.md
└── gradle/libs.versions.toml                 # Catálogo de versões
```

### Testes

| Arquivo | Cobertura |
| --- | --- |
| `PasswordHasherTest` | Hash e verificação PBKDF2 |
| `FieldGeometryTest` | Movimento, redimensionamento e sobreposição de campos |
| `CpfTest`, `CpfVisualTransformationTest` | Validação de CPF e máscara |
| `UserRepositoryProfileTest` | Perfil do usuário, CPF travado, validações |
| `AllowedSignatureMethodsTest` | Conversão dos modos de assinatura |
| `IcpBrasilSignerTest` | Assinatura CAdES, validações do certificado, verificação |
| `MockDocumentAssistantTest`, `AiAssistantServiceTest` | Resumo, chat, cache e controle de acesso |
| `DocumentFlowInstrumentedTest` | Envio com campos, rejeição sem campo, transições de situação, reassinatura bloqueada, acesso |
| `AiMigrationInstrumentedTest` | Migração do banco para a versão com IA |
| `DocumentSourcePickerTest`, `ScannedPdfImportTest` | Escolha de origem do PDF e importação do scanner |

---

## 7. Como executar e testar

**Requisitos:** Android Studio recente, JDK 17 e Android SDK com API 35. O Gradle baixa o que faltar. Para a digitalização por foto, o aparelho precisa de Google Play Services e de pelo menos 1,7 GB de RAM.

```bash
./gradlew :app:assembleDebug                  # APK em app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest              # testes unitários (JVM, sem emulador)
./gradlew :app:connectedDebugAndroidTest      # testes instrumentados (requer dispositivo ou emulador)
```

`local.properties` (caminho do SDK) é local e não deve ser versionado.

### Roteiro de demonstração

1. Instale o APK e crie a conta do Admin na tela de configuração inicial.
2. Cadastre dois signatários. Um pode entrar por senha e o outro por código.
3. Envie um PDF com título, os dois signatários e um campo para cada um. Teste também a opção **Foto**.
4. Saia, entre como o primeiro signatário, assine e confirme "Parcialmente assinado". Teste o resumo com IA.
5. Entre com o segundo signatário, assine e confirme "Concluído". Abra o PDF final.
6. Reinicie o app e confira que os dados continuam.

---

## 8. Limitações conhecidas

- **Sem backend nem sincronização.** Os dados ficam no aparelho. Um documento enviado só aparece para os signatários **naquele mesmo aparelho**. A camada REST é prevista para a Sprint 2.
- **Sem notificações.** Nenhum e-mail, SMS ou push é enviado. O código de login é exibido na tela.
- **Assistente de IA simulado.** As respostas vêm de regras locais, não de um modelo de linguagem. A interface está pronta para um motor real.
- **Assinatura com certificado parcial.** Não valida a cadeia até a AC Raiz, não consulta LCR/OCSP e não inclui carimbo do tempo.
- **Sem OCR.** PDFs escaneados ou fotos não têm texto pesquisável para o assistente.
- **Migrações antigas destrutivas.** Versões do banco anteriores à 3 são recriadas. Migrações posteriores preservam os dados.
- **Build de release sem assinatura.** O APK gerado é de debug. O ProGuard está desabilitado (`isMinifyEnabled = false`).
- **Acessibilidade em aperfeiçoamento.** O editor de campos ainda não tem zoom nem alternativa sem arrastar, e nem todos os elementos têm rótulos para leitor de tela.

