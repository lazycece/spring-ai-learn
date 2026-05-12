-- users
INSERT INTO users (name, email, city) VALUES ('张三', 'zhangsan@example.com', '北京');
INSERT INTO users (name, email, city) VALUES ('李四', 'lisi@example.com', '上海');
INSERT INTO users (name, email, city) VALUES ('王五', 'wangwu@example.com', '广州');
INSERT INTO users (name, email, city) VALUES ('赵六', 'zhaoliu@example.com', '深圳');

-- products
INSERT INTO products (name, category, price, stock) VALUES ('iPhone 15', '电子产品', 5999.00, 120);
INSERT INTO products (name, category, price, stock) VALUES ('MacBook Pro', '电子产品', 12999.00, 45);
INSERT INTO products (name, category, price, stock) VALUES ('无线耳机', '配件', 299.00, 15);
INSERT INTO products (name, category, price, stock) VALUES ('机械键盘', '配件', 499.00, 32);
INSERT INTO products (name, category, price, stock) VALUES ('运动鞋', '服装', 699.00, 200);
INSERT INTO products (name, category, price, stock) VALUES ('T恤', '服装', 99.00, 500);
INSERT INTO products (name, category, price, stock) VALUES ('充电宝', '配件', 149.00, 8);
INSERT INTO products (name, category, price, stock) VALUES ('iPad Air', '电子产品', 4599.00, 80);

-- orders
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (1, 1, 2, 11998.00, '2026-04-01 10:30:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (1, 3, 1, 299.00, '2026-04-02 14:20:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (2, 2, 1, 12999.00, '2026-04-05 09:15:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (2, 4, 1, 499.00, '2026-04-06 16:45:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (3, 1, 1, 5999.00, '2026-04-10 11:00:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (3, 5, 3, 2097.00, '2026-04-12 20:30:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (4, 8, 1, 4599.00, '2026-04-15 08:00:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (4, 6, 5, 495.00, '2026-04-18 13:45:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (1, 5, 2, 1398.00, '2026-04-22 18:00:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (2, 8, 1, 4599.00, '2026-04-25 10:10:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (3, 2, 1, 12999.00, '2026-04-28 15:30:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (4, 1, 1, 5999.00, '2026-05-01 09:00:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (1, 4, 1, 499.00, '2026-05-03 14:00:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (2, 7, 2, 298.00, '2026-05-05 17:30:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (3, 3, 1, 299.00, '2026-05-08 11:45:00');
INSERT INTO orders (user_id, product_id, quantity, amount, order_date) VALUES (4, 5, 1, 699.00, '2026-05-10 16:00:00');
