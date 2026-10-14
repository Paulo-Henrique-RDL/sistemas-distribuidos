-- Dados de teste. O ultimo produto tem estoque 2 de proposito: e com ele que
-- se demonstra a recusa por estoque insuficiente no pedido-service.
INSERT INTO produto (sku, nome, descricao, preco, estoque) VALUES
    ('TEC-001', 'Teclado mecanico ABNT2',  'Switch marrom, retroiluminado',        289.90,  40),
    ('MOU-002', 'Mouse sem fio 1600 DPI',  'Conexao 2.4 GHz com receptor USB',      89.90, 120),
    ('MON-003', 'Monitor 24 polegadas',    'IPS Full HD, 75 Hz, HDMI e VGA',       949.00,  15),
    ('HEA-004', 'Headset com microfone',   'Over-ear, cabo P2, controle no fio',   159.90,  60),
    ('CAD-005', 'Cadeira de escritorio',   'Encosto em tela, apoio de braco 3D', 1249.00,   2);
