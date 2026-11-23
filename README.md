# CONEXA — Backend e interface web (Etapa 3: Fundação + Sprints 4.1 a 4.3 e Etapas 5 e 6: Áreas, Tarefas, Eventos, Conexões e Radar)

Aplicação monolítica Java 17 + Spring Boot 3, com Spring MVC + Thymeleaf, Spring Security (BCrypt),
Spring Data JPA, Flyway e PostgreSQL, organizada em componentes com porta e núcleo (Hexagonal).

## O que esta etapa entrega

| História | Entrega |
|---|---|
| US01 Criar conta | Cadastro com validação, e-mail único, senha com hash BCrypt, login automático |
| US02 Entrar e sair | Login, logout, mensagem genérica de erro, páginas internas protegidas |
| US03 Minha Vida | Criada no cadastro por evento interno (Observer), na mesma transação |
| US04/US05 Áreas (Sprint 4.1) | Criar, renomear e excluir Áreas (com confirmação); regra de acesso "dono" via `PoliticaDeAcesso`; migração `V2__areas.sql`. Compilado e testado. |
| US06/US07/US08 Tarefas (Sprint 4.2) | Criar (prazo obrigatório, responsável = criador), editar, concluir, excluir, destaque de atrasadas na Área e no Dashboard; exclusão da Área remove as tarefas por evento; migração `V3__tarefas.sql`. Compilado e testado (`BUILD SUCCESS`, 15/11/2026). |
| US09/US10/US11 Eventos (Sprint 4.3) | Criar, editar e excluir (Área obrigatória, duração em minutos), calendário mensal, conflito de horário entre Áreas (eventos que só se tocam não conflitam), cascata na exclusão da Área; migração `V4__eventos.sql`. Compilado e testado. |
| US12/US13/US14 Conexões (Etapa 5) | Conectar por e-mail, níveis Visualizador e Colaborador (Strategy em `PoliticaDeAcesso`), "Compartilhadas comigo", responsável por tarefa (dono ou Colaborador), troca de nível e revogação valendo na próxima requisição, tarefas devolvidas ao dono, cascata com a Área; migração `V5__conexoes.sql`. Compilado e testado, CI verde. |
| US15/US16/US17 Radar, Dashboard e alertas (Etapa 6) | Índice de Carga pela regra de pontos do escopo (fator de escala 4 em `conexa.radar.fator-de-escala`), tela Radar com composição do índice, atrasadas e conflitos, alertas de atraso e de menos de 24 h calculados na consulta, Dashboard completo com estado vazio. **Código ainda não compilado: rode `mvn clean verify`.** |
| Base de interface | Layout Thymeleaf portado do protótipo; Dashboard vazio; Configurações (editar nome, sair) |
| Infraestrutura | Migrações Flyway, Docker, Docker Compose, CI, Actuator, teste de arquitetura |

Todas as funcionalidades do MVP estão implementadas; a navegação mostra
apenas o que já funciona.

## Executar

**Sem Docker (banco H2 em memória, com as mesmas migrações):**

Linux, macOS e Git Bash:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Windows (PowerShell). O PowerShell quebra o argumento no ponto, então ele precisa de aspas:
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Alternativa, pela variável de ambiente:
```powershell
# PowerShell
$env:SPRING_PROFILES_ACTIVE = "local"; mvn spring-boot:run
```
```bat
:: cmd
set SPRING_PROFILES_ACTIVE=local && mvn spring-boot:run
```

Depois, abra http://localhost:8080. Sem o perfil `local`, a aplicação tenta conectar a um
PostgreSQL em `localhost:5432` e para com erro de conexão se ele não existir.

**Com Docker Compose (PostgreSQL):**
```bash
docker compose up --build
```

**Testes:** `mvn test`  (usam H2 em modo PostgreSQL; nenhum serviço externo é necessário).
Para rodar o mesmo que a integração contínua roda: `mvn verify`.

**Maven Wrapper (opcional):** para compilar sem instalar o Maven, gere o wrapper uma única vez com
`mvn wrapper:wrapper`, versione os arquivos criados (`mvnw`, `mvnw.cmd` e a pasta `.mvn/`) e passe a
usar `./mvnw` (Linux/macOS) ou `.\mvnw.cmd` (Windows) no lugar de `mvn`.

## Estrutura

```
br.conexa
├── autenticacao   porta/ (interface pública, eventos)  ·  nucleo/ (regras, entidade)  ·  adaptador/ (Spring Security)
├── areas          porta/ MinhaVidaPort  ·  nucleo/ (reage a PessoaCadastradaEvent)
├── web            controllers MVC (só enxergam portas)
└── config         segurança, relógio
```

Regra verificada por teste (ArchUnit, CT-086): fora do próprio componente, só a `porta` é visível;
controllers nunca acessam `nucleo` nem `adaptador`.

## Segurança

- Senhas com BCrypt; CSRF ativo em todos os formulários; sessão com cookie HttpOnly e SameSite=Lax.
- Cabeçalho CSP restritivo; sem JavaScript inline.
- Perfil `prod`: HTTPS obrigatório (RNF02) e cookie `Secure`. Deve rodar atrás de um proxy reverso
  que termina o TLS e envia `X-Forwarded-Proto`.
- A autorização por Área (RNF01, RNF03) entra com o componente Conexões, na Etapa 5.

## Casos de teste cobertos

CT-001 a CT-011, CT-012 a CT-017 (Áreas), CT-018 a CT-023 e CT-015 (Tarefas), CT-026 a CT-032 (Eventos), CT-033 a CT-054 (Conexões e permissões), CT-055 a CT-079 (Radar, alertas e Dashboard; CT-068, CT-075 e CT-077 também exigem conferência manual; a confirmar com `mvn verify`), CT-081 (senha com hash), CT-086 (arquitetura) e CT-087 (núcleo testado com dublês).
Não verificados aqui: CT-082 (redirecionamento HTTPS, depende da implantação).

## Pendências desta etapa

- `docker compose up` e o `Dockerfile`: validados em 08/11/2026 (funcionando, conforme teste na máquina do desenvolvedor).
- Gerar e versionar o Maven Wrapper (`mvn wrapper:wrapper`).
- Página 404: uma rota inexistente mostra a página padrão do Spring, e não `error/404.html`.
- Teste do redirecionamento para HTTPS no perfil `prod` e verificação do CT-082 na implantação.
- Recuperação de senha e exclusão de conta: fora do MVP, ver revisão de requisitos.
