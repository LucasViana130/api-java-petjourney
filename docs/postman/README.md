# Testes pelo Postman

Importe `PetJourney.postman_collection.json`. O padrão é `http://localhost:8080`. Para ambiente publicado, informe a URL efetiva em `baseUrl`. Não use dados pessoais ou credenciais reais na exportação.

## Preparação

1. Execute os quatro logins da pasta 00. O token principal permanece sendo o do administrador da clínica; os demais ficam em variáveis específicas.
2. Os usuários de exemplo só funcionam se os seeds estiverem presentes e inalterados. Ajuste o body dos logins quando necessário.
3. Confira `clinicId`, `veterinarianId`, `petId` e os IDs SmartCare. Os IDs 1, 2 e 3 são exemplos da base inicial, não uma garantia de qualquer ambiente.
4. Gere registros próprios. Para repetir a criação de clínica, use um CNPJ válido diferente: o exemplo fixo fica reservado mesmo após exclusão lógica.

## Roteiro recomendado

- Pasta 01: criar clínica, consultar `hasAdmin=false`, criar seu administrador e verificar `hasAdmin=true` na pasta 12. A chamada de administrador usa `createdClinicId`, sem modificar a clínica do token principal.
- Pastas 02 e 03: cadastrar tutor/pet e veterinário. E-mail de conta criada não pode ser alterado; preserve o e-mail no PUT. Confira as variáveis salvas antes de continuar.
- Pasta 04: criar uma disponibilidade futura.
- Pasta 05: escolher **Criar Agendamento OU Workflow Agendar Consulta**, pois usam o mesmo horário. Não rodar os dois sem criar outro horário. Atualizar se necessário.
- Pasta 08: registrar consulta completa para o agendamento criado. Isso conclui o atendimento e gera prontuário/medicamentos. Depois consulte os relatórios JSON/PDF da pasta 05.
- Cancelamento e conclusão manual são cenários alternativos: não cancelar depois de concluir. Para testar cancelamento, crie outro agendamento com mais de 24 horas de antecedência.
- Pastas 06 e 07 são CRUDs individuais alternativos ao workflow, não etapas obrigatórias de uma consulta completa.
- No cadastro conjunto da pasta 08, a collection salva `firstAccessUsername` e `firstAccessCode`. Volte a **Ativar Primeiro Acesso** na pasta 00. Em produção, use o código recebido por e-mail.
- Pasta 09: relatórios e bloqueio por perfil. O total é a linha `TOTAL`; não some detalhes e subtotais juntos.
- Pasta 10: executar somente com Python real disponível. Análise deve retornar 200; não considerar 503 como aprovação.
- Pasta 11: permissões e pet sem prontuário podem ser testados sem IA. Os dois cenários de indisponibilidade exigem uma instância de teste desligada.
- Pasta 99: limpeza manual, somente no fim. Confira cada ID; não excluir seeds. A inativação de tutor/veterinário bloqueia também os tokens emitidos anteriormente.

## Limitações do Runner

A collection é um catálogo de cenários com pré-condições, não uma suíte única para executar todas as pastas sem seleção. Existem operações alternativas, alterações de variáveis, relações entre registros e estados mutuamente exclusivos. Use as pastas conforme o roteiro. Vários testes negativos precisam dos logins anteriores.

Para demonstrar isolamento, autentique um segundo administrador de outra clínica, repita os relatórios e confirme que nenhum pet pertence à clínica anterior. Não modifique `clinicId` para tentar escolher o escopo: ele vem do JWT.

Para regressão da inativação: autentique um tutor/veterinário criado para teste, consulte uma rota permitida para preencher cache, inative pelo administrador e repita com o mesmo token. O esperado é 401. Um novo login também deve ser recusado.
