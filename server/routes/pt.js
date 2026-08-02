const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/pt/trainers -> Danh sách huấn luyện viên
router.get('/trainers', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT trainer_id AS trainerId, full_name AS fullName, phone, specialty, rating,
              price_per_session AS pricePerSession
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
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { trainerId, scheduledTime, note } = req.body;

    if (!trainerId || !scheduledTime) {
      connection.release();
      return res.status(400).json({ error: 'Thiếu trainerId hoặc scheduledTime' });
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

    const [trainerRows] = await connection.query(
      'SELECT * FROM pt_trainers WHERE trainer_id = ?', [trainerId]
    );
    if (trainerRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'Huấn luyện viên không tồn tại' });
    }
    const trainer = trainerRows[0];

    let [walletRows] = await connection.query(
      'SELECT * FROM wallets WHERE member_id = ? FOR UPDATE', [memberId]
    );
    let wallet;
    if (walletRows.length === 0) {
      const [result] = await connection.query('INSERT INTO wallets (member_id, balance) VALUES (?, 0)', [memberId]);
      wallet = { wallet_id: result.insertId, balance: 0 };
    } else {
      wallet = walletRows[0];
    }

    if (Number(wallet.balance) < Number(trainer.price_per_session)) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Số dư ví không đủ, vui lòng nạp thêm tiền' });
    }

    await connection.query(
      'UPDATE wallets SET balance = balance - ? WHERE wallet_id = ?',
      [trainer.price_per_session, wallet.wallet_id]
    );
    await connection.query(
      'INSERT INTO wallet_transactions (wallet_id, amount, type, note) VALUES (?, ?, "purchase", ?)',
      [wallet.wallet_id, trainer.price_per_session, `Đặt lịch PT với ${trainer.full_name}`]
    );

    await connection.query(
      `INSERT INTO pt_sessions (member_id, trainer_id, scheduled_time, note, status)
       VALUES (?, ?, ?, ?, 'pending')`,
      [memberId, trainerId, scheduledTime, note || null]
    );

    await connection.commit();
    connection.release();

    res.status(201).json({ message: 'Đặt lịch PT thành công, đang chờ huấn luyện viên xác nhận' });
  } catch (err) {
    await connection.rollback();
    connection.release();
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

// PUT /api/pt/cancel/:sessionId -> Hủy 1 lịch PT đã đặt
router.put('/cancel/:sessionId', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const sessionId = req.params.sessionId;

    const [rows] = await pool.query(
      `SELECT s.session_id, s.status FROM pt_sessions s
       JOIN members m ON m.member_id = s.member_id
       WHERE s.session_id = ? AND m.user_id = ?`,
      [sessionId, userId]
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy lịch đặt này' });
    }
    if (!['pending', 'confirmed'].includes(rows[0].status)) {
      return res.status(400).json({ error: 'Không thể hủy lịch đã hoàn thành hoặc đã hủy trước đó' });
    }

    await pool.query(`UPDATE pt_sessions SET status = 'cancelled' WHERE session_id = ?`, [sessionId]);

    res.json({ message: 'Hủy lịch PT thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;