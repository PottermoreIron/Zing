CREATE TABLE `leaf_alloc`
(
    `biz_tag`     varchar(128) NOT NULL DEFAULT '',
    `max_id`      bigint(20)   NOT NULL DEFAULT '1',
    `step`        int(11)      NOT NULL,
    `description` varchar(256)          DEFAULT NULL,
    `update_time` datetime     NOT NULL DEFAULT now() ON UPDATE now(),
    PRIMARY KEY (`biz_tag`)
) ENGINE = InnoDB;

insert into leaf_alloc(biz_tag, max_id, step, description)
values ('user', 1, 2000, 'get user id');