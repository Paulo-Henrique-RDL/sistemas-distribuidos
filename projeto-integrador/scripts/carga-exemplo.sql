-- =============================================================================
-- Carga de exemplo, opcional
-- =============================================================================
--
-- Serve para quem quiser abrir o sistema ja com dados, em vez de cadastrar tudo
-- pela API. Nao e executado automaticamente: a aplicacao sobe com o banco vazio.
--
-- Como aplicar:
--   1. suba a aplicacao (as tabelas sao criadas);
--   2. abra http://localhost:8080/h2-console
--      JDBC URL: jdbc:h2:file:./dados/contas    usuario: sa    senha: (vazia)
--   3. cole e execute este script.
--
-- Atencao: rodar duas vezes gera erro de documento e nome duplicados, que sao
-- campos unicos.
-- =============================================================================

INSERT INTO pessoa (nome, documento, tipo, email, telefone) VALUES
    ('Papelaria Central Ltda', '12.345.678/0001-90', 'FORNECEDOR', 'contato@papelaria.com.br', '64 3333-1000'),
    ('Energia Goias S/A',      '23.456.789/0001-01', 'FORNECEDOR', 'atendimento@energia.com.br', '0800 123-4567'),
    ('Mercado Bom Preco',      '98.765.432/0001-10', 'CLIENTE',    'financeiro@bompreco.com.br', '64 3222-5000'),
    ('Jose Carlos da Silva',   '123.456.789-00',     'CLIENTE',    'jose.silva@email.com',       '64 99888-1234'),
    ('Transportadora Rapida',  '34.567.890/0001-12', 'AMBOS',      'contato@rapida.com.br',      '64 3444-2000');

INSERT INTO categoria (nome, descricao) VALUES
    ('Material de escritorio', 'Papelaria, suprimentos e consumiveis'),
    ('Energia eletrica',       'Contas de energia das unidades'),
    ('Vendas de produtos',     'Receitas de venda de mercadorias'),
    ('Prestacao de servicos',  'Receitas de servicos prestados'),
    ('Fretes e transportes',   'Despesas e receitas de transporte');

INSERT INTO forma_pagamento (nome) VALUES
    ('Dinheiro'), ('PIX'), ('Boleto bancario'), ('Cartao de credito'), ('Transferencia bancaria');

-- Contas a pagar em situacoes variadas.
-- A terceira tem vencimento no passado e continua ABERTA: a aplicacao a exibe
-- como VENCIDA, porque essa situacao e derivada, nao gravada.
INSERT INTO conta_pagar
    (descricao, numero_documento, fornecedor_id, categoria_id, valor, data_emissao,
     data_vencimento, data_pagamento, valor_pago, forma_pagamento_id, situacao, observacao, data_cadastro)
VALUES
    ('Compra de papel A4 e toner', 'NF-1001', 1, 1,  450.00, '2026-10-01', '2026-11-10', NULL, NULL, NULL, 'ABERTA',   'Pedido mensal',        CURRENT_TIMESTAMP),
    ('Energia eletrica - outubro', 'NF-2210', 2, 2,  830.50, '2026-10-05', '2026-10-25', NULL, NULL, NULL, 'ABERTA',   NULL,                   CURRENT_TIMESTAMP),
    ('Energia eletrica - agosto',  'NF-2208', 2, 2,  795.30, '2026-08-05', '2026-08-25', NULL, NULL, NULL, 'ABERTA',   'Em atraso',            CURRENT_TIMESTAMP),
    ('Frete de mercadorias',       'CTE-553', 5, 5,  320.00, '2026-09-10', '2026-09-30', '2026-09-28', 320.00, 2,      'PAGA',     'Pago via PIX',         CURRENT_TIMESTAMP),
    ('Compra cancelada',           NULL,      1, 1,  150.00, '2026-09-15', '2026-10-15', NULL, NULL, NULL, 'CANCELADA','Pedido cancelado',     CURRENT_TIMESTAMP);

-- Contas a receber em situacoes variadas.
INSERT INTO conta_receber
    (descricao, numero_documento, cliente_id, categoria_id, valor, data_emissao,
     data_vencimento, data_recebimento, valor_recebido, forma_recebimento_id, situacao, observacao, data_cadastro)
VALUES
    ('Venda de suprimentos',    'NF-2001', 3, 3, 1250.00, '2026-10-02', '2026-11-02', NULL, NULL, NULL, 'ABERTA',   NULL,                CURRENT_TIMESTAMP),
    ('Servico de manutencao',   'NF-2002', 4, 4,  680.00, '2026-09-20', '2026-10-20', NULL, NULL, NULL, 'ABERTA',   NULL,                CURRENT_TIMESTAMP),
    ('Venda de agosto',         'NF-1990', 3, 3,  430.00, '2026-08-10', '2026-09-10', NULL, NULL, NULL, 'ABERTA',   'Cliente notificado', CURRENT_TIMESTAMP),
    ('Frete prestado',          'CTE-110', 5, 5,  275.00, '2026-09-01', '2026-09-30', '2026-09-29', 275.00, 5,      'RECEBIDA', NULL,                CURRENT_TIMESTAMP),
    ('Venda devolvida',         NULL,      4, 3,  199.90, '2026-09-05', '2026-10-05', NULL, NULL, NULL, 'CANCELADA','Produto devolvido',  CURRENT_TIMESTAMP);
