-- Dados de teste, exigidos pela tarefa ("arquivos para popular o banco de dados
-- para teste"). Rodam uma vez so: o Flyway registra em flyway_schema_history
-- que a V2 ja foi aplicada e nao repete na proxima inicializacao.
INSERT INTO cliente (nome, documento, email, cidade, uf) VALUES
    ('Mariana Lopes',          '123.456.789-00',     'mariana.lopes@email.com',   'Morrinhos',  'GO'),
    ('Comercial Bom Preco',    '12.345.678/0001-90', 'compras@bompreco.com.br',   'Goiania',    'GO'),
    ('Rafael Teixeira',        '987.654.321-00',     'rafael.teixeira@email.com', 'Uberlandia', 'MG'),
    ('Distribuidora Sul',      '23.456.789/0001-01', 'pedidos@distsul.com.br',    'Curitiba',   'PR');
