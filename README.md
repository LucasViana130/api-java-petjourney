# PetJourney API

API Java Spring Boot para gestao clinica veterinaria, autenticacao por JWT RSA, cadastro de clinicas, veterinarios, tutores, pets, consultas, prontuarios, medicamentos, primeiro acesso por e-mail e relatorio de consulta em PDF.

## Tecnologias

- Java 17
- Spring Boot 3.4.0
- Spring Web
- Spring Security
- OAuth2 Resource Server / JWT RSA
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL
- Spring Mail SMTP
- Spring Cache
- Spring HATEOAS
- Springdoc OpenAPI / Swagger
- OpenPDF
- H2 para testes
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
- Cancelamento de consulta com regra de 24 horas e notificacao por e-mail.
- Soft delete de Clinica, Veterinario, Tutor e Pet para preservar historico e evitar quebra por FK.
- Collection Postman completa em `docs/postman/PetJourney.postman_collection.json`, configurada por padrao para o Railway.

## Usuarios seed

Depois que o Flyway roda, estes usuarios ficam disponiveis:

```text
ADMIN_SISTEMA: admin.sistema@petjourney.com / 123456
ADMIN_CLINICA: admin.petfeliz@petjourney.com / 123456
VETERINARIO: joao@petjourney.com / 123456
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

## Execucao local

### Requisitos

- Java 17
- Maven
- Docker Desktop ou PostgreSQL local
- OpenSSL

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

Se mudar a senha do container:

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

Para envio real com Gmail SMTP, use senha de app do Google, nunca a senha normal da conta:

```powershell
$env:MAIL_ENABLED="true"
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="seuemail@gmail.com"
$env:MAIL_PASSWORD="sua_senha_de_app_do_google"
$env:MAIL_FROM="PetJourney <seuemail@gmail.com>"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS="true"
```

No IntelliJ, coloque as variaveis em `Run -> Edit Configurations -> PetJourneyApplication -> Environment variables`, separadas por ponto e virgula e sem `$env:`.

### 5. Rodar API

```powershell
mvn spring-boot:run
```

Ou execute `PetJourneyApplication` pelo IntelliJ.

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
java -Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -Xss512k -Dspring.jmx.enabled=false -jar target/petjourney-0.0.1-SNAPSHOT.jar
```

Os arquivos `.java-version` e `system.properties` indicam Java 17 para o ambiente de build/deploy.

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

Para que o e-mail seja enviado de verdade, configure:

```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=sua_senha_de_app_do_google
MAIL_FROM=PetJourney <seuemail@gmail.com>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Se `MAIL_ENABLED=false`, o Railway apenas registra o e-mail no log.

### 5. Conferir deploy

O log esperado deve conter:

```text
HikariPool-1 - Start completed
Successfully validated 7 migrations
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

URL padrao da collection:

```text
https://api-java-petjourney-production.up.railway.app
```

Para testar localmente, altere apenas a variavel `baseUrl` para:

```text
http://localhost:8080
```

Fluxo recomendado:

1. Importe a collection.
2. Confirme a variavel `baseUrl`.
3. Rode `00 - Auth > Login ADMIN_CLINICA`.
4. O teste da request salva o JWT automaticamente em `token` e `adminClinicaToken`.
5. Rode as pastas de consulta e cadastro conforme o perfil.
6. Deixe requests de exclusao para o final do teste, porque elas fazem soft delete dos registros criados.

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
Enviando e-mail PetJourney via SMTP
E-mail PetJourney enviado para ...
```

Com `MAIL_ENABLED=false`, o log deve mostrar:

```text
E-mail PetJourney em modo local
```

## Testes automatizados

```powershell
mvn test
```

Os testes usam H2 e Flyway com perfil `test`, sem exigir PostgreSQL local.

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

### E-mail SMTP

```properties
MAIL_ENABLED=false
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@petjourney.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
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
- O front/mobile nunca envia e-mail diretamente; ele chama a API e o backend envia pelo SMTP configurado.
- `POST /auth/login` e o endpoint oficial de login.
- `POST /login` continua disponivel por compatibilidade.
- O username/e-mail no login e no primeiro acesso e normalizado com `trim().toLowerCase()`.
- `GET /auth/me` pode ser usado pelo front/mobile para restaurar sessao.
- As listagens `GET /veterinarians`, `GET /tutors` e `GET /pets` retornam pagina Spring; leia `response.content`.
- `PetResponse` retorna `tutorId` e `tutorName`.
- Enums devem ser enviados exatamente como a API espera, por exemplo `CACHORRO`, `GATO`, `MACHO` e `FEMEA`.

## Regras de acesso

- `ADMIN_SISTEMA` tem visao global das clinicas.
- `ADMIN_SISTEMA` gerencia clinicas e cria `ADMIN_CLINICA`.
- `ADMIN_CLINICA` gerencia veterinarios, tutores, pets e agenda da propria clinica.
- `VETERINARIO` atua dentro da propria clinica.
- `TUTOR` acessa apenas seus dados, pets e informacoes clinicas permitidas.
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

## Exclusoes

- Clinica, Veterinario, Tutor e Pet usam soft delete com `active=false`.
- Registros desativados deixam de aparecer nas telas operacionais e retornam 404 nas consultas comuns.
- Ao excluir uma clinica, o backend desativa administradores da clinica, veterinarios, tutores, pets e contas vinculadas.
- Ao excluir um tutor, o backend desativa a conta do tutor e seus pets.
- Ao excluir um veterinario, o backend desativa a conta do veterinario.
- O historico clinico fica preservado no banco.

## Observacoes para avaliacao

- Flyway possui 7 migrations versionadas.
- A collection Postman esta dentro do repositorio.
- A API sobe localmente com Docker Compose ou no Railway com PostgreSQL gerenciado.
- O modo local de e-mail por log permite testar sem credenciais SMTP.
- O modo real de e-mail funciona com Gmail SMTP usando senha de app.
- O projeto foi preparado para nao versionar credenciais reais.
