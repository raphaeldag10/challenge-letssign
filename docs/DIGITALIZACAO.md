# Digitalização de documento pela câmera

Na etapa Documento, **Selecionar PDF** e **Trocar arquivo** abrem a escolha de origem:

- **Foto:** abre o scanner pela câmera. Fotografe uma ou mais páginas, confira o recorte, a orientação e os filtros e confirme. O scanner gera um PDF, que é copiado para o armazenamento privado e exibido na prévia do assistente.
- **Arquivo:** abre o seletor de arquivos limitado a PDFs, como no fluxo anterior.

Depois da importação, informe o título e avance para participantes, campos e revisão. Cancelar a escolha ou a captura preserva o rascunho. Um PDF inválido não substitui o documento anterior nem seus campos.

## Tecnologia e disponibilidade

Foi usado o [Google ML Kit Document Scanner](https://developers.google.com/ml-kit/vision/doc-scanner/android), versão 16.0.0, modo FULL, com saída PDF e importação da galeria desativada. O scanner oferece recorte, correção de perspectiva e filtros para dar à foto aparência de documento digitalizado. O resultado contém imagens das páginas; não foi adicionado OCR/texto pesquisável.

A captura depende de Google Play Services e de aparelho com pelo menos 1,7 GB de RAM. O primeiro uso pode precisar de internet para baixar o módulo do scanner. A câmera é gerenciada pelo Google Play Services; o app não solicita uma permissão CAMERA própria. Se a preparação falhar, o app informa o erro e permite tentar novamente ou usar Arquivo.

## Verificação

- 39 testes unitários existentes executados com sucesso.
- 10 testes instrumentados executados com sucesso no emulador Pixel 10 Pro XL, Android API 37: os 5 testes de fluxo existentes e 5 novos testes para escolha de origem, cancelamento, filtro de PDF, cópia de PDF com duas páginas e preservação do rascunho diante de arquivo inválido.
- Os testes de escolha de origem falharam pela ausência do diálogo antes da implementação.
- APK debug gerado; caminho: `app/build/outputs/apk/debug/app-debug.apk`.
- Lint executado sem erros; permanecem avisos anteriores do projeto.
- Revisão independente de código sem problemas concretos identificados.
- A captura e o tratamento visual de uma folha real ainda precisam ser conferidos em celular com câmera e Google Play Services. Os testes automatizados não simulam a captura do SDK.

Também foram corrigidos dois bloqueios preexistentes encontrados na validação: chamadas antigas de `signDocument` nos testes instrumentados e o uso de `InputStream.readNBytes`, incompatível com Android API 28–32, no leitor de PDF.

Comandos de verificação:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug
```

Neste Windows, a JVM apresentou falha na conexão de socket Unix usada pelo Gradle. A execução local utilizou o JDK 17 já instalado e a variável temporária `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\nonexistent-gradle-socket-dir`, forçando o fallback TCP. Nenhuma configuração de Java do repositório foi alterada.
