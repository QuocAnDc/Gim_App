const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/classes -> Xem tất cả lịch lớp học sắp tới (kèm số chỗ còn trống)
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT s.schedule_id AS scheduleId, c.name AS className, c.capacity,
              s.start_time AS startTime, s.end_time AS endTime, s.room,
              (SELECT COUNT(*) FROM class_bookings b
                 WHERE b.schedule_id = s.schedule_id AND b.status = 'booked') AS bookedCount
       FROM class_schedules s
       JOIN group_classes c ON c.class_id = s.class_id
       WHERE s.status = 'open' AND s.start_time >= NOW()
       ORDER BY s.start_time ASC`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/classes/book -> Đặt lịch (bắt buộc đăng nhập)
router.post('/book', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { scheduleId } = req.body;

    if (!scheduleId) {
      return res.status(400).json({ error: 'Thiếu scheduleId' });
    }

    // 1. Lấy member_id
    const [memberRows] = await pool.query(
      'SELECT member_id FROM members WHERE user_id = ?', [userId]
    );
    if (memberRows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy hội viên' });
    }
    const memberId = memberRows[0].member_id;

    // 2. Kiểm tra đã đặt lịch này chưa (tránh đặt trùng)
    const [existing] = await pool.query(
      `SELECT booking_id FROM class_bookings
       WHERE schedule_id = ? AND member_id = ? AND status = 'booked'`,
      [scheduleId, memberId]
    );
    if (existing.length > 0) {
      return res.status(409).json({ error: 'Bạn đã đặt lịch này rồi' });
    }

    // 3. Kiểm tra còn chỗ trống không
    const [capRows] = await pool.query(
      `SELECT c.capacity,
              (SELECT COUNT(*) FROM class_bookings b
                 WHERE b.schedule_id = s.schedule_id AND b.status = 'booked') AS bookedCount
       FROM class_schedules s
       JOIN group_classes c ON c.class_id = s.class_id
       WHERE s.schedule_id = ?`,
      [scheduleId]
    );
    if (capRows.length === 0) {
      return res.status(404).json({ error: 'Lịch học không tồn tại' });
    }
    if (capRows[0].bookedCount >= capRows[0].capacity) {
      return res.status(400).json({ error: 'Lớp đã đầy chỗ' });
    }

    // 4. Tạo booking
    await pool.query(
      `INSERT INTO class_bookings (schedule_id, member_id, status)
       VALUES (?, ?, 'booked')`,
      [scheduleId, memberId]
    );

    res.status(201).json({ message: 'Đặt lịch thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;