# LetsSign — contexto do desafio e recorte da Sprint 1

Leitura realizada em 04/11/2026: todas as 39 páginas do PDF fornecido pelo usuário, incluindo extração de texto e inspeção visual. Fonte: `C:/Users/raphael.gomes/Downloads/2TDS_RS_Challenge LetsSign_2027.pptx_RevFinal (2).pdf`.

Orientação do usuário: trabalhar somente na Sprint 1 neste primeiro momento. A etapa atual permanece de entendimento/design; esta leitura não inicia a implementação.

## Contexto completo

- Páginas 1–12: apresentação da LetsSign, assinatura eletrônica/digital, automação e gestão de documentos, contexto institucional e assinatura manuscrita Signosoft. As funcionalidades comerciais apresentadas não são automaticamente requisitos do aplicativo acadêmico.
- Página 13: desafio de tornar a preparação, configuração, envio e acompanhamento de documentos simples, seguros e intuitivos no celular.
- Páginas 14–25: objetivo, funcionalidades, requisitos técnicos, entregáveis, agenda e avaliação da Sprint 1.
- Páginas 26–27: formação das equipes e orientação de submissão.
- Páginas 28–34: Sprint 2, backend real e integração futura.
- Páginas 35–39: referências, evento, premiação e encerramento.

## Escopo obrigatório da Sprint 1

| Item | Requisito | Fonte |
| --- | --- | --- |
| Plataforma | Aplicativo móvel nativo; o usuário escolheu Android. | pp. 15, 21 |
| Acesso | Login para Admin e Signatário, com recursos separados por perfil. | p. 16 |
| Signatários | Admin cadastra e edita nome, e-mail, telefone e senha; e-mail/senha são as credenciais de acesso. | p. 16 |
| Documento | Título, seleção de PDF existente no celular e visualização de páginas. Informar arquivo inválido, corrompido ou não processável. | p. 17 |
| Pessoas do documento | Ao menos uma pessoa; múltiplos signatários; função Contratante, Contratado ou Testemunha; remover/substituir antes do envio. | p. 17 |
| Campos | Associados à pessoa, por página, arrastáveis, móveis e excluíveis antes do envio; mais de um campo por pessoa. | pp. 18, 22 |
| Coordenadas | Persistir página, posição e dimensões relativas ao tamanho da página para preservar a posição em outra tela/dispositivo. | p. 18 |
| Revisão | Documento, lista de pessoas, campos/páginas e mensagem do convite antes de confirmar. | p. 19 |
| Envio | Registrar resultado; em falha, orientar e permitir repetir sem duplicar solicitações. | p. 19 |
| Acompanhamento | Situação geral e individual, datas/horários, histórico de criação/envio/assinatura e documento final. | p. 19 |
| Assinatura | Signatário consulta pendentes/assinados, abre o documento, assina e salva no sistema; atualização persistida de status. | pp. 16, 20 |
| Persistência | Banco local, como Room/SQLite no Android; repositórios preparados para futura persistência por APIs REST. | p. 21 |
| Armazenamento do PDF | Arquivo no dispositivo com metadados no banco OU conteúdo binário em BLOB. Não é obrigatório implementar ambos. | p. 21 |
| Preservação | Documento enviado e configurações preservados; qualquer alteração posterior exige nova versão e novo fluxo. | p. 22 |
| Conclusão | Documento concluído somente após todos os signatários obrigatórios finalizarem. | p. 22 |
| Controle de acesso | Um usuário não pode acessar os dados/documentos de outro sem que lhe tenham sido enviados. | p. 22 |

As opções Canvas + PDFBox-Android são exemplos do enunciado, não uma exigência de biblioteca específica (p. 22). Assinatura criptográfica com certificado, CPF, geolocalização e integração SignPad não foram definidos como requisitos obrigatórios da Sprint 1; a apresentação institucional não deve ser usada para ampliar esse recorte.

## Interpretação operacional para a versão local

Exigência do enunciado: persistência local agora; backend real e notificações com deeplink depois. Decisão proposta para demonstrar o fluxo da Sprint 1: criar solicitações de assinatura no banco local e exibi-las ao entrar com a conta destinatária na mesma instalação. A mensagem do convite acompanha a solicitação local.

Isso permite demonstrar Admin → enviar → sair → entrar como Signatário → assinar → sair → entrar como Admin → acompanhar. Não significa sincronização entre aparelhos nem disparo real de e-mail/SMS/push. Essa escolha operacional é uma interpretação do recorte, não uma descrição explícita da demonstração exigida no PDF.

O aplicativo deve persistir registros e PDFs reais; a exigência de banco local não se resume a telas com dados estáticos. Reabrir o app deve manter o estado. A separação de acesso também se aplica às contas armazenadas nessa mesma instalação.

A repetição do envio deve reutilizar a mesma operação local e não criar participantes/solicitações duplicados. Erros principais são validação, leitura/processamento do PDF e gravação local. Não exigir conexão com a internet para completar o fluxo principal da Sprint 1.

A preservação de coordenadas continua obrigatória mesmo sem sincronização nesta etapa: a representação do campo deve usar a página e suas dimensões, independentemente da escala do visualizador. A transferência entre dispositivos será assunto da integração futura.

## Ajustes sobre o design já apresentado

As 16 telas continuam como base visual, com estes ajustes de comportamento/cópia para o recorte local:

1. Enviar registra solicitações locais. Preferir “Solicitações de assinatura registradas” e “Disponível para os signatários neste app”, evitando sugerir entrega por e-mail.
2. E-mail é credencial e identificação da pessoa; seu preenchimento não implica disparo de mensagem nesta etapa.
3. Falhas de gravação/processamento substituem a dependência de conexão no fluxo principal. A prancha de falha de envio ilustra recuperação, mas o ícone de conexão deve ser adaptado para uma falha local quando usado na Sprint 1.
4. O filtro de acesso precisa ser aplicado aos registros e à abertura dos PDFs, além de ocultar itens na interface.
5. Documento enviado fica preservado. Para alterar título, PDF, pessoas ou campos, oferecer uma nova versão com novo fluxo, sem editar o envio original. A ação poderá reutilizar o assistente existente com indicação da nova versão.
6. Ao concluir a própria assinatura, a participação passa a Assinado. Se faltarem outras pessoas, o documento fica Parcialmente assinado; após a última, Concluído. A regra explícita da p. 22 resolve a redação mais geral da p. 20.
7. Os campos devem permanecer dentro do papel e vinculados a participantes. Impedir sobreposição e exigir ao menos um campo por pessoa são escolhas propostas no guia visual, não regras adicionais transcritas do enunciado.

Este documento complementa o `GUIA-DE-TELAS.md` e tem precedência sobre suposições anteriores de integração remota. As pranchas e o pacote `LetsSign-design-v1.zip` registram a proposta visual original; esta leitura não regenerou imagens nem modificou o código do app.

## Fora desta primeira etapa

As páginas 29–30 atribuem à Sprint 2:

- Backend real em Java/Spring ou C#/.NET, conforme opções do enunciado.
- Banco NoSQL.
- APIs REST funcionais e integração real do aplicativo.
- Envio de arquivos por endpoint.
- E-mail e/ou push com deeplink que abre o documento no app.

Na Sprint 1, preparar os repositórios para essa futura troca de persistência não significa construir esses serviços agora.

## Entregáveis da Sprint 1

Conforme p. 23:

- Código-fonte completo em `.zip`.
- APK para o Android escolhido.
- Apresentação em PPT ou PDF com nome do projeto, integrantes e RMs, problemática, solução, tecnologias, fluxo, diferenciais e link do vídeo. Deve seguir a identidade visual da solução.
- Vídeo demonstrativo de até cinco minutos mostrando as principais funcionalidades implementadas.

Nome do projeto, lista de integrantes/RMs e link do vídeo ainda não foram fornecidos nesta conversa. Esses dados serão necessários na preparação da apresentação, não para continuar o design.

## Avaliação e agenda

| Critério da Sprint 1 | Peso |
| --- | --- |
| Adequação ao problema | 20% |
| Implementação técnica funcional | 30% |
| Qualidade do código | 20% |
| Apresentação e documentação | 20% |
| Criatividade e inovação | 10% |

Fonte: p. 25. O foco da entrega deve ser demonstrar o fluxo completo e funcional, com persistência, além da qualidade visual.

A p. 24 informa entrega em **05/11 às 23h50**, valendo 10 pontos. O slide não informa o ano; não inferir o ano somente pelo nome do arquivo. A p. 26 informa equipes da mesma turma, até cinco integrantes, envio por apenas um integrante e ausência dos três dias de tolerância de atraso.

## Situação após esta leitura

Contexto completo lido e recorte da Sprint 1 registrado. As telas existentes são proposta visual, não aplicativo implementado. Nenhum requisito de backend da Sprint 2 será antecipado por causa do contexto institucional ou da apresentação geral do desafio.
