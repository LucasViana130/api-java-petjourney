# PetJourney

## Configuracao local

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
Front/Mobile -> Backend -> Resend -> Usuario
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
