INSERT INTO users (username, password, role, created_date)
VALUES ('admin',  '$2a$10$pcqD.SM31hFddwkeXqTYLewqT4cFU2zVKhjPAC8LiLBAeF0gvW/LO', 'ADMIN', CURRENT_TIMESTAMP),
       ('user1',  '$2a$10$/r.9ONaJIGgAtjpCNJGJaOMx16fTRa7Sk4qG/zEKT.YVzF2EMRLxi', 'USER',  CURRENT_TIMESTAMP),
       ('user2',  '$2a$10$zLLlBnZqyd3gsr1f8LOJx.6FEl5ETFxUH5WwDKoFEaH5QgQ9LkI4m', 'USER',  CURRENT_TIMESTAMP);
