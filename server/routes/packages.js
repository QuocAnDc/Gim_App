const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT package_id AS packageId, name, duration_days AS durationDays, price
       FROM membership_packages`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.post('/buy', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { packageId } = req.body;

    if (!packageId) {
      connection.release();
      return res.status(400).json({ error: 'Thiếu packageId' });
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

    const [pkgRows] = await connection.query(
      'SELECT * FROM membership_packages WHERE package_id = ?', [packageId]
    );
    if (pkgRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'Gói tập không tồn tại' });
    }
    const pkg = pkgRows[0];

    const cardCode = 'CARD-' + Date.now();
    const [cardResult] = await connection.query(
      `INSERT INTO membership_cards
         (member_id, package_id, card_code, activation_date, expiry_date, status)
       VALUES (?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY), 'active')`,
      [memberId, packageId, cardCode, pkg.duration_days]
    );
    const cardId = cardResult.insertId;

    await connection.query(
      `INSERT INTO payments (member_id, card_id, amount, method, status)
       VALUES (?, ?, ?, 'cash', 'success')`,
      [memberId, cardId, pkg.price]
    );

    await connection.commit();
    connection.release();

    res.status(201).json({ message: 'Mua gói tập thành công', cardId, cardCode });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;