## Build e testes

`mvn verify`: BUILD SUCCESS. 54 testes, zero falhas, zero erros e nenhum ignorado. JAR gerado. Os testes de serviços/repositories utilizam mocks e os testes de controllers utilizam MockMvc; isso não é um teste completo do banco.

| Classe | Testes | Falhas | Erros |
|---|---:|---:|---:|
| SmartCareClientTest | 14 | 0 | 0 |
| ActiveAccountTokenValidatorTest | 5 | 0 | 0 |
| SmartCareControllerTest | 5 | 0 | 0 |
| SystemAdminControllerTest | 1 | 0 | 0 |
| SqlStateErrorMapperTest | 3 | 0 | 0 |
| ReportProcedureRepositoryTest | 4 | 0 | 0 |
| ClinicServiceTest | 3 | 0 | 0 |
| PetServiceTest | 2 | 0 | 0 |
| ReportServiceTest | 4 | 0 | 0 |
| SmartCareServiceTest | 9 | 0 | 0 |
| SystemAdminServiceTest | 4 | 0 | 0 |

## Validação HTTP real

31 verificações passaram usando JAR e PostgreSQL locais:

- Login dos perfis, relatórios de pets/pesos, filtro e isolamento entre duas clínicas.
- 400 para filtro inválido, 401 sem token e 403 para veterinário nos relatórios.
- SmartCare: 422 sem prontuário, 404 para pet de outra clínica e 503 com serviço desligado.
- Análise e ranking 200 com **servidor HTTP simulado**, exclusivamente para conferir protocolo. Não comprova IA real.
- Cadastro de tutor com pet, ativação, login e prontuário.
- Consulta de prontuário antes da inativação para preencher o cache; depois de inativar o tutor, o mesmo token recebeu 401, assim como o novo login.
- Token de veterinário recusado com 401 após inativação, inclusive em rota consultada anteriormente.
- Token de administrador da clínica recusado com 401 após inativação da clínica.

## Migrations

V1–V10 preservadas byte a byte em relação à base anterior. Hashes SHA-256:

| Arquivo | SHA-256 |
|---|---|
| V10__petjourney_rotinas.sql | `41a50b7e617f946daaf4466669065b785e9b2af9128258ad6791eddcb46b2e5c` |
| V1__create_core_schema.sql | `e19c269fdce1f6684c4cc0868d718788311bc08d1d6a280642efa1b05019ea82` |
| V2__create_users_and_access_links.sql | `c6529c1ef73274f544af221353beee292334a1397a7da71745cddad8ed34d89f` |
| V3__seed_sprint3_data.sql | `2e6ad563cc6f997d0b5e21255dd06678fc6360f0f1778fd425010dc745dd901d` |
| V4__add_system_admin_and_clinic_tutor_isolation.sql | `47949c489c1822a50fc1a2b67a3f8309ee5a6af4900ae06c6018d0302d89fb48` |
| V5__single_clinic_tutor_first_access_and_availability.sql | `1312fc0730206ab5ce03b9fc25ebe20f99fcc86c4c87c00217d61a53b3de91d1` |
| V6__add_consultation_report_fields.sql | `f8ef1831e2d2949c278114798cdaa3af1514a502b79cbf8bc71583a93583d160` |
| V7__add_soft_delete_flags.sql | `3ec27fa4d7c677f4821d493749cfead580861f8817065c8a45f68da00d0eeceb` |
| V8__petjourney_objetos_tecnicos.sql | `17de2f2260c7e501f4e68e63d6b4e6e327385703bcd586d67879624f09cde39e` |
| V9__petjourney_integridade_e_gatilhos.sql | `6a08bcadb0ffaf83a27a00b9234ae8ac80ced75b3cb0ab5f680cad9ce33f7032` |
