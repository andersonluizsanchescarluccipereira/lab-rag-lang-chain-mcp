# Pix — Consulta de Chaves API | Java 21, MVC, JWT, Swagger e MySQL

**API REST de laboratorio**, porta **8083**. Terceiro projeto da familia **Saldo API (:8081)**, **Extrato API (:8082)** e **Pix Consulta de Chaves (:8083)**.

> **Pix simulado:** este projeto consulta exclusivamente o diretorio local de chaves ficticias gravadas no MySQL. **Nao consulta o DICT do Banco Central**, nao conversa com o SPI, nao registra chaves reais, nao executa pagamentos e nao deve ser usado em producao. Uma consulta real ao DICT requer acesso institucional/autorizado e observancia das regras aplicaveis.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.16, Spring Web MVC |
| Camadas | Controller → Service → Repository → MySQL; model + DTOs |
| Persistencia | Spring Data JPA, Hibernate, MySQL 8.4 |
| Seguranca | Spring Security stateless, JWT HS256 / JJWT 0.13.0, BCrypt |
| Documentacao | Springdoc OpenAPI 2.10.0 + Swagger UI |
| Testes | JUnit 5 + Mockito |
| Porta da API | **8083** |
| MySQL demo no host | **127.0.0.1:3309** (internamente 3306) |
| MySQL Saldo API no host | **127.0.0.1:3307** |

## 1. Subir em modo demo (mais simples)

Requisitos: Docker Compose V2 **ou** Podman + `podman-compose`, `openssl`, internet na primeira compilacao/obtenção de imagens.

```bash
unzip pix-chave-java21-mvc-jwt.zip
cd pix-chave-java21-mvc-jwt
./scripts/up.sh   # alias de quickstart-demo.sh
```

Este script:

1. Cria **`.env`** automaticamente com duas chaves aleatorias persistentes de **32 bytes**: uma para JWT e outra para HMAC das chaves Pix; preserva o arquivo se ele ja existir.
2. Prioriza **`podman-compose -f compose.yaml up --build -d`** se estiver instalado; senao usa Docker Compose.
3. Sobe o MySQL 8.4 e o servico Java na porta **8083**, com o perfil `demo` e tres chaves ficticias.

**Swagger:** http://localhost:8083/swagger-ui/index.html (ou http://localhost:8083/swagger-ui.html)  
**OpenAPI JSON:** http://localhost:8083/v3/api-docs

**Usuario demo:** `demo@arqvora.local` / `Demo@123456`  
**Usuario destinatario ficticio:** `loja@arqvora.local` / `Loja@123456`

As tres chaves cadastradas automaticamente sao:

- `EMAIL` → `demo@arqvora.local` (usuario demo)
- `EVP` → `11111111-2222-4333-8444-555555555555` (usuario demo)
- `EMAIL` → `loja@example.com` (destinatario Loja Exemplo)

### Comandos Podman (Linux Mint XFCE)

```bash
# Evitar 'podman compose' quando ele delega ao docker-compose legado.
podman-compose -f compose.yaml up --build -d
podman ps
podman logs -f pix-chave-java21-mvc-jwt_pix-api_1  # nome pode variar

# Melhor: use podman-compose -f compose.yaml logs -f pix-api
podman-compose -f compose.yaml logs -f pix-api

# Encerrar SEM apagar o banco
podman-compose -f compose.yaml down
```

**Nao execute `down -v` se quiser preservar o MySQL.** O arquivo Compose tem `version: '2.4'`, sem campo raiz `name`, e usa a imagem `docker.io/library/mysql:8.4` para evitar busca ambigua de imagem no Podman.

### Testar pelo terminal

```bash
# 1) Login — recebe accessToken
curl -sS -X POST 'http://localhost:8083/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@arqvora.local","password":"Demo@123456"}'

# 2) Colar o accessToken na variavel (nao inclua a palavra Bearer aqui)
export TOKEN='COLE_O_ACCESS_TOKEN_AQUI'

# 3) Minhas chaves (apenas as do dono autenticado)
curl -sS -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8083/api/v1/pix/keys/me?page=0&size=20'

# 4) Consultar chave do destinatario ficticio (diretorio LOCAL)
curl -sS -X POST 'http://localhost:8083/api/v1/pix/keys/lookup' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"type":"EMAIL","key":"loja@example.com"}'

# 5) Registrar uma chave ficticia propria (nao registra no DICT)
curl -i -X POST 'http://localhost:8083/api/v1/pix/keys/me' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"type":"EMAIL","key":"contato-demo@example.com"}'
```

Ou rode o smoke test com `curl` e `python3` instalados:

```bash
./scripts/smoke-test-demo.sh
```

### Swagger: autenticar corretamente

No Swagger (`/swagger-ui/index.html`), chame `POST /api/auth/login`, copie **apenas** `accessToken`, clique em **Authorize**, cole o token e feche. A interface anexa `Bearer` automaticamente. Se receber **401**, confira expiracao, chave JWT, banco/usuario e o cabecalho `Authorization: Bearer <token>`.

## 2. Endpoints

| Metodo | URI | JWT | Objetivo |
|---|---|---|---|
| POST | `/api/auth/login` | Nao | Obter JWT para usuario existente |
| GET | `/api/v1/pix/keys/me` | Sim | Listar chaves proprias, `page` (>= 0), `size` (1 a 50) |
| GET | `/api/v1/pix/keys/me/{keyId}` | Sim | Consultar chave **propria** por UUID |
| POST | `/api/v1/pix/keys/lookup` | Sim | Resolver chave ficticia e obter destinatario **mascarado** |
| POST | `/api/v1/pix/keys/me` | Sim | Registrar chave na **base ficticia** da propria conta |
| DELETE | `/api/v1/pix/keys/me/{keyId}` | Sim | Excluir chave da base ficticia (204) |

Tipos admitidos: `CPF`, `CNPJ`, `EMAIL`, `PHONE` (formato internacional E.164, como `+55...`) e `EVP` (UUID). CPF e CNPJ exigem digitos verificadores validos. E-mails sao normalizados em minusculas; EVPs usam UUID canonico.

**Exemplo de resposta (dados totalmente ficticios):**

```json
{
  "type": "EMAIL",
  "maskedKey": "lo***@example.com",
  "recipientMaskedName": "L*** E***",
  "institution": "ARQVORA Bank Lab",
  "accountMasked": "****5f0e"
}
```

Os caracteres finais em `accountMasked` sao apenas um identificador de laboratorio; nao representam agencia ou numero de conta bancarios reais.

**HTTP:** `200` consulta; `201` registro; `204` exclusao; `400` validacao; `401` JWT ausente, incorreto ou expirado; `404` chave inexistente/de outro usuario; `409` chave ja existente; `422` limite de chaves; `429` consultas excessivas.

**Privacidade e abuso:** a base nao armazena o valor original da chave Pix em texto claro: guarda somente `HMAC-SHA256(tipo + ":" + valor_normalizado)` e uma mascara. O segredo HMAC **precisa permanecer estavel** entre reinicializacoes; se trocado, as chaves previamente cadastradas nao poderao mais ser encontradas. E necessario guardar o segredo com seguranca. Consultas de terceiros revelam apenas mascaras, e sao limitadas a 20/minuto/usuario/**instancia** por um controle em memoria, nao distribuido. Isso e uma defesa didatica, nao um controle robusto de producao.

## 3. Integracao com Saldo :8081 e Extrato :8082

**Objetivo:** reaproveitar `app_users`, `accounts` e o JWT existente. A API Pix adiciona **somente `pix_keys`** ao `saldo_db`. Nao le nem altera saldo, `account_transactions` ou o extrato.

> A URL privada do chat anterior nao pode ser lida integralmente sem autenticacao. As decisoes de compatibilidade foram tomadas a partir dos projetos Saldo/Extrato ja construidos: `app_users`, `accounts`, identificador UUID em `sub`, chave JWT compartilhada e portas 8081/8082.

Passos:

1. Suba e inicialize **primeiro a Saldo API (:8081)** no banco `saldo_db` exposto em **127.0.0.1:3307**. Isso cria `app_users` e `accounts`. A API de Extrato pode continuar na porta **8082**.
2. Execute **uma unica vez** o SQL `db/pix-keys-mysql.sql` nesse mesmo banco (este script nao toca tabelas existentes):

   ```bash
   # Exemplo se houver mysql client no host:
   mysql -h 127.0.0.1 -P 3307 -u saldo -psaldo_dev_only saldo_db \
     < db/pix-keys-mysql.sql

   # Alternativa sem client no host: no diretorio da Saldo API, container MySQL em execucao:
   # podman exec -i CONTAINER_MYSQL_SALDO mysql -u saldo -psaldo_dev_only saldo_db \
   #   < /CAMINHO/DO/PROJETO/PIX/db/pix-keys-mysql.sql
   ```

3. Execute a **API Pix pelo Maven no host**, sem subir `compose.yaml` do modo demo. Configure a mesma chave de JWT que usa em Saldo e Extrato e uma chave HMAC propria para Pix:

   ```bash
   cd pix-chave-java21-mvc-jwt
   export JWT_SECRET_BASE64='MESMA_CHAVE_BASE64_DA_SALDO_E_EXTRATO'
   export PIX_LOOKUP_SECRET_BASE64='OUTRA_CHAVE_BASE64_DE_32_BYTES_PERSISTENTE'
   export DB_URL='jdbc:mysql://localhost:3307/saldo_db?serverTimezone=UTC'
   export DB_USER='saldo'
   export DB_PASSWORD='saldo_dev_only'
   unset SPRING_PROFILES_ACTIVE
   mvn clean test
   mvn spring-boot:run
   ```

**Atenção ao `validate`:** no perfil padrao o Hibernate usa `ddl-auto: validate`: a API nao cria, remove ou altera estruturas. Por isso o SQL de `pix_keys` deve existir antes de iniciar a API. No demo isolado, `ddl-auto: update` e permitido **somente** no banco `pix_db`.

**Autenticacao:** logue na API de Saldo (`POST http://localhost:8081/api/auth/login`) e utilize o `accessToken` retornado nas chamadas Pix (`http://localhost:8083/api/v1/pix/keys/me`). O token funciona porque ambas verificam HS256 com **a mesma chave**, o claim `sub` guarda o **UUID do usuario** e o usuario existe na tabela compartilhada `app_users`. Para validar o banco da API Pix, ela le `accounts.owner_id` do usuario autenticado, nao um `ownerId` enviado pelo cliente.

**Por que nao usar um Compose unico automaticamente?** O Compose desta entrega se destina ao banco **isolado** para evitar sobrescrever o volume/porta da Saldo API. Para producao, nao recomendamos banco de dados compartilhado entre microsservicos nem segredos JWT simetricos compartilhados; aqui ambos simplificam a integracao didatica.

### Visao geral

```text
 Cliente / Swagger / curl
          |
  JWT (sub=UUID do usuario)
          |
    Pix API :8083
 JwtAuthenticationFilter
          |
     PixKeyController
          |
      PixKeyService -------- LookupRateLimiter (20/min por usuario)
          |                   |
   PixKeyNormalizer      PixKeyHasher (HMAC-SHA256)
          |                   |
       PixKeyRepository + AccountRepository + UserRepository
          |
          v
    MySQL (standalone :3309, pix_db)
      app_users / accounts / pix_keys

    ou (modo integrado)

    MySQL Saldo API :3307, saldo_db
      app_users / accounts / account_transactions / pix_keys
           ^                ^                ^
           |                |                |
       Saldo :8081       Extrato :8082    Pix :8083
```

### Pastas MVC

```text
src/main/java/com/arqvora/pix/
├── PixApplication.java
├── config/         # Spring Security + OpenAPI
├── controller/     # AuthController + PixKeyController
├── dto/            # Contratos HTTP, sem expor entidades JPA
├── model/          # AppUser, Account, PixKey, PixKeyType
├── repository/     # Spring Data JPA
├── security/       # JwtService, JwtAuthenticationFilter, PixKeyHasher
├── service/        # AuthService, PixKeyService, normalizacao, rate limit
├── error/          # Exceptions + ProblemDetail
└── demo/           # Seed ficticio somente no perfil demo
src/test/java/com/arqvora/pix/
├── security/       # JWT e HMAC
└── service/        # Validacao, rate limiting, isolamento de usuario, lookup
```

### Decisoes de arquitetura e limites

- **Seguranca:** JWT stateless + consulta de conta pelo `sub`; `findByIdAndAccount_OwnerId` evita IDOR nas proprias chaves. O lookup ficticio exige autenticação, tem limites e devolve mascaras.
- **KISS:** MySQL com indice unico em `key_hash`, sem Kafka/Redis desnecessarios em consulta didatica.
- **Concorrencia:** a unicidade da chave e garantida no banco por `UNIQUE(key_hash)`, alem de validacao de negocio (409). Nao confie somente em `existsBy...` sob concorrencia.
- **Resiliencia:** no laboratorio, o MySQL e um ponto unico de falha; para producao avaliar replicacao, pool tuning, circuit breaker nos acessos externos e observabilidade.
- **Evolucao realista:** integracao DICT por adaptador autorizado, infraestrutura de mensageria para eventos de registro/portabilidade de chaves, limites distribuidos, auditoria com minimizacao de PII, consentimento/base legal LGPD, mensuracao p95/p99 e rejeicoes, e revisao de normas vigentes do Bacen.
- **Limite demonstrativo:** cadastro de cinco chaves por conta (regra propria do laboratorio, nao uma implementacao completa de limites regulatorios); sem comprovacao de titularidade ou verificacao de posse de telefone/e-mail.

## 4. Problemas comuns ja prevenidos

| Sintoma | Diagnostico e correcao |
|---|---|
| `name does not match regex ^x-` | Arquivo Compose usa `version: '2.4'` e nao possui raiz `name:`; nao usar `docker-compose` legado no lugar de `podman-compose`. |
| `short-name mysql could not be resolved` | Imagem explicita `docker.io/library/mysql:8.4`. |
| `container not found` | Quase sempre e consequencia do erro anterior ao subir a imagem; consulte `podman-compose -f compose.yaml ps` e logs. |
| `401 Unauthorized` | JWT expirado, Base64 divergente, `Bearer ` ausente, ou usuario nao existe no banco corrente. |
| `Schema-validation: missing table pix_keys` | Modo integrado: execute `db/pix-keys-mysql.sql` no `saldo_db` apos criar schema pela Saldo API. |
| `Communications link failure` | Confira MySQL, `3309` demo vs `3307` integrado; dentro do Compose use `mysql:3306` em vez de `localhost`. |
| Busca nao encontra chave apos reinicio | Chave `PIX_LOOKUP_SECRET_BASE64` mudou. Preserve `.env`; HMAC nao e reversivel. |
| Swagger retorna 403/404 | Use `http://localhost:8083/swagger-ui/index.html`; confirme `SPRING_PROFILES_ACTIVE` e logs de inicializacao. |
| Porta 3309 ou 8083 ocupada | `ss -ltnp | grep -E ':3309|:8083'`; nao remapeie :8083 se quiser manter o contrato deste lab. |

## 5. Testes

```bash
mvn clean test
mvn package
```

Testes unitarios verificam: validade de CPF/CNPJ, normalizacao de EMAIL/PHONE/EVP, HMAC, JWT, limites de consulta, paginacao, isolamento das chaves e resposta mascarada. O script `smoke-test-demo.sh` testa por HTTP a instancia demo em execucao.

**Importante:** `mvn test` exige Java 21 + Maven 3.9+, e um ambiente com acesso ao Maven Central pelo menos no primeiro build. Sem essas dependencias externas, nao e possivel compilar e executar a suite apenas pela inspeção de arquivos.

**Uso exclusivo para estudos.** Nunca use as senhas demo, o Compose e a configuracao default em ambiente produtivo.
