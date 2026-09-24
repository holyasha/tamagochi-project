-- Таблица владельцев тамагочи
CREATE TABLE owners (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    birth_date DATE NOT NULL,
    tamagochis_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

-- Таблица тамагочи
CREATE TABLE tamagochis (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    species VARCHAR(100) NOT NULL,
    color VARCHAR(50) NOT NULL,
    is_alive BOOLEAN NOT NULL DEFAULT true,
    health INTEGER NOT NULL CHECK (health >= 0 AND health <= 100),
    hunger INTEGER NOT NULL CHECK (hunger >= 0 AND hunger <= 100),
    happiness INTEGER NOT NULL CHECK (happiness >= 0 AND happiness <= 100),
    energy INTEGER NOT NULL CHECK (energy >= 0 AND energy <= 100),
    clearliness INTEGER NOT NULL CHECK (clearliness >= 0 AND clearliness <= 100),
    birth_date DATE NOT NULL,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Индекс для быстрого поиска тамагочи по владельцу
CREATE INDEX idx_tamagochis_owner_id ON tamagochis(owner_id);

-- Индекс для поиска живых тамагочи
CREATE INDEX idx_tamagochis_is_alive ON tamagochis(is_alive);
