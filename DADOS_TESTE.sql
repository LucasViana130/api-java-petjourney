-- Script para popular o banco de dados PetJourney (H2)

-- 1. Inserir Clínicas (TBCLINICA)
INSERT INTO TB_CLINICA (name, cnpj, phone, email, address) VALUES 
('Clínica Veterinária Pet Feliz', '12345678000190', '11987654321', 'contato@petfeliz.com.br', 'Rua dos Animais, 123 - São Paulo'),
('Hospital Veterinário Saúde Animal', '98765432000110', '11912345678', 'atendimento@saudeanimal.com', 'Av. Paulista, 1000 - São Paulo');

-- 2. Inserir Veterinários (TBVETERINARIO)
-- Vinculados à Clínica 1 (Pet Feliz)
INSERT INTO TB_VETERINARIO (name, crmv, phone, email, specialty, clinic_id) VALUES 
('Dr. João Silva', 'CRMV-SP 12345', '11977776666', 'joao.silva@vet.com', 'Clínico Geral', 1),
('Dra. Maria Oliveira', 'CRMV-SP 54321', '11966665555', 'maria.oliveira@vet.com', 'Cirurgiã', 1);

-- Vinculados à Clínica 2 (Saúde Animal)
INSERT INTO TB_VETERINARIO (name, crmv, phone, email, specialty, clinic_id) VALUES 
('Dr. Ricardo Santos', 'CRMV-SP 99887', '11944443333', 'ricardo.santos@vet.com', 'Dermatologista', 2);

-- 3. Inserir Tutores (TBTUTOR)
INSERT INTO TB_TUTOR (name, cpf, phone, email) VALUES 
('Carlos Souza', '12345678901', '11911112222', 'carlos.souza@email.com'),
('Ana Paula Lima', '98765432100', '11933334444', 'ana.lima@email.com');

-- 4. Inserir Pets (TBPET)
-- Pets do Carlos (Tutor 1)
INSERT INTO TB_PET (name, species, breed, sex, birth_date, weight, tutor_id) VALUES 
('Rex', 'CACHORRO', 'Golden Retriever', 'MACHO', '2020-05-15', 32.5, 1),
('Mel', 'GATO', 'Persa', 'FEMEA', '2021-08-20', 4.2, 1);

-- Pets da Ana (Tutor 2)
INSERT INTO TB_PET (name, species, breed, sex, birth_date, weight, tutor_id) VALUES 
('Thor', 'CACHORRO', 'Bulldog Francês', 'MACHO', '2022-01-10', 12.8, 2);

-- 5. Inserir Prontuários (TBPRONTUARIO)
INSERT INTO TB_PRONTUARIO (registration_date, main_complaint, diagnosis, conduct, observations, pet_id, veterinarian_id) VALUES 
('2023-10-01 10:00:00', 'Vômitos e diarreia', 'Gastroenterite leve', 'Dieta leve e hidratação', 'Animal um pouco desidratado', 1, 1),
('2023-11-15 14:30:00', 'Coceira intensa nas orelhas', 'Otite bacteriana', 'Limpeza e antibiótico local', 'Ouvido direito mais inflamado', 3, 3);

-- 6. Inserir Medicamentos (TBMEDICAMENTO)
INSERT INTO TB_MEDICAMENTO (name, dosage, frequency, start_date, end_date, observations, status, pet_id, veterinarian_id) VALUES 
('Amoxicilina', '250mg', '12 em 12 horas', '2023-10-01', '2023-10-08', 'Dar após a refeição', 'FINALIZADO', 1, 1),
('Otopet Gotas', '3 gotas', '8 em 8 horas', '2023-11-15', '2023-11-22', 'Limpar antes de aplicar', 'ATIVO', 3, 3);

-- 7. Inserir Agendamentos (TBAGENDAMENTO)
INSERT INTO TB_AGENDAMENTO (title, description, date_time, status, pet_id, veterinarian_id, clinic_id) VALUES 
('Vacinação V10', 'Reforço anual da vacina V10', '2024-05-20 09:00:00', 'PENDENTE', 1, 1, 1),
('Retorno Otite', 'Avaliação do tratamento de otite', '2023-11-25 16:00:00', 'REALIZADO', 3, 3, 2);
