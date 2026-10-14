-- Estrutura da unica tabela do catalogo-service.
CREATE TABLE produto (
    id        BIGINT        AUTO_INCREMENT PRIMARY KEY,
    sku       VARCHAR(30)   NOT NULL UNIQUE,
    nome      VARCHAR(150)  NOT NULL,
    descricao VARCHAR(400),
    preco     NUMERIC(15,2) NOT NULL,
    estoque   INTEGER       NOT NULL
);
