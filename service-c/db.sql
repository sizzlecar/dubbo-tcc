CREATE TABLE `transfer_log` (
  `id` int(11) NOT NULL,
  `from_user_id` varchar(30) NOT NULL,
  `to_user_id` varchar(30) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `update_time` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;