# PetJourney API

API Java Spring Boot para gestao clinica veterinaria, autenticacao por JWT RSA, cadastro de clinicas, veterinarios, tutores, pets, consultas, prontuarios, medicamentos, primeiro acesso por e-mail, relatorio de consulta em PDF, relatorios consolidados por procedures PostgreSQL e integracao SmartCare com servico Python/FastAPI.

## Proposta e decisões de implementação

O PetJourney organiza a rotina de clínicas veterinárias: cadastro de clientes e pets, agenda, atendimento, prescrições e acesso do tutor ao histórico. O SmartCare prioriza acompanhamento a partir dos fatos clínicos; não é um diagnóstico veterinário.

```mermaid
flowchart LR
    Mobile[Aplicativo React Native] -->|JWT e HTTPS| Java[API Java / Spring Boot]
    Java -->|JPA, JDBC e Flyway| DB[(PostgreSQL)]
    Java -->|Fatos clínicos via HTTP| IA[SmartCare Python / FastAPI]
    Java --> Email[Brevo ou SMTP]
```

- Controllers expõem os contratos; services aplicam regras; repositories acessam os dados.
- JWT identifica perfil e clínica. Cada requisição autenticada verifica se a conta continua ativa, inclusive antes de consultas em cache.
- A consulta completa usa uma transação para prontuário, medicamentos e conclusão do agendamento.
- Exclusão lógica preserva o histórico de clínica, tutor, pet e veterinário.
- Uma clínica possui um único administrador. `ClinicResponse.hasAdmin` permite ao aplicativo refletir essa condição; a API e o índice único do banco também impedem duplicação.
- Relatórios chamam procedures e recebem JSON estruturado. O Python calcula o SmartCare sem acessar diretamente o banco.

## Roteiro de demonstração e evidências

1. Apresentar o problema, a arquitetura e as decisões do grupo.
2. Mostrar administrador do sistema criando clínica e seu administrador; explicar `hasAdmin` e o conflito de duplicação.
3. Cadastrar tutor e pet, ativar acesso e criar disponibilidade/agendamento futuro.
4. Registrar atendimento com medicamento e abrir relatório JSON/PDF e histórico do tutor.
5. Demonstrar relatórios da clínica, isolamento e SmartCare com o serviço Python real.
6. Demonstrar erros controlados e inativação bloqueando a sessão anterior.

Veja [evidências e validação](docs/evidencias/README.md) e o [roteiro Postman](docs/postman/README.md). O vídeo final deve respeitar os 15 minutos da disciplina Java, incluir todos os integrantes e mostrar os fluxos na aplicação publicada. UI/UX e colaboração também fazem parte da avaliação; a suíte do backend não comprova esses itens sozinha.

## Integrantes

| Nome | RM |
|---|---|
| Lucas Gonçalves Viana | 563254 |
| Deryk de Souza Queiroz | 563412 |
| Vinicius Paschoeto da Silva | 563089 |
| Felipe Wiclif Leal da Silva| 563901 |

---

## Links da entrega

| Item | Link |
|---|---|
| Video Youtube | https://www.youtube.com/watch?v=gfT_4wNRK2c |
| Repositório GitHub | https://github.com/LucasViana130/api-java-petjourney.git |
| Repositório Front End | https://github.com/deryksouza2006/PetJourney2.0.git |
| Deploy Railway | https://api-java-petjourney-production.up.railway.app |

## Tecnologias

- Java 17
- Spring Boot 3.4.0
- Spring Web
- Spring `RestClient` para integracao HTTP com o SmartCare
- Spring Security
- OAuth2 Resource Server / JWT RSA
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL
- JdbcTemplate para chamada segura de procedures PostgreSQL
- Spring Mail SMTP / Brevo API
- Spring Cache
- Spring HATEOAS
- Springdoc OpenAPI / Swagger
- OpenPDF
- JUnit 5, Mockito e testes HTTP com MockMvc/MockRestServiceServer
- Docker Compose para PostgreSQL local
- Railway para deploy

## Funcionalidades principais

- Login em `POST /auth/login` e rota compatibilidade `POST /login`.
- JWT assinado com RSA, contendo `role`, `clinicId`, `tutorId` e `veterinarianId`.
- Primeiro acesso para tutor e veterinario com codigo temporario enviado por e-mail.
- Administrador geral `ADMIN_SISTEMA` para gerenciar clinicas e criar administradores de clinica.
- Administrador de clinica `ADMIN_CLINICA` para gerenciar veterinarios, tutores, pets e agenda da clinica.
- Veterinario com acesso ao escopo da propria clinica.
- Tutor com acesso ao proprio perfil, pets, consultas e dados clinicos permitidos.
- Soft delete de Clinica, Veterinario, Tutor e Pet para preservar historico e evitar quebra por FK.
- Relatorio JSON de pets da clinica gerado por `pkg_petjourney.prc_relatorio_pets_json`.
- Relatorio de pesos por especie e sexo gerado por `pkg_petjourney.prc_relatorio_peso_pets`.
- Analise SmartCare individual e ranking da clinica, usando fatos clinicos obtidos pelo backend Java e score calculado exclusivamente pelo servico Python.
- Collection Postman com cenários manuais de CRUD, relatórios, SmartCare e permissões em `docs/postman/PetJourney.postman_collection.json`, configurada por padrão para o ambiente local.

## Usuarios seed

Depois que o Flyway roda, estes usuarios ficam disponiveis:

```text
ADMIN_SISTEMA: admin.sistema@petjourney.com / 123456
ADMIN_CLINICA: admin.petfeliz@petjourney.com / 123456
VETERINARIO: dr.joao@petjourney.com / 123456
TUTOR: carlos@petjourney.com / 123456
```

Endpoint oficial de login:

```http
POST /auth/login
```

Body:

```json
{
  "username": "admin.petfeliz@petjourney.com",
  "password": "123456"
}
```

Use o token retornado nas rotas protegidas:

```http
Authorization: Bearer SEU_TOKEN
```

## Relatorios PostgreSQL da Sprint 4

Os relatorios usam as procedures do schema `pkg_petjourney` e retornam JSON estruturado pela API. O cliente nunca envia `clinicId`: o backend obtem a clinica diretamente do JWT autenticado.

Nesta primeira versao, somente `ADMIN_CLINICA` possui acesso:

```http
GET /reports/pets
GET /reports/pets/weight
GET /reports/pets/weight?species=GATO
```

Resposta de `GET /reports/pets`:

```json
[
  {
    "petId": 1,
    "tutorId": 1,
    "clinicId": 1,
    "pet": "Rex",
    "tutor": "Carlos Souza",
    "clinica": "Clinica Veterinaria Pet Feliz"
  }
]
```

Resposta de `GET /reports/pets/weight`:

```json
[
  {
    "tipo": "DETALHE",
    "species": "CACHORRO",
    "sex": "MACHO",
    "pesoKg": 32.50
  },
  {
    "tipo": "SUBTOTAL",
    "species": "CACHORRO",
    "sex": null,
    "pesoKg": 32.50
  },
  {
    "tipo": "TOTAL",
    "species": null,
    "sex": null,
    "pesoKg": 32.50
  }
]
```

O filtro `species` aceita `CACHORRO`, `GATO`, `AVE`, `ROEDOR`, `REPTIL` e `OUTRO`. Clinica valida sem pets retorna `[]`.

Erros de negocio das procedures usam SQLSTATE `PJxxx`. A API converte esses codigos para respostas HTTP seguras, sem retornar SQL, credenciais ou stack trace.

### Migrations da Sprint 4

- `V8__petjourney_objetos_tecnicos.sql`: cria o schema `pkg_petjourney`, auditoria e log tecnico.
- `V9__petjourney_integridade_e_gatilhos.sql`: adiciona restricoes, indices, coerencia entre clinicas e auditoria de agendamentos.
- `V10__petjourney_rotinas.sql`: cria procedures, funcoes e relatorios.

Em uma base nova, o Flyway executa normalmente V1 ate V10. Antes de atualizar um ambiente existente, confira `flyway_schema_history`: se versoes antigas de V8 a V10 ja tiverem sido aplicadas, nao substitua os arquivos nem execute `flyway repair`; preserve os checksums e use a migration corretiva V11 fornecida no pacote PostgreSQL da Sprint 4.

## SmartCare

A integracao segue o fluxo:

```text
Mobile -> Java -> PostgreSQL (fatos clinicos) -> Python/FastAPI -> Java -> Mobile
```

O Python nao acessa diretamente o PostgreSQL. O Java autentica o usuario, deriva a clinica do JWT, valida o acesso ao pet e envia ao SmartCare apenas os fatos necessarios. O backend Java nao calcula score, nao inventa analise de contingencia e nao grava score no banco.

Endpoints:

```http
GET /smartcare/pets/{petId}/analysis
GET /smartcare/ranking
```

Somente `ADMIN_CLINICA` e `VETERINARIO` podem acessar esses endpoints. `TUTOR` e `ADMIN_SISTEMA` recebem `403`. Um pet de outra clinica nao fica acessivel e o cliente nao informa `clinicId`.

Resposta individual:

```json
{
  "petId": 1,
  "score": 50,
  "prioridade": "MEDIA",
  "motivos": ["Acompanhamento recomendado"],
  "recomendacao": "Manter retorno programado"
}
```

Resposta do ranking:

```json
{
  "analises": [
    {
      "petId": 1,
      "petName": "Rex",
      "score": 50,
      "prioridade": "MEDIA",
      "motivos": ["Acompanhamento recomendado"],
      "recomendacao": "Manter retorno programado"
    }
  ],
  "semDadosSuficientes": [
    {
      "petId": 2,
      "petName": "Mel"
    }
  ]
}
```

O Java chama `POST /smartcare/analisar` no endereco configurado por `SMARTCARE_BASE_URL`. Antes de repassar a resposta, valida o `petId`, o score entre `0` e `100`, as prioridades `BAIXA`, `MEDIA` ou `ALTA`, os motivos e a recomendacao.

- `422 Unprocessable Entity`: o pet nao possui prontuario valido ate a data atual; o Python nao e chamado.
- `503 Service Unavailable`: o Python esta offline, excedeu o timeout, retornou erro HTTP ou enviou resposta invalida.
- O ranking e interrompido integralmente em falha externa; nao ha resposta parcial.

## Execucao local

### Requisitos

- Java 17
- Maven
- Docker Desktop ou PostgreSQL local
- OpenSSL
- Python e o projeto FastAPI do SmartCare, para integracao com a IA real

### 1. Subir PostgreSQL

O `compose.yaml` usa PostgreSQL em `localhost:5433`, banco `petjourney`, usuario `postgres` e senha `postgres`.

```powershell
docker compose up -d
docker compose ps
```

### 2. Gerar chaves JWT RSA

As chaves locais nao ficam no Git por seguranca. Gere antes de iniciar a API:

```powershell
openssl genpkey -algorithm RSA -out src/main/resources/app.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -in src/main/resources/app.key -pubout -out src/main/resources/app.pub
```

Arquivos esperados:

```text
src/main/resources/app.key
src/main/resources/app.pub
```

### 3. Configurar banco local

Se usar o `compose.yaml` sem alterar nada, nenhuma variavel e obrigatoria. O padrao local e:

```properties
DB_URL=jdbc:postgresql://localhost:5433/petjourney
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Para usar outra senha ou outro banco:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/petjourney"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua_senha_local"
```

Para uma instalação nova, a senha inicial do container pode ser definida assim. Em um volume já inicializado, mudar a variável não altera a senha existente do PostgreSQL:

```powershell
$env:POSTGRES_PASSWORD="sua_senha_local"
docker compose up -d
```

### 4. Configurar e-mail local

Por padrao, o envio real fica desligado:

```powershell
$env:MAIL_ENABLED="false"
```

Nesse modo, os e-mails aparecem no log da API, incluindo codigo de primeiro acesso.

Para envio real local via SMTP, use senha de app do Google, nunca a senha normal da conta:

```powershell
$env:MAIL_ENABLED="true"
$env:MAIL_PROVIDER="smtp"
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="seuemail@gmail.com"
$env:MAIL_PASSWORD="sua_senha_de_app_do_google"
$env:MAIL_FROM="PetJourney <seuemail@gmail.com>"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS="true"
$env:MAIL_SMTP_SSL="false"
$env:MAIL_SMTP_CONNECTION_TIMEOUT="5000"
$env:MAIL_SMTP_TIMEOUT="5000"
$env:MAIL_SMTP_WRITE_TIMEOUT="5000"
```

No IntelliJ, coloque as variaveis em `Run -> Edit Configurations -> PetJourneyApplication -> Environment variables`, separadas por ponto e virgula e sem `$env:`.

O envio SMTP roda em background. O cadastro de tutor/veterinario nao fica preso esperando o Gmail; confirme sucesso ou falha do envio no log da API.

Para testar localmente com Brevo API, use:

```powershell
$env:MAIL_ENABLED="true"
$env:MAIL_PROVIDER="brevo"
$env:MAIL_FROM="PetJourney <seuemail@gmail.com>"
$env:BREVO_API_KEY="sua_api_key_da_brevo"
```

### 5. Iniciar o SmartCare Python

Na pasta do servico FastAPI, siga o README do projeto Python e exponha `POST /smartcare/analisar` em `http://localhost:8001`. Se o projeto usar `main.py` como entrada, o comando convencional e:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 127.0.0.1 --port 8001
```

Configure a API Java:

```powershell
$env:SMARTCARE_BASE_URL="http://localhost:8001"
$env:SMARTCARE_CONNECT_TIMEOUT="2s"
$env:SMARTCARE_READ_TIMEOUT="5s"
```

O serviço Python é executado separadamente. Confirme seu módulo de entrada e dependências no projeto SmartCare. Os testes com servidor HTTP simulado validam o protocolo, mas não substituem a execução das regras no Python real.

### 6. Rodar API

```powershell
mvn spring-boot:run
```

Na inicializacao, o Flyway aplica as migrations pendentes ate V10 e o Hibernate valida o esquema com `spring.jpa.hibernate.ddl-auto=validate`.

Ou execute `PetJourneyApplication` pelo IntelliJ.

No IntelliJ, em `Run -> Edit Configurations -> PetJourneyApplication -> Environment variables`, configure no minimo `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_ENABLED=false` e `SMARTCARE_BASE_URL`. Mantenha `RSA_PRIVATE_KEY` e `RSA_PUBLIC_KEY` apontando para as chaves locais ignoradas pelo Git.

URLs locais:

```text
Swagger: http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/api-docs
```

## Deploy no Railway

O repositorio esta preparado para Railway com `railway.toml`.

O Railway executa:

```text
mvn -DskipTests package
java -Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -Xss512k -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dspring.jmx.enabled=false -jar target/petjourney-0.0.1-SNAPSHOT.jar
```

### 1. Criar servicos

1. Crie um projeto no Railway.
2. Adicione um servico PostgreSQL.
3. Adicione um servico para a API usando `Deploy from GitHub repo`.
4. Gere um dominio publico em `Settings -> Networking` no servico da API.

### 2. Variaveis obrigatorias da API

Configure no servico da API, nao no Postgres:

```properties
DB_URL=jdbc:postgresql://HOST_DO_POSTGRES:PORTA_DO_POSTGRES/NOME_DO_BANCO
DB_USERNAME=USUARIO_DO_POSTGRES
DB_PASSWORD=SENHA_DO_POSTGRES
JWT_ISSUER=petjourney-api
JWT_EXPIRATION_MINUTES=60
RSA_PRIVATE_KEY=conteudo_completo_da_chave_privada_pem
RSA_PUBLIC_KEY=conteudo_completo_da_chave_publica_pem
```

Para montar `DB_URL`, copie do servico PostgreSQL do Railway:

```text
DB_URL=jdbc:postgresql://PGHOST:PGPORT/PGDATABASE
DB_USERNAME=PGUSER
DB_PASSWORD=PGPASSWORD
```

Nao use `localhost` no Railway. Dentro do container, `localhost` aponta para a propria API, nao para o banco.

### 3. Chaves RSA no Railway

Copie localmente:

```powershell
Get-Content -Raw src/main/resources/app.key
Get-Content -Raw src/main/resources/app.pub
```

Cole o conteudo completo da chave privada em `RSA_PRIVATE_KEY` e da chave publica em `RSA_PUBLIC_KEY`, incluindo cabecalho e rodape do PEM.

```properties
RSA_PRIVATE_KEY=<conteudo_completo_da_chave_privada_pem>
RSA_PUBLIC_KEY=<conteudo_completo_da_chave_publica_pem>
```

Se o Railway nao aceitar multiplas linhas, substitua as quebras por `\n`. O backend aceita os dois formatos.

### 4. E-mail real no Railway

Em producao, o envio de e-mail usa Brevo pela API HTTP transacional.

```properties
MAIL_ENABLED=true
MAIL_PROVIDER=brevo
MAIL_FROM=PetJourney <seuemail@gmail.com>
BREVO_API_KEY=sua_api_key_da_brevo
BREVO_API_URL=https://api.brevo.com/v3/smtp/email
BREVO_TIMEOUT_MS=10000
```

### SmartCare em produção

Configure um endereço do serviço Python acessível a partir da API Java. `localhost` só funciona se ambos estiverem no mesmo ambiente de rede/processamento.

```properties
SMARTCARE_BASE_URL=https://HOST_REAL_DO_SMARTCARE
SMARTCARE_CONNECT_TIMEOUT=2s
SMARTCARE_READ_TIMEOUT=5s
```

Configuracao na Brevo:

1. Crie uma conta em `brevo.com`.
2. Valide seu proprio e-mail como remetente.
3. Crie uma API key em `SMTP & API`.
4. Configure `BREVO_API_KEY` no Railway.
5. Configure `MAIL_FROM` com o mesmo e-mail remetente validado.

SMTP continua suportado para teste local:

```properties
MAIL_ENABLED=true
MAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=sua_senha_de_app_do_google
MAIL_FROM=PetJourney <seuemail@gmail.com>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_SMTP_SSL=false
MAIL_SMTP_CONNECTION_TIMEOUT=5000
MAIL_SMTP_TIMEOUT=5000
MAIL_SMTP_WRITE_TIMEOUT=5000
```

Se `MAIL_ENABLED=false`, o backend apenas registra o e-mail no log.

### 5. Conferir deploy

O log esperado deve conter:

```text
HikariPool-1 - Start completed
Successfully validated 10 migrations
Schema "public" is up to date
Tomcat started
Started PetJourneyApplication
```

Teste:

```text
https://api-java-petjourney-production.up.railway.app/swagger-ui.html
POST https://api-java-petjourney-production.up.railway.app/auth/login
```

Nao use `:8080` na URL publica. O Railway roteia a porta automaticamente.

Se o dominio publico retornar `502`, confira primeiro os logs do ultimo deploy. A API precisa terminar com `Started PetJourneyApplication`. Se aparecer aviso de falta de memoria ou o processo reiniciar apos subir, mantenha os limites de memoria do `railway.toml` e evite habilitar `JPA_SHOW_SQL=true` em producao.

## Testes pelo Postman

Collection:

```text
docs/postman/PetJourney.postman_collection.json
```

A variável `baseUrl` usa o ambiente local por padrão. Para testar uma publicação, substitua pelo endereço efetivo da API.

Padrão local:

```text
http://localhost:8080
```

Consulte [o roteiro completo](docs/postman/README.md). A collection contém cenários alternativos; não execute todas as pastas em sequência sem selecionar os casos.

Fluxo recomendado:

1. Importe a collection.
2. Confirme a variavel `baseUrl`.
3. Rode `00 - Auth > Login ADMIN_CLINICA`.
4. O teste da request salva o JWT automaticamente em `token` e `adminClinicaToken`.
5. Rode as pastas de consulta e cadastro conforme o perfil.
6. Deixe requests de exclusao para o final do teste, porque elas fazem soft delete dos registros criados.

Para testar recebimento real de e-mail, altere a variavel da collection `testEmailRecipient` para um e-mail seu antes de rodar o login. Com Gmail, a collection gera aliases unicos como `seuemail+tutor.123@gmail.com` e `seuemail+vet.123@gmail.com`; eles chegam na mesma caixa de entrada e evitam conflito de e-mail no banco.

Ao rodar uma nova bateria de testes no Postman, execute novamente `00 - Auth > Login ADMIN_CLINICA`. Essa request gera uma nova rodada de dados dinamicos (`runId`, CPFs e e-mails de teste), reduzindo colisao com registros ja criados no Railway.

Se receber `409 Conflict` em cadastro de tutor:

- `Ja existe tutor cadastrado com este CPF`: use outro CPF de 11 digitos.
- `Ja existe usuario cadastrado com este e-mail`: use outro e-mail.

Mesmo com soft delete, CPF de tutor e username da conta podem continuar reservados no banco para preservar historico e evitar inconsistencias de login.

A collection também inclui pastas para relatórios PostgreSQL, SmartCare online, falhas/permissões e administrador único. As exclusões ficam na pasta 99.

A collection cobre:

- Login, `/auth/me` e primeiro acesso.
- Clinicas e criacao de `ADMIN_CLINICA` pelo `ADMIN_SISTEMA`.
- Tutores, pets, veterinarios e disponibilidades.
- Agendamentos, cancelamento, conclusao e relatorios.
- Prontuarios, medicamentos e workflows completos.

Para testar pelo Swagger no Railway, abra:

```text
https://api-java-petjourney-production.up.railway.app/swagger-ui.html
```

Depois de fazer login em `POST /auth/login`, clique em `Authorize` e informe:

```text
Bearer SEU_TOKEN
```

## Testes automatizados

Execute:

```powershell
mvn test
```

Os testes automatizados cobrem derivacao da clinica autenticada, bloqueio de perfis, binding dos parametros JDBC, `NULL` tipado, conversao dos documentos JSON pelo Jackson, mapeamento dos SQLSTATEs, construcao dos fatos SmartCare, ranking, dados insuficientes, timeout, indisponibilidade e validacao da resposta externa.

Exemplos locais, depois de obter um token de `ADMIN_CLINICA` ou `VETERINARIO`:

```powershell
curl.exe -H "Authorization: Bearer SEU_TOKEN" http://localhost:8080/smartcare/pets/1/analysis
curl.exe -H "Authorization: Bearer SEU_TOKEN" http://localhost:8080/smartcare/ranking
```

Para validar as migrations e procedures de forma integrada, use um PostgreSQL isolado e uma base vazia. A sequencia deve ser V1 ate V8, diagnostico somente leitura, V9 e V10. Nao use a fixture reconstruida do pacote sobre o banco da aplicacao.

## Testar primeiro acesso por e-mail

### Tutor com pet

Com token de `ADMIN_CLINICA` ou `VETERINARIO`:

```http
POST /workflows/tutors/register-with-pet
```

Body:

```json
{
  "tutor": {
    "name": "Tutor Teste Email",
    "cpf": "98765432109",
    "phone": "11999999999",
    "email": "email_destino@exemplo.com"
  },
  "pet": {
    "name": "Bolt",
    "species": "CACHORRO",
    "breed": "SRD",
    "sex": "MACHO",
    "birthDate": "2021-04-10",
    "weight": 14.7,
    "tutorId": 1
  }
}
```

Use CPF e e-mail novos em cada teste.

### Veterinario

Com token de `ADMIN_CLINICA`:

```http
POST /veterinarians
```

Body:

```json
{
  "name": "Vet Teste Email",
  "crmv": "CRMV-TESTE-001",
  "phone": "11988887777",
  "email": "email_destino@exemplo.com",
  "specialty": "Clinico geral",
  "clinicId": 1
}
```

Com `MAIL_ENABLED=true`, o log deve mostrar:

```text
Enviando e-mail PetJourney via Brevo
E-mail PetJourney enviado via Brevo para ...
```

Se aparecer `Falha ao enviar e-mail PetJourney via Brevo`, revise `MAIL_PROVIDER=brevo`, `BREVO_API_KEY` e se o e-mail usado em `MAIL_FROM` foi validado como remetente na Brevo.

Com `MAIL_ENABLED=false`, o log deve mostrar:

```text
E-mail PetJourney em modo local
```

## Variaveis de ambiente

### Banco

```properties
DB_URL=jdbc:postgresql://localhost:5433/petjourney
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

No Railway tambem e possivel usar `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSWORD`, mas `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` sao mais explicitos.

### JWT RSA

```properties
RSA_PRIVATE_KEY=classpath:app.key
RSA_PUBLIC_KEY=classpath:app.pub
JWT_ISSUER=petjourney-api
JWT_EXPIRATION_MINUTES=60
```

### E-mail

```properties
MAIL_ENABLED=false
MAIL_PROVIDER=smtp
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@petjourney.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_SMTP_SSL=false
MAIL_SMTP_CONNECTION_TIMEOUT=5000
MAIL_SMTP_TIMEOUT=5000
MAIL_SMTP_WRITE_TIMEOUT=5000
BREVO_API_KEY=
BREVO_API_URL=https://api.brevo.com/v3/smtp/email
BREVO_TIMEOUT_MS=10000
```

### Performance/runtime

```properties
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false
DB_POOL_MAX_SIZE=3
DB_POOL_MIN_IDLE=1
DB_CONNECTION_TIMEOUT=30000
TOMCAT_MAX_THREADS=25
TOMCAT_MIN_SPARE_THREADS=5
DEVTOOLS_RESTART_ENABLED=false
SPRING_JMX_ENABLED=false
```

## Regras de seguranca e contrato

- Nunca versionar senhas, API keys, `.env`, `app.key` ou `app.pub`.
- Use `.env.example` apenas como referencia; ele nao contem credenciais reais.
- O front/mobile nunca envia e-mail diretamente; ele chama a API e o backend envia pelo provedor configurado (`brevo` ou `smtp`).
- `POST /auth/login` e o endpoint oficial de login.
- `POST /login` continua disponivel por compatibilidade.
- O username/e-mail no login e no primeiro acesso e normalizado com `trim().toLowerCase()`.
- `GET /auth/me` pode ser usado pelo front/mobile para restaurar sessao.
- As listagens `GET /veterinarians`, `GET /tutors` e `GET /pets` retornam pagina Spring; leia `response.content`.
- `PetResponse` retorna `tutorId` e `tutorName`.
- Enums devem ser enviados exatamente como a API espera, por exemplo `CACHORRO`, `GATO`, `MACHO` e `FEMEA`.
- Os endpoints de relatorio retornam arrays JSON, nao strings contendo JSON escapado.
- O cliente nao pode informar uma clinica arbitraria para os relatorios; o escopo vem do token.
- O cliente nao pode informar uma clinica arbitraria para o SmartCare; o escopo tambem vem do token.
- A indisponibilidade do Python retorna `503`, sem calculo alternativo no Java.

## Regras de acesso

- `ADMIN_SISTEMA` tem visao global das clinicas.
- `ADMIN_SISTEMA` gerencia clinicas e cria `ADMIN_CLINICA`.
- `ADMIN_CLINICA` gerencia veterinarios, tutores, pets e agenda da propria clinica.
- `ADMIN_CLINICA` acessa os relatorios PostgreSQL apenas da propria clinica.
- `ADMIN_CLINICA` e `VETERINARIO` acessam o SmartCare apenas para pets ativos da propria clinica.
- `VETERINARIO` atua dentro da propria clinica.
- `TUTOR` acessa apenas seus dados, pets e informacoes clinicas permitidas.
- `ADMIN_SISTEMA`, `VETERINARIO` e `TUTOR` nao acessam os relatorios da Sprint 4 nesta primeira versao.
- `TUTOR` e `ADMIN_SISTEMA` nao acessam os endpoints SmartCare.
- Cancelamento exige mais de 24 horas de antecedência e agendamento não realizado.
- Tutor nao pode excluir agendamento diretamente; deve usar `PATCH /appointments/{id}/cancel`.
- Tutor pode consultar prontuario e medicamentos do proprio pet, mas nao pode criar, editar ou excluir dados clinicos.

## Primeiro acesso

- Tutor nao possui cadastro publico.
- `ADMIN_CLINICA` ou `VETERINARIO` cadastra tutor junto com pet.
- `ADMIN_CLINICA` cadastra veterinario.
- O backend cria a conta como inativa.
- O backend gera codigo temporario de primeiro acesso.
- O codigo e enviado por e-mail ou registrado no log quando `MAIL_ENABLED=false`.
- O usuario ativa a conta em `POST /auth/first-access/activate`.
- Senhas nunca sao enviadas por e-mail.
- `firstAccessCode` ainda aparece na resposta do cadastro tutor + pet para facilitar testes locais e Postman.
