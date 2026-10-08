-- (최초 1회, root 계정으로 실행) 데이터베이스 및 계정 생성
CREATE DATABASE IF NOT EXISTS joeun DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'joeun'@'localhost' IDENTIFIED BY '123456';
CREATE USER IF NOT EXISTS 'joeun'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON joeun.* TO 'joeun'@'localhost';
GRANT ALL PRIVILEGES ON joeun.* TO 'joeun'@'%';
FLUSH PRIVILEGES;
