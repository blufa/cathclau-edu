-- Blog d'entreprise (contenu plateforme, hors tenant) : catégories, articles, tags, commentaires modérés.

CREATE TABLE blog_categories (
    id    BIGSERIAL PRIMARY KEY,
    label VARCHAR(150) NOT NULL,
    slug  VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE blog_posts (
    id               BIGSERIAL PRIMARY KEY,
    slug             VARCHAR(220) NOT NULL UNIQUE,
    title            VARCHAR(255) NOT NULL,
    excerpt          VARCHAR(500),
    cover_image_url  VARCHAR(1024),
    content_html     TEXT NOT NULL,
    category_id      BIGINT REFERENCES blog_categories(id),
    author_id        BIGINT NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP
);
CREATE INDEX idx_blog_posts_status ON blog_posts (status, published_at);

CREATE TABLE blog_post_tags (
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    tag     VARCHAR(60) NOT NULL,
    PRIMARY KEY (post_id, tag)
);

CREATE TABLE blog_comments (
    id           BIGSERIAL PRIMARY KEY,
    post_id      BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    author_name  VARCHAR(150) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    website      VARCHAR(255),
    content      TEXT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_blog_comments_post_status ON blog_comments (post_id, status);

-- Rôle Rédacteur : compte plateforme strictement limité au blog.
INSERT INTO roles (name, code, description)
VALUES ('Rédacteur', 'REDACTEUR', 'Gestion du blog d''entreprise uniquement (aucun accès écoles/abonnements)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (name, code) VALUES
    ('Create blog post', 'article:create'),
    ('Read blog post', 'article:read'),
    ('Update blog post', 'article:update'),
    ('Delete blog post', 'article:delete'),
    ('Publish blog post', 'article:publish'),
    ('Moderate blog comments', 'article:moderate_comments')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'REDACTEUR'
  AND p.code IN ('article:create','article:read','article:update','article:delete','article:publish','article:moderate_comments')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SUPER_ADMIN a déjà toutes les permissions via le CROSS JOIN de V16 (INSERT au moment du seed initial) ;
-- pour les nouvelles permissions ajoutées après coup, il faut les rattacher explicitement.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('article:create','article:read','article:update','article:delete','article:publish','article:moderate_comments')
ON CONFLICT (role_id, permission_id) DO NOTHING;
