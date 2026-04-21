CREATE TABLE actor_handle (
    handle VARCHAR(30) NOT NULL,
    user_id VARCHAR(36) DEFAULT NULL,
    community_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (handle),
    CONSTRAINT uk_actor_handle_user
        UNIQUE (user_id),
    CONSTRAINT uk_actor_handle_community
        UNIQUE (community_id),
    CONSTRAINT fk_actor_handle_user
        FOREIGN KEY (user_id)
            REFERENCES user (id),
    CONSTRAINT fk_actor_handle_community
        FOREIGN KEY (community_id)
            REFERENCES community (id)
);
