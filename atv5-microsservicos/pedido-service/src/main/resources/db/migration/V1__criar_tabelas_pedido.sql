-- As duas tabelas que se relacionam, exigidas pela tarefa.
--
-- cliente_id e produto_id sao apenas identificadores: NAO ha FOREIGN KEY para
-- eles, porque cliente e produto vivem nos bancos dos outros microsservicos.
-- Quem valida esses ids e a chamada HTTP feita antes de gravar.
CREATE TABLE pedido (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    cliente_id  BIGINT        NOT NULL,
    situacao    VARCHAR(20)   NOT NULL,
    valor_total NUMERIC(15,2) NOT NULL,
    data_pedido TIMESTAMP     NOT NULL
);

-- Aqui sim ha chave estrangeira de verdade: as duas tabelas estao no mesmo
-- banco, entao o proprio banco garante que nao existe item orfao.
CREATE TABLE item_pedido (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL,
    produto_id     BIGINT        NOT NULL,
    sku            VARCHAR(30)   NOT NULL,
    nome_produto   VARCHAR(150)  NOT NULL,
    quantidade     INTEGER       NOT NULL,
    preco_unitario NUMERIC(15,2) NOT NULL,
    subtotal       NUMERIC(15,2) NOT NULL,
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
);

CREATE INDEX idx_pedido_cliente ON pedido (cliente_id);
CREATE INDEX idx_item_pedido    ON item_pedido (pedido_id);
