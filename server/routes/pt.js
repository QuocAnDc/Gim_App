const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/pt/trainers -> Danh sách huấn luyện viên
router.get('/trainers', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT trainer_id AS trainerId, full_name AS fullName, phone, specialty, rating
       FROM pt_trainers`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/pt/book -> Hội viên đặt lịch tập với 1 HLV (trạng thái "pending", chờ HLV xác nhận)
router.post('/book', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { trainerId, scheduledTime, note } = req.body;

    if (!trainerId || !scheduledTime) {
      return res.status(400).json({ error: 'Thiếu trainerId hoặc scheduledTime' });
    }

    const [memberRows] = await pool.query(
      'SELECT member_id FROM members WHERE user_id = ?', [userId]
    );
    if (memberRows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy hội viên' });
    }
    const memberId = memberRows[0].member_id;

    await pool.query(
      `INSERT INTO pt_sessions (member_id, trainer_id, scheduled_time, note, status)
       VALUES (?, ?, ?, ?, 'pending')`,
      [memberId, trainerId, scheduledTime, note || null]
    );

    res.status(201).json({ message: 'Đặt lịch PT thành công, đang chờ huấn luyện viên xác nhận' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// GET /api/pt/my-sessions -> Xem các buổi tập PT của mình
router.get('/my-sessions', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;

    const [rows] = await pool.query(
      `SELECT s.session_id AS sessionId, t.full_name AS trainerName,
              s.scheduled_time AS scheduledTime, s.status, s.note
       FROM pt_sessions s
       JOIN members m ON m.member_id = s.member_id
       JOIN pt_trainers t ON t.trainer_id = s.trainer_id
       WHERE m.user_id = ?
       ORDER BY s.scheduled_time DESC`,
      [userId]
    );

    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;