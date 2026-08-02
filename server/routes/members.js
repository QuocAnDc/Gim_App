const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/members/me -> Lấy thông tin hội viên + thẻ đang dùng của người đang đăng nhập
router.get('/me', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;

    const [rows] = await pool.query(
          `SELECT u.full_name AS fullName, u.phone, u.email,
                  m.member_code AS memberCode,
                  m.dob AS dob, m.gender AS gender, m.address AS address,
                  c.card_id AS cardId, c.card_code AS cardCode,
                  c.activation_date AS activationDate, c.expiry_date AS expiryDate,
                  c.status AS cardStatus,
                  p.name AS packageName, p.price
       FROM users u
       LEFT JOIN members m ON m.user_id = u.user_id
       LEFT JOIN membership_cards c ON c.member_id = m.member_id
       LEFT JOIN membership_packages p ON p.package_id = c.package_id
       WHERE u.user_id = ?
       ORDER BY c.card_id DESC
       LIMIT 1`,
      [userId]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy hội viên' });
    }

    res.json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// PUT /api/members/me -> Cập nhật thông tin cá nhân
router.put('/me', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { fullName, email, dob, gender, address } = req.body;

    await connection.beginTransaction();

    if (fullName || email !== undefined) {
      await connection.query(
        'UPDATE users SET full_name = COALESCE(?, full_name), email = ? WHERE user_id = ?',
        [fullName || null, email || null, userId]
      );
    }

    await connection.query(
      `UPDATE members SET dob = ?, gender = ?, address = ? WHERE user_id = ?`,
      [dob || null, gender || null, address || null, userId]
    );

    await connection.commit();
    connection.release();

    res.json({ message: 'Cập nhật thông tin thành công' });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;