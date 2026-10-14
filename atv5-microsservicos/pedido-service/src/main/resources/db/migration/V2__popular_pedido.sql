-- Dois pedidos de exemplo, com itens, para o sistema ja subir com dados.
-- Os ids de cliente e produto correspondem aos inseridos nos outros dois
-- microsservicos pelas migrations V2 de cada um.
INSERT INTO pedido (id, cliente_id, situacao, valor_total, data_pedido) VALUES
    (1, 1, 'CRIADO',    1229.70, TIMESTAMP '2026-10-10 09:30:00'),
    (2, 2, 'CANCELADO',  949.00, TIMESTAMP '2026-10-11 14:05:00');

-- O monitor aparece a 690,00 no pedido 1 e a 949,00 no pedido 2 de proposito:
-- e o preco que valia em cada data. O item guarda o valor praticado, e por isso
-- o historico nao muda quando o catalogo reajusta.
INSERT INTO item_pedido (pedido_id, produto_id, sku, nome_produto, quantidade, preco_unitario, subtotal) VALUES
    (1, 1, 'TEC-001', 'Teclado mecanico ABNT2', 1, 289.90,  289.90),
    (1, 2, 'MOU-002', 'Mouse sem fio 1600 DPI', 1,  89.90,   89.90),
    (1, 4, 'HEA-004', 'Headset com microfone',  1, 159.90,  159.90),
    (1, 3, 'MON-003', 'Monitor 24 polegadas',   1, 690.00,  690.00),
    (2, 3, 'MON-003', 'Monitor 24 polegadas',   1, 949.00,  949.00);

-- O H2 nao avanca o contador automatico quando o id e informado na insercao,
-- entao o proximo pedido criado pela API comecaria no 1 e colidiria.
ALTER TABLE pedido ALTER COLUMN id RESTART WITH 3;
