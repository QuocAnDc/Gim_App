const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// POST /api/checkin -> Hội viên tự check-in bằng thẻ đang có
router.post('/', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;

    // 1. Tìm member + thẻ đang active mới nhất
    const [rows] = await pool.query(
      `SELECT m.member_id, c.card_id, c.status, c.expiry_date
       FROM members m
       LEFT JOIN membership_cards c ON c.member_id = m.member_id
       WHERE m.user_id = ?
       ORDER BY c.card_id DESC
       LIMIT 1`,
      [userId]
    );

    if (rows.length === 0 || !rows[0].card_id) {
      return res.status(400).json({ error: 'Bạn chưa có thẻ tập nào' });
    }

    const card = rows[0];

    // 2. Kiểm tra điều kiện: thẻ phải active và chưa hết hạn
    if (card.status !== 'active') {
      return res.status(403).json({ error: `Thẻ đang ở trạng thái "${card.status}", không thể check-in` });
    }

    const today = new Date().toISOString().split('T')[0];
    if (card.expiry_date && card.expiry_date.toISOString().split('T')[0] < today) {
      return res.status(403).json({ error: 'Thẻ đã hết hạn' });
    }

    // 3. Kiểm tra đã check-in hôm nay chưa check-out chưa (tránh check-in trùng)
    const [openSession] = await pool.query(
      `SELECT log_id FROM checkin_logs
       WHERE card_id = ? AND checkout_time IS NULL
       ORDER BY log_id DESC LIMIT 1`,
      [card.card_id]
    );

    if (openSession.length > 0) {
      // Đã có phiên đang mở -> đây là lượt Check-OUT
      await pool.query(
        'UPDATE checkin_logs SET checkout_time = NOW() WHERE log_id = ?',
        [openSession[0].log_id]
      );
      return res.json({ message: 'Check-out thành công. Hẹn gặp lại!', type: 'checkout' });
    }

    // 4. Chưa có phiên nào mở -> đây là lượt Check-IN mới
    await pool.query(
      `INSERT INTO checkin_logs (member_id, card_id, checkin_time)
       VALUES (?, ?, NOW())`,
      [card.member_id, card.card_id]
    );

    res.status(201).json({ message: 'Check-in thành công. Chúc bạn tập luyện vui vẻ!', type: 'checkin' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// GET /api/checkin/history -> Lịch sử check-in của hội viên
router.get('/history', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;

    const [rows] = await pool.query(
      `SELECT l.log_id AS logId, l.checkin_time AS checkinTime, l.checkout_time AS checkoutTime
       FROM checkin_logs l
       JOIN members m ON m.member_id = l.member_id
       WHERE m.user_id = ?
       ORDER BY l.log_id DESC
       LIMIT 20`,
      [userId]
    );

    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;