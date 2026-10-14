-- Estrutura da unica tabela do cliente-service.
CREATE TABLE cliente (
    id        BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    documento VARCHAR(20)  NOT NULL UNIQUE,
    email     VARCHAR(120) NOT NULL,
    cidade    VARCHAR(80),
    uf        VARCHAR(2)
);
