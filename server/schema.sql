CREATE DATABASE IF NOT EXISTS gym_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gym_db;

CREATE TABLE roles (
    role_id     INT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE branches (
    branch_id   INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(20)
);

CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(20) UNIQUE NOT NULL,
    email         VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id       INT NOT NULL,
    branch_id     INT,
    status        ENUM('active','locked') DEFAULT 'active',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE members (
    member_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL UNIQUE,
    dob           DATE,
    gender        ENUM('male','female','other'),
    address       VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE membership_packages (
    package_id    INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    price         DECIMAL(12,2) NOT NULL
);

CREATE TABLE membership_cards (
    card_id          INT AUTO_INCREMENT PRIMARY KEY,
    member_id        INT NOT NULL,
    package_id       INT NOT NULL,
    card_code        VARCHAR(50) UNIQUE NOT NULL,
    activation_date  DATE,
    expiry_date      DATE,
    status           ENUM('pending','active','frozen','expired','cancelled') DEFAULT 'pending',
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (package_id) REFERENCES membership_packages(package_id)
);

CREATE TABLE checkin_logs (
    log_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     INT NOT NULL,
    card_id       INT NOT NULL,
    checkin_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    checkout_time DATETIME NULL,
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (card_id) REFERENCES membership_cards(card_id)
);

INSERT INTO roles (role_name) VALUES ('ADMIN'),('LE_TAN'),('HOI_VIEN'),('PT');
INSERT INTO branches (name, address, phone) VALUES ('Chi nhánh Quận 1', '123 Nguyễn Huệ, Q1', '0900000001');
INSERT INTO membership_packages (name, duration_days, price) VALUES ('Classic 1 tháng', 30, 500000);