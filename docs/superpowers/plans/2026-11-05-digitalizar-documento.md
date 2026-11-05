# Digitalização de documentos pela câmera

**Objetivo:** na branch `codex/digitalizar-documento-camera`, tocar em Selecionar PDF ou Trocar arquivo oferece Foto e Arquivo. Foto abre o scanner com câmera, recorte, correção de perspectiva e filtros; ao confirmar, o PDF entra no assistente existente. Arquivo mantém a seleção de PDF atual.

**Arquitetura:** Google ML Kit Document Scanner 16.0.0, modo FULL, saída apenas PDF e importação da galeria desativada. O scanner permite fotografar várias páginas. O PDF retornado é copiado e validado pelo FileStorage existente antes de substituir o rascunho. Cancelamento preserva o documento e os dados preenchidos. A preparação do scanner bloqueia novos cliques e a navegação para a próxima etapa.

**Restrições:** Android mínimo 28 do projeto; scanner requer Google Play Services e pelo menos 1,7 GB de RAM. Pode haver download do módulo no primeiro uso. Falhas oferecem nova tentativa ou a opção Arquivo. Não há reconhecimento de texto/OCR nesta entrega.

## Execução

- [x] Verificar testes existentes e ambiente Android.
- [x] Escrever e executar testes de interface: abrir escolha Foto/Arquivo, cancelar preservando rascunho e validar o encaminhamento de Arquivo ao seletor de PDFs.
- [x] Criar `DocumentSourcePicker.kt` com diálogo e integração de Activity Result; adicionar dependência ML Kit no catálogo e módulo.
- [x] Integrar a escolha à etapa Documento; importar a saída PDF pelo ViewModel existente, tratando resultado vazio, falha de abertura e cancelamento.
- [x] Testar importação de PDF de duas páginas no armazenamento privado, continuidade do rascunho, cancelamento da escolha e preservação do PDF anterior em importação inválida. Cancelamento/erro do SDK revisados estaticamente; captura real pendente de celular.
- [x] Executar testes unitários, testes instrumentados, lint e build do APK; revisar diff e documentar validação em `docs/DIGITALIZACAO.md`.

## Verificação

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`

`gradlew.bat :app:connectedDebugAndroidTest` com emulador disponível. A câmera e o download real do módulo também devem ser conferidos em celular com Google Play Services.

## Progresso

- Branch criada a partir de `main` (`6980e4d`); mudanças preexistentes em `.idea`, `design`, `output` e `tmp` preservadas.
- Gradle apresentou socket Unix de loopback inválido. Verificação executada com JDK 17 e fallback TCP por variável temporária, sem alterar configuração Java do repositório.
- RED: três testes de interface falharam pela ausência da escolha de origem. GREEN: 39 unitários e 10 instrumentados passaram; APK gerado e lint sem erros.
- Corrigidos bloqueios preexistentes: argumentos obrigatórios ausentes nos testes de `signDocument` e chamada `readNBytes` incompatível com minSdk 28. O contrato `OpenDocument` usa tipo `*/*` e extra `MIME_TYPES` limitado a PDF; o teste valida ambos.
- Revisão independente somente leitura concluída sem problemas concretos. Nenhum merge ou push solicitado; a implementação permanece na branch criada.
