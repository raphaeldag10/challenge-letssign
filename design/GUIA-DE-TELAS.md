# LetsSign — proposta de telas Android

Data: 04/11/2026. Entrega de design para revisão, anterior à implementação. Nenhum código do aplicativo foi alterado.

## Direção visual

Base: imagem institucional fornecida pelo usuário, com marca LetsSign, roxo, índigo, ciano e geometria angular. As cores abaixo são uma aproximação visual da referência, não uma especificação oficial da marca. O logotipo nos mockups é uma representação; na implementação deve ser usado o arquivo oficial da marca, quando disponível.

O acesso concentra o degradê e a geometria da marca. Nas tarefas, superfícies claras e texto escuro priorizam a leitura do PDF. Cabeçalhos compactos identificam a marca; listas com divisores evitam excesso de cartões.

| Uso | Cor proposta |
| --- | --- |
| Ações primárias e seleção | Roxo `#6230B8` |
| Marca e degradê | Índigo `#241475` |
| Destaque secundário | Ciano `#20B7DD` |
| Fundo | `#F7F8FC` |
| Superfícies e papel | Branco `#FFFFFF` |
| Texto principal | `#202135` |
| Texto secundário | `#626879` |
| Divisores | `#E2E4EC` |
| Assinado e concluído | Verde `#16734A` |
| Pendente e parcial | Âmbar `#956000` |
| Erros de validação | Vermelho `#B3261E` |

Tipografia proposta: Roboto; títulos de 24–28 sp, corpo de 16 sp e rótulos de 14 sp. Grade de 8 dp, margens de 20–24 dp, botões de pelo menos 48 dp de altura e cantos de 12 dp em botões e campos. Ciano é decorativo ou complementar, sem uso em textos pequenos sobre branco.

As pranchas são imagens conceituais em alta fidelidade, não um protótipo clicável ou arquivo Figma editável. Os textos e regras deste guia prevalecem sobre eventuais diferenças de renderização dos mockups.

## Navegação

Admin: login → documentos; navegação inferior com Documentos, Signatários e Conta. Novo documento abre uma sequência com quatro etapas: Documento → Pessoas → Campos → Revisão. A navegação inferior fica oculta nesse fluxo para concentrar a tarefa.

Signatário: login → documentos recebidos; abas Pendentes e Assinados, com navegação inferior Documentos e Conta. Não há cadastro de pessoas nem edição dos campos do PDF nesse perfil.

Decisão proposta: o perfil é identificado pelas credenciais autenticadas. Um seletor de perfil não concede acesso administrativo. Se uma conta tiver ambos os perfis, a troca só aparece depois da autenticação e entre os perfis autorizados. Conta contém apenas identificação do usuário, perfil e Sair nesta proposta.

## Inventário de 16 telas

| Tela | Conteúdo e ação principal |
| --- | --- |
| 01. Acesso | Marca, e-mail, senha com mostrar/ocultar e Entrar. |
| 02. Documentos do Admin | Busca, filtros Todos/Enviados/Parciais/Concluídos, progresso por documento e Novo documento. Rascunhos aparecem com rótulo próprio em Todos. |
| 03. Signatários | Busca por nome/e-mail, lista e ações Cadastrar e Editar. |
| 04. Cadastro/edição | Nome completo, e-mail, telefone e senha; Salvar signatário. A edição reutiliza a tela, com o título Editar signatário. |
| 05. Documento | Título, seletor nativo de arquivo, PDF escolhido, total de páginas, miniaturas e Trocar arquivo. |
| 06. Pessoas | Seleção múltipla, função por pessoa, remoção, substituição e acesso ao cadastro de signatário. |
| 07. Campos | PDF ampliável, navegação entre páginas, pessoa selecionada, adicionar, arrastar, redimensionar e excluir campos. |
| 08. Revisão | Acesso ao PDF completo, pessoas, funções, páginas/campos, mensagem do convite e Confirmar envio. |
| 09. Envio registrado | Confirmação persistida, quantidade de solicitações, data/hora e Acompanhar documento. |
| 10. Acompanhamento | Situação geral, progresso, status individual, datas de assinatura e visualização do documento. |
| 11. Histórico/conclusão | Eventos com data/hora; ao concluir, Abrir PDF final e Salvar cópia. |
| 12. Falha de envio | Painel sobre a revisão com dados preservados, Tentar novamente e Voltar à revisão. |
| 13. Recebidos | Pendentes/Assinados e Abrir documento. A aba Assinados representa a participação do usuário, mesmo com outras assinaturas pendentes. |
| 14. Leitura pelo signatário | PDF completo, destaque dos próprios campos e acesso à criação de assinatura. |
| 15. Assinatura | Área de desenho, Limpar, prévia, confirmação e Confirmar e salvar. |
| 16. Assinatura salva | Confirmação da participação, situação geral separada e próximos participantes pendentes. |

## Conteúdo demonstrativo

Documento: Contrato de prestação de serviços; arquivo contrato_servicos.pdf, três páginas, 1,2 MB. Pessoas: Ana Lima, contratante; Bruno Costa, contratado; Carla Souza, testemunha. Dados são fictícios. A função pertence à participação naquele documento e não ao cadastro geral da pessoa.

## Cadastro e edição de pessoas

Todos os quatro campos são obrigatórios na criação. Validar nome, formato do e-mail e telefone antes de salvar; mostrar o erro junto ao campo e mover o foco para o primeiro erro. E-mail duplicado recebe orientação para editar a pessoa existente. Política de senha fica para a implementação e deve ser informada na tela antes de exigir seu cumprimento.

Na edição, a senha atual nunca é exibida nem devolvida à tela; deixar Nova senha vazia mantém a existente. Uma alteração cadastral não modifica retrospectivamente pessoas, funções, eventos e endereços registrados em envios anteriores. Ao cadastrar alguém durante a etapa Pessoas, retornar ao rascunho preservado e disponibilizar a nova pessoa para seleção.

## Documento e pessoas

O título é obrigatório. O seletor nativo permite escolher PDFs do armazenamento disponível no celular. Antes de liberar Continuar, validar formato real e abertura das páginas; extensão .pdf isoladamente não basta.

Deve existir ao menos um signatário, sem duplicar a mesma pessoa na seleção. Cada pessoa recebe uma função: Contratante, Contratado ou Testemunha. Proposta: convites simultâneos, sem ordem obrigatória de assinatura. Remover uma pessoa também remove seus campos, após confirmação com a quantidade afetada; substituir permite reassociar os campos à pessoa substituta, com confirmação. Ao trocar o arquivo PDF, avisar que os campos do arquivo anterior serão descartados.

Sair de um cadastro iniciado mostra Manter rascunho, Descartar alterações e Continuar editando. Voltar entre etapas mantém os dados. Novos rascunhos não geram convites.

## Editor de campos de assinatura

PDF no centro, controles de página e zoom acima, seletor de pessoa e Adicionar campo abaixo. Ao selecionar um campo, mostrar identificação da pessoa, alças de tamanho e Excluir. Cor identifica a pessoa, mas sempre acompanha seu nome. Campos já existentes permanecem visíveis ao selecionar outra pessoa.

O usuário escolhe a página e adiciona um campo, que pode ser movido e redimensionado dentro dos limites do papel. Mais de um campo por pessoa é permitido. A pessoa e sua função acompanham o campo, com índice quando necessário. Uma alternativa acessível a arrastar oferece Mover para cima/baixo/esquerda/direita e ajuste de tamanho. Zoom não altera a posição gravada.

Decisão proposta: exigir ao menos um campo por pessoa antes de avançar. Se faltar, mostrar “Adicione um campo para Bruno Costa” e levar à pessoa correspondente. Não permitir campos fora da página; campos sobrepostos precisam ser reposicionados antes da revisão.

Contrato de posicionamento para a futura implementação: guardar índice da página e retângulo normalizado. x e y são distâncias da esquerda e do topo da página visível, divididas pela largura e altura; largura e altura do campo usam a mesma referência. Todas as medidas usam a área visível do PDF após aplicar corte e rotação, com origem superior esquerda e sem margens da interface. Na gravação do PDF, converter para as coordenadas nativas da página. A escala e o deslocamento de zoom são apenas de exibição. Assim, uma nova tela reproduz o mesmo retângulo relativo no mesmo PDF. Esta é uma especificação, sem implementação nesta entrega.

## Revisão, envio e recuperação

A revisão permite abrir todas as páginas, inspecionar os campos e editar cada etapa. Listar pessoas, funções, quantidade de campos e páginas, agrupando os campos de cada pessoa. A mensagem de convite é editável e opcional. Confirmar envio só é habilitado com PDF válido, título, pessoas e campos completos.

Durante o envio: indicar “Enviando…”, bloquear o botão contra repetição e preservar os dados. A tela de sucesso só aparece depois que o registro do envio é confirmado pelo sistema.

Em timeout ou falha, usar “Envio não confirmado”, porque parte da operação pode ter sido registrada. Tentar novamente deve consultar a mesma operação e reutilizar sua identificação, criando apenas solicitações que faltarem. A interface não pode prometer ausência de duplicação sem esse comportamento futuro do sistema. Se houver resultado parcial, informar quantos convites foram registrados e quantos ainda faltam. Após registro confirmado, o documento deixa de ser editável neste escopo.

## Estados e mensagens adicionais

| Situação | Resposta da interface |
| --- | --- |
| Login inválido | “E-mail ou senha incorretos.”, mantendo o e-mail. |
| Falha de conexão | Mensagem contextual, dados preservados e Tentar novamente. |
| Sem documentos do Admin | “Você ainda não enviou documentos.” e Novo documento. |
| Sem pessoas cadastradas | “Cadastre quem vai assinar seus documentos.” e Cadastrar signatário. |
| Busca vazia | “Nenhum resultado encontrado.” e Limpar busca. |
| Signatário sem pendências | “Tudo em dia. Nenhum documento aguardando sua assinatura.” |
| Arquivo de outro formato | “Selecione um arquivo PDF.” e Escolher outro arquivo. |
| PDF corrompido | “Não foi possível abrir este PDF. Ele pode estar corrompido.” e Escolher outro arquivo. |
| PDF protegido/não processável | “Não foi possível processar este PDF. Escolha uma cópia sem proteção.” sem apagar título/pessoas. |
| Falta de título/pessoa/campo | Erro próximo ao item e direcionamento à etapa a corrigir. |
| Carregamento do PDF | Progresso de leitura; evitar área aparentemente vazia. |
| Falha ao salvar assinatura | “Não foi possível confirmar o salvamento.”; preservar desenho e estado pendente, com nova tentativa da mesma operação. |
| Assinatura já registrada em outro acesso | Recarregar o registro e mostrar a confirmação existente; não aplicar a assinatura novamente. |

Estados de erro reutilizam os mesmos componentes e não exigem uma tela independente por mensagem. A prancha 12 exemplifica a apresentação recuperável sobre a revisão.

## Assinar e salvar

Proposta de interação para este escopo: assinatura desenhada com o dedo. O signatário lê todas as páginas e pode navegar aos próprios campos. Campos de outras pessoas não podem ser editados. Criar assinatura abre a área de desenho, Limpar apaga o traço e a prévia mostra sua aplicação. Desenho vazio impede confirmar. Exigir a confirmação de revisão mostrada no mockup.

Se houver vários campos atribuídos à mesma pessoa, mostrar todos na prévia e aplicar a mesma assinatura a todos em uma única confirmação. Indicar a quantidade de campos e páginas antes de salvar; não exigir um desenho diferente por campo. Assinaturas de outras pessoas são preservadas.

Confirmar e salvar mostra progresso e bloqueia cliques repetidos. Somente após persistir o documento e o registro de participação, atualizar o status e mover o item para Assinados. O usuário não vê confirmação definitiva apenas porque o desenho foi criado localmente. Acesso ao documento assinado permanece dentro do app; o PDF final consolidado só fica disponível quando todos terminarem.

## Estados do registro

| Registro | Estados previstos |
| --- | --- |
| Documento | Rascunho → Enviado → Parcialmente assinado → Concluído |
| Participação individual | Pendente → Assinado |
| Operação de envio | Preparando, enviando, confirmado ou não confirmado; exibidos separadamente da situação do documento |

Com um signatário, o documento vai de Enviado diretamente a Concluído quando a assinatura for salva. Com vários, a primeira assinatura o torna Parcialmente assinado; a última o torna Concluído. A mensagem “Sua participação foi concluída” não significa que o documento inteiro terminou. O requisito de concluir o registro ao salvar é interpretado como concluir a participação individual, mantendo a situação geral coerente com as assinaturas restantes.

O histórico reúne criação, envio, cada assinatura e conclusão, com data/hora e identificação do responsável. Horários devem ser apresentados no fuso do dispositivo; os exemplos de 04/11/2026 são ilustrativos.

## Acessibilidade e adaptação

Respeitar barras do sistema, teclado e aumento de fonte; formulários rolam para manter foco visível. Ação principal fixa acima da área de gesto, sem cobrir conteúdo. Rótulos acessíveis para ícones, nomes nos campos, status por texto e ícone além da cor. Contraste e dimensões precisam ser verificados na interface implementada, já que uma imagem não comprova acessibilidade. No PDF, fornecer lista dos campos por pessoa/página como alternativa à inspeção visual.

## Limites desta entrega

Inclui quatro pranchas, 16 telas conceituais, identidade visual proposta e este guia. Não inclui aplicativo funcional, APIs, autenticação implementada, edição real de PDF, envio de convites ou garantia de validade jurídica. A referência institucional orienta a aparência; seus textos promocionais não foram convertidos em promessas do produto.

Ficam para o planejamento de implementação: persistência e backend, autenticação, processamento do PDF, política de senha e limites de arquivo, mecanismo de convites, trilha de auditoria e eventuais exigências jurídicas. Nenhuma biblioteca, dependência ou arquitetura foi escolhida nesta entrega de design.
