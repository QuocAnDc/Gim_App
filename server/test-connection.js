require('dotenv').config();
const mysql = require('mysql2/promise');

async function testConnection() {
  try {
    const connection = await mysql.createConnection({
      host: process.env.DB_HOST,
      port: process.env.DB_PORT,
      user: process.env.DB_USER,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_NAME,
    });

    console.log(' Kết nối MySQL thành công!');

    const [rows] = await connection.query('SELECT COUNT(*) AS total FROM roles');
    console.log('Số dòng trong bảng roles:', rows[0].total);

    await connection.end();
  } catch (err) {
    console.error(' Kết nối thất bại:', err.message);
  }
}

testConnection();