# PetJourney

## Como executar localmente

### Requisitos

- Java 17
- Maven
- Docker Desktop ou PostgreSQL local
- OpenSSL para gerar as chaves RSA

### 1. Subir o PostgreSQL

O projeto usa PostgreSQL em `localhost:5433`, banco `petjourney`, usuario `postgres` e senha `postgres` por padrao.

```powershell
docker compose up -d
```

Se quiser conferir se o container esta rodando:

```powershell
docker compose ps
```

### 2. Gerar as chaves JWT RSA

As chaves nao ficam no Git por seguranca. Gere antes de iniciar a API:

```powershell
openssl genpkey -algorithm RSA -out src/main/resources/app.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -in src/main/resources/app.key -pubout -out src/main/resources/app.pub
```

Os arquivos esperados sao:

```text
src/main/resources/app.key
src/main/resources/app.pub
```

### 3. Configurar o banco

Se estiver usando o `compose.yaml` sem alterar nada, nao precisa configurar variaveis. A API ja usa:

```properties
DB_URL=jdbc:postgresql://localhost:5433/petjourney
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Para usar outra senha ou URL, configure antes de iniciar:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/petjourney"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua_senha_local"
```

Se mudar a senha do container, use tambem:

```powershell
$env:POSTGRES_PASSWORD="sua_senha_local"
docker compose up -d
```

### 4. Configurar e-mail

Para entrega local sem envio real, mantenha:

```powershell
$env:MAIL_ENABLED="false"
```

Nesse modo, o codigo de primeiro acesso aparece no log da API.

Para envio real com Gmail, use uma senha de app do Google. Nao use a senha normal da conta:

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

No IntelliJ, coloque essas variaveis em `Run -> Edit Configurations -> PetJourneyApplication -> Environment variables`, sem `$env:` e separadas por ponto e virgula:

```text
MAIL_ENABLED=true;MAIL_HOST=smtp.gmail.com;MAIL_PORT=587;MAIL_USERNAME=seuemail@gmail.com;MAIL_PASSWORD=sua_senha_de_app_do_google;MAIL_FROM=PetJourney <seuemail@gmail.com>;MAIL_SMTP_AUTH=true;MAIL_SMTP_STARTTLS=true
```

### 5. Rodar a API

Pelo terminal:

```powershell
mvn spring-boot:run
```

Ou pelo IntelliJ, execute a classe `PetJourneyApplication`.

Quando subir corretamente, o log deve mostrar:

```text
Tomcat started on port 8080
Started PetJourneyApplication
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Collection Postman:

```text
docs/postman/PetJourney.postman_collection.json
```

Importe essa collection no Postman e rode primeiro `Auth > Login ADMIN_CLINICA`. O teste da request salva o JWT automaticamente na variavel `token` da collection.

### 6. Login inicial

Usuarios seed para teste:

```text
ADMIN_SISTEMA: admin.sistema@petjourney.com / 123456
ADMIN_CLINICA: admin.petfeliz@petjourney.com / 123456
VETERINARIO: joao@petjourney.com / 123456
TUTOR: carlos@petjourney.com / 123456
```

Endpoint oficial de login:

```http
POST http://localhost:8080/auth/login
```

Body:

```json
{
  "username": "admin.petfeliz@petjourney.com",
  "password": "123456"
}
```

Use o token retornado no header:

```http
Authorization: Bearer SEU_TOKEN
```

### 7. Testar primeiro acesso por e-mail

Com token de `ADMIN_CLINICA`, chame:

```http
POST http://localhost:8080/workflows/tutors/register-with-pet
```

Body de exemplo:

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

Use um CPF novo em cada teste. Se `MAIL_ENABLED=false`, o codigo aparece no log. Se `MAIL_ENABLED=true`, o backend envia pelo SMTP configurado.

### 8. Rodar testes

```powershell
mvn test
```

## Configuracao local detalhada

Por padrao, o projeto espera PostgreSQL em `localhost:5433` com banco `petjourney`, usuario `postgres` e senha `postgres`.

Para usar outra senha ou URL, configure variaveis de ambiente antes de iniciar:

```properties
DB_URL=jdbc:postgresql://localhost:5433/petjourney
DB_USERNAME=postgres
DB_PASSWORD=sua_senha_local
POSTGRES_PASSWORD=sua_senha_local
```

Nao coloque senhas reais em `application.properties`, `compose.yaml`, `.env` versionado ou arquivos da IDE.

## Chaves JWT RSA

O projeto usa JWT com RSA. As chaves locais de desenvolvimento ficam fora do Git:

```text
src/main/resources/app.key
src/main/resources/app.pub
```

Gere as chaves localmente antes de subir a aplicacao:

```bash
openssl genpkey -algorithm RSA -out src/main/resources/app.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -in src/main/resources/app.key -pubout -out src/main/resources/app.pub
```

Tambem e possivel apontar outros arquivos por variaveis:

```properties
RSA_PRIVATE_KEY=classpath:app.key
RSA_PUBLIC_KEY=classpath:app.pub
```

## E-mail

O envio de e-mail e feito pelo backend. O front/mobile apenas chama a API; o fluxo correto e:

```text
Front/Mobile -> Backend -> SMTP configurado -> Usuario
```

Por padrao, o ambiente local nao envia e-mail real. Com `MAIL_ENABLED=false`, o backend registra no log o conteudo que seria enviado, incluindo o codigo de primeiro acesso do tutor. Esse modo nao exige host SMTP, usuario, senha ou API key.

### Variaveis de ambiente

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

### Gmail via SMTP

Para teste gratuito com Gmail, use uma senha de app do Google. Nao use a senha normal da conta.

```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=sua_senha_de_app
MAIL_FROM=PetJourney <seuemail@gmail.com>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

O Gmail pode reescrever o remetente para a conta autenticada. Para entrega de faculdade/teste, isso costuma ser suficiente.

### Resend via SMTP

Para envio real com Resend:

```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.resend.com
MAIL_PORT=587
MAIL_USERNAME=resend
MAIL_PASSWORD=<API_KEY_DA_RESEND>
MAIL_FROM=PetJourney <no-reply@seudominio.com>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Nunca coloque credenciais SMTP ou API keys no codigo, no `application.properties` ou em arquivos versionados. Use variaveis de ambiente no ambiente de execucao.

### Fluxos com e-mail

Primeiro acesso do tutor:

- `ADMIN_CLINICA` ou `VETERINARIO` cadastra tutor junto com pet.
- O backend cria a conta do tutor como inativa.
- O backend gera um codigo temporario de primeiro acesso.
- O backend envia o codigo para o e-mail do tutor ou registra no log quando `MAIL_ENABLED=false`.
- O tutor usa "Primeiro acesso" no app para informar e-mail, codigo e criar a propria senha.
- Senhas nunca sao enviadas por e-mail.

Cancelamento de consulta:

- `PATCH /appointments/{id}/cancel` mantem a regra de 24 horas.
- Se quem cancelou foi `TUTOR`, o backend notifica o veterinario; se o veterinario nao tiver e-mail, notifica a clinica.
- Se quem cancelou foi `VETERINARIO` ou `ADMIN_CLINICA`, o backend notifica o tutor.
- Se o destinatario estiver vazio, o backend registra warning e nao interrompe a operacao.

## Contrato para Mobile

- Existe um administrador geral com role `ADMIN_SISTEMA`: `admin.sistema@petjourney.com` / `123456` nos dados seed.
- `ADMIN_SISTEMA` tem a visao global de clinicas com `GET /clinics`.
- `ADMIN_SISTEMA` gerencia clinicas com `POST /clinics`, `PUT /clinics/{id}` e `DELETE /clinics/{id}`.
- `ADMIN_SISTEMA` cria administradores de clinica com `POST /system/clinics/{clinicId}/admins`.
- `GET /clinics/{id}` pode ser usado por perfis autenticados, mas o backend restringe o acesso ao escopo permitido; ADMIN_CLINICA, VETERINARIO e TUTOR nao possuem listagem global.
- Use `POST /auth/login` como endpoint oficial de login.
- O endpoint `POST /login` continua disponivel por compatibilidade.
- O e-mail/username enviado no login e no primeiro acesso e normalizado com `trim().toLowerCase()`, entao maiusculas, minusculas e espacos acidentais nao alteram o usuario encontrado.
- Envie `Authorization: Bearer TOKEN` nas rotas protegidas.
- Use `GET /auth/me` para restaurar/validar sessao.
- As listagens `GET /veterinarians`, `GET /tutors` e `GET /pets` retornam pagina Spring; no Mobile, leia os dados em `response.content`.
- `PetResponse` retorna `tutorId` e `tutorName`; use `tutorId` ao editar pet com `PUT /pets/{id}`.
- No workflow `POST /workflows/tutors/register-with-pet`, o `pet.tutorId` enviado pelo cliente e ignorado; o backend sempre usa o tutor criado na mesma requisicao.
- Os enums enviados pelo Mobile devem manter os valores da API, por exemplo `CACHORRO`, `GATO`, `MACHO` e `FEMEA`.
- `ADMIN_CLINICA` pode excluir Tutor/Pet/Veterinario conforme regras da API; `VETERINARIO` nao deve exibir botoes de exclusao.
- As exclusoes de Clinica, Veterinario, Tutor e Pet usam soft delete: o registro fica preservado no banco com `active=false`, deixa de aparecer nas telas operacionais e nao quebra historico clinico.
- Ao excluir uma clinica, o backend tambem desativa seus administradores de clinica, veterinarios, tutores, pets e respectivas contas de acesso.
- Tutor nao pode excluir agendamento diretamente; deve usar `PATCH /appointments/{id}/cancel`, que aplica as regras de negocio.
- Tutor pode consultar prontuario e medicamento do proprio pet, mas nao pode criar, editar nem excluir dados clinicos.
- `TutorRequest` nao recebe senha; a senha do tutor sempre e criada em `POST /auth/first-access/activate`.
- Tutor nao pode alterar o proprio CPF pelo `PUT /tutors/{id}`.
- O e-mail do tutor fica imutavel quando existe conta de acesso associada, pois ele e usado como username de login.
- Ao excluir um tutor, o backend desativa a conta de acesso associada e seus pets, preservando consultas, prontuarios e medicamentos historicos.
- Ao cadastrar veterinario com e-mail, o backend cria uma conta `VETERINARIO` inativa, gera codigo de primeiro acesso e envia/loga o codigo.
- O e-mail do veterinario fica imutavel quando existe conta de acesso associada, pois ele e usado como username de login.
- Ao excluir um veterinario, o backend desativa a conta de acesso associada, preservando consultas, prontuarios e medicamentos historicos.
- Ao cadastrar tutor com pet, o backend cria uma conta `TUTOR` inativa, gera codigo de primeiro acesso e envia/loga o codigo.
- O campo `firstAccessCode` ainda aparece na resposta do cadastro tutor + pet para facilitar testes locais.
