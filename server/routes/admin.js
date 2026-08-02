const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');
const { requireAdmin } = require('../middleware/requireAdmin');

const router = express.Router();

// Áp dụng cho MỌI route trong file này: phải đăng nhập + phải là Admin
router.use(requireAuth, requireAdmin);

/* ================= NGƯỜI DÙNG ================= */

// GET /api/admin/users -> Danh sách toàn bộ người dùng
router.get('/users', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT u.user_id AS userId, u.full_name AS fullName, u.phone, u.email,
              u.status, r.role_name AS roleName
       FROM users u
       JOIN roles r ON r.role_id = u.role_id
       ORDER BY u.user_id DESC`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// PUT /api/admin/users/:id/status -> Khoá / Mở khoá tài khoản
router.put('/users/:id/status', async (req, res) => {
  try {
    const { status } = req.body; // 'active' hoặc 'locked'
    if (!['active', 'locked'].includes(status)) {
      return res.status(400).json({ error: 'Trạng thái không hợp lệ' });
    }
    await pool.query('UPDATE users SET status = ? WHERE user_id = ?', [status, req.params.id]);
    res.json({ message: 'Cập nhật trạng thái thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// DELETE /api/admin/users/:id -> Xoá người dùng
router.delete('/users/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM users WHERE user_id = ?', [req.params.id]);
    res.json({ message: 'Xoá người dùng thành công' });
  } catch (err) {
    console.error(err);
    // Nếu người dùng này còn dữ liệu liên quan (thẻ, thanh toán...), MySQL sẽ chặn xoá vì ràng buộc khoá ngoại
    res.status(500).json({ error: 'Không thể xoá (người dùng này còn dữ liệu liên quan)' });
  }
});

/* ================= GÓI TẬP ================= */

router.get('/packages', async (req, res) => {
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

router.post('/packages', async (req, res) => {
  try {
    const { name, durationDays, price } = req.body;
    if (!name || !durationDays || !price) {
      return res.status(400).json({ error: 'Thiếu thông tin' });
    }
    await pool.query(
      'INSERT INTO membership_packages (name, duration_days, price) VALUES (?, ?, ?)',
      [name, durationDays, price]
    );
    res.status(201).json({ message: 'Thêm gói tập thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.put('/packages/:id', async (req, res) => {
  try {
    const { name, durationDays, price } = req.body;
    await pool.query(
      'UPDATE membership_packages SET name = ?, duration_days = ?, price = ? WHERE package_id = ?',
      [name, durationDays, price, req.params.id]
    );
    res.json({ message: 'Cập nhật gói tập thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.delete('/packages/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM membership_packages WHERE package_id = ?', [req.params.id]);
    res.json({ message: 'Xoá gói tập thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Không thể xoá (gói này đang có hội viên sử dụng)' });
  }
});

/* ================= HUẤN LUYỆN VIÊN ================= */

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

router.post('/trainers', async (req, res) => {
  try {
    const { fullName, phone, specialty, pricePerSession } = req.body;
    if (!fullName) return res.status(400).json({ error: 'Thiếu tên HLV' });
    await pool.query(
      'INSERT INTO pt_trainers (full_name, phone, specialty, price_per_session) VALUES (?, ?, ?, ?)',
      [fullName, phone || null, specialty || null, pricePerSession || 100000]
    );
    res.status(201).json({ message: 'Thêm HLV thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.put('/trainers/:id', async (req, res) => {
  try {
    const { fullName, phone, specialty, pricePerSession } = req.body;
    await pool.query(
      'UPDATE pt_trainers SET full_name = ?, phone = ?, specialty = ?, price_per_session = ? WHERE trainer_id = ?',
      [fullName, phone, specialty, pricePerSession, req.params.id]
    );
    res.json({ message: 'Cập nhật HLV thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.delete('/trainers/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM pt_trainers WHERE trainer_id = ?', [req.params.id]);
    res.json({ message: 'Xoá HLV thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Không thể xoá (HLV này đang có lịch hẹn)' });
  }
});

/* ================= SẢN PHẨM (QUẦY BAR) ================= */

router.get('/products', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT product_id AS productId, name, price, stock FROM products');
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.post('/products', async (req, res) => {
  try {
    const { name, price, stock } = req.body;
    if (!name || !price) return res.status(400).json({ error: 'Thiếu thông tin' });
    await pool.query(
      'INSERT INTO products (name, price, stock) VALUES (?, ?, ?)',
      [name, price, stock || 0]
    );
    res.status(201).json({ message: 'Thêm sản phẩm thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.put('/products/:id', async (req, res) => {
  try {
    const { name, price, stock } = req.body;
    await pool.query(
      'UPDATE products SET name = ?, price = ?, stock = ? WHERE product_id = ?',
      [name, price, stock, req.params.id]
    );
    res.json({ message: 'Cập nhật sản phẩm thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.delete('/products/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM products WHERE product_id = ?', [req.params.id]);
    res.json({ message: 'Xoá sản phẩm thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Không thể xoá sản phẩm' });
  }
});

/* ================= LỚP HỌC ================= */

router.get('/classes', async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT class_id AS classId, name, description, capacity FROM group_classes'
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.post('/classes', async (req, res) => {
  try {
    const { name, description, capacity } = req.body;
    if (!name) return res.status(400).json({ error: 'Thiếu tên lớp' });
    await pool.query(
      'INSERT INTO group_classes (name, description, capacity) VALUES (?, ?, ?)',
      [name, description || null, capacity || 20]
    );
    res.status(201).json({ message: 'Thêm lớp học thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.put('/classes/:id', async (req, res) => {
  try {
    const { name, description, capacity } = req.body;
    await pool.query(
      'UPDATE group_classes SET name = ?, description = ?, capacity = ? WHERE class_id = ?',
      [name, description, capacity, req.params.id]
    );
    res.json({ message: 'Cập nhật lớp học thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.delete('/classes/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM group_classes WHERE class_id = ?', [req.params.id]);
    res.json({ message: 'Xoá lớp học thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Không thể xoá (lớp này đang có lịch học)' });
  }
});

/* ================= LỊCH BUỔI HỌC (của từng lớp) ================= */

// GET /api/admin/classes/:classId/schedules -> Xem các buổi học đã tạo cho 1 lớp
router.get('/classes/:classId/schedules', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT schedule_id AS scheduleId, start_time AS startTime, end_time AS endTime, room, status
       FROM class_schedules
       WHERE class_id = ?
       ORDER BY start_time ASC`,
      [req.params.classId]
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/admin/classes/:classId/schedules -> Thêm 1 buổi học mới cho lớp
router.post('/classes/:classId/schedules', async (req, res) => {
  try {
    const { startTime, endTime, room } = req.body;
    if (!startTime || !endTime) {
      return res.status(400).json({ error: 'Thiếu thời gian bắt đầu/kết thúc' });
    }
    await pool.query(
      `INSERT INTO class_schedules (class_id, start_time, end_time, room, status)
       VALUES (?, ?, ?, ?, 'open')`,
      [req.params.classId, startTime, endTime, room || null]
    );
    res.status(201).json({ message: 'Thêm buổi học thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// DELETE /api/admin/schedules/:id -> Xoá 1 buổi học
router.delete('/schedules/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM class_schedules WHERE schedule_id = ?', [req.params.id]);
    res.json({ message: 'Xoá buổi học thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Không thể xoá (buổi học này đã có người đặt lịch)' });
  }
});

/* ================= DUYỆT LỊCH PT ================= */

// GET /api/admin/pt-sessions -> Xem tất cả buổi PT (ưu tiên pending trước)
router.get('/pt-sessions', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT s.session_id AS sessionId, u.full_name AS memberName,
              t.full_name AS trainerName, s.scheduled_time AS scheduledTime,
              s.status, s.note
       FROM pt_sessions s
       JOIN members m ON m.member_id = s.member_id
       JOIN users u ON u.user_id = m.user_id
       JOIN pt_trainers t ON t.trainer_id = s.trainer_id
       ORDER BY FIELD(s.status, 'pending', 'confirmed', 'completed', 'rejected', 'cancelled'), s.scheduled_time ASC`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// PUT /api/admin/pt-sessions/:id/status -> Duyệt (confirmed) / Từ chối (rejected) / Hoàn thành (completed)
router.put('/pt-sessions/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    if (!['confirmed', 'rejected', 'completed'].includes(status)) {
      return res.status(400).json({ error: 'Trạng thái không hợp lệ' });
    }
    await pool.query('UPDATE pt_sessions SET status = ? WHERE session_id = ?', [status, req.params.id]);
    res.json({ message: 'Cập nhật trạng thái thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

/* ================= GIAO DỊCH VÍ (toàn hệ thống) ================= */

// GET /api/admin/transactions -> Xem tất cả giao dịch ví của mọi hội viên
router.get('/transactions', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT t.tx_id AS txId, u.full_name AS memberName, t.amount, t.type, t.note,
              t.created_at AS createdAt
       FROM wallet_transactions t
       JOIN wallets w ON w.wallet_id = t.wallet_id
       JOIN members m ON m.member_id = w.member_id
       JOIN users u ON u.user_id = m.user_id
       ORDER BY t.tx_id DESC
       LIMIT 100`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

/* ================= CHECK-IN / CHECK-OUT (toàn hệ thống) ================= */

// GET /api/admin/checkin-logs -> Xem lịch sử check-in/out của mọi hội viên
router.get('/checkin-logs', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT l.log_id AS logId, u.full_name AS memberName,
              l.checkin_time AS checkinTime, l.checkout_time AS checkoutTime
       FROM checkin_logs l
       JOIN members m ON m.member_id = l.member_id
       JOIN users u ON u.user_id = m.user_id
       ORDER BY l.log_id DESC
       LIMIT 100`
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;