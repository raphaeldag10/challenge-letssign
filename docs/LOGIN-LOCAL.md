# Login local

Instancie um único `UserRepository(AppDatabase.getInstance(context).userDao())` para compartilhar a sessão entre as telas. As funções suspend devem ser chamadas por uma coroutine do ViewModel.

Na primeira configuração, use `setupInitialAdmin(nome, email, telefone, senha)`. A operação é transacional e recusa criar outro administrador quando já existe um. Não há senha padrão.

Use `login(email, senha)` para obter o ID, nome e perfil do usuário. `currentUser` representa a sessão em memória e `logout()` encerra o acesso. Reiniciar o processo exige novo login; os usuários permanecem no Room.

Somente uma sessão ADMIN pode chamar `registerSignatory` e `updateSignatory`. Na edição, omita a senha para preservar a anterior. E-mails são normalizados e duplicados são recusados. As senhas usam PBKDF2-HMAC-SHA256 com salt aleatório e não são retornadas às telas.

Esta contribuição contém a lógica, sem telas. O fluxo de documentos ainda deve verificar se o usuário tem acesso antes de listar ou abrir cada PDF.
