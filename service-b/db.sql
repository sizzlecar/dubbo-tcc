CREATE TABLE `user_account_freeze` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_account_id` int(11) NOT NULL COMMENT '用户账号表',
  `amount` decimal(12,2) NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_account` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL COMMENT '用户id',
  `type` int(11) NOT NULL COMMENT '账号类型，0 人名币，1 美元',
  `balance` decimal(18,4) NOT NULL COMMENT '账号余额',
  `update_time` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;