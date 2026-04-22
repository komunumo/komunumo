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

-- [jooq ignore start]
INSERT INTO actor_handle (handle, user_id, community_id)
    SELECT SUBSTRING_INDEX(TRIM(LEADING '@' FROM TRIM(profile)), '@', 1), id, NULL
        FROM user
        WHERE profile IS NOT NULL
            AND TRIM(profile) <> '';

INSERT INTO actor_handle (handle, user_id, community_id)
    SELECT SUBSTRING_INDEX(TRIM(LEADING '@' FROM TRIM(profile)), '@', 1), NULL, id
        FROM community
        WHERE TRIM(profile) <> '';
-- [jooq ignore stop]

ALTER TABLE user DROP COLUMN profile;
ALTER TABLE community DROP COLUMN profile;
ALTER TABLE `community` CHANGE `description` `description` TEXT NOT NULL DEFAULT '';
