const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/lockers -> Xem tất cả tủ + trạng thái
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT locker_id AS lockerId, code, status FROM lockers ORDER BY code`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/lockers/rent -> Thuê 1 tủ còn trống
router.post('/rent', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { lockerId } = req.body;

    if (!lockerId) {
      connection.release();
      return res.status(400).json({ error: 'Thiếu lockerId' });
    }

    await connection.beginTransaction();

    const [memberRows] = await connection.query(
      'SELECT member_id FROM members WHERE user_id = ?', [userId]
    );
    if (memberRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'Không tìm thấy hội viên' });
    }
    const memberId = memberRows[0].member_id;

    // Khoá dòng tủ này lại khi kiểm tra, tránh 2 người cùng thuê 1 tủ cùng lúc (race condition)
    const [lockerRows] = await connection.query(
      'SELECT * FROM lockers WHERE locker_id = ? FOR UPDATE', [lockerId]
    );
    if (lockerRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'Tủ không tồn tại' });
    }
    if (lockerRows[0].status !== 'available') {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Tủ này đã có người dùng' });
    }

    await connection.query(
      `INSERT INTO locker_rentals (locker_id, member_id, status) VALUES (?, ?, 'active')`,
      [lockerId, memberId]
    );
    await connection.query(
      `UPDATE lockers SET status = 'occupied' WHERE locker_id = ?`, [lockerId]
    );

    await connection.commit();
    connection.release();

    res.status(201).json({ message: 'Thuê tủ thành công' });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/lockers/return -> Trả tủ đang thuê
router.post('/return', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { lockerId } = req.body;

    await connection.beginTransaction();

    const [memberRows] = await connection.query(
      'SELECT member_id FROM members WHERE user_id = ?', [userId]
    );
    const memberId = memberRows[0].member_id;

    const [rentalRows] = await connection.query(
      `SELECT rental_id FROM locker_rentals
       WHERE locker_id = ? AND member_id = ? AND status = 'active'
       ORDER BY rental_id DESC LIMIT 1`,
      [lockerId, memberId]
    );
    if (rentalRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Bạn không thuê tủ này' });
    }

    await connection.query(
      `UPDATE locker_rentals SET status = 'returned', end_time = NOW() WHERE rental_id = ?`,
      [rentalRows[0].rental_id]
    );
    await connection.query(
      `UPDATE lockers SET status = 'available' WHERE locker_id = ?`, [lockerId]
    );

    await connection.commit();
    connection.release();

    res.json({ message: 'Trả tủ thành công' });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;