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
      `SELECT trainer_id AS trainerId, full_name AS fullName, phone, specialty, rating
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
    const { fullName, phone, specialty } = req.body;
    if (!fullName) return res.status(400).json({ error: 'Thiếu tên HLV' });
    await pool.query(
      'INSERT INTO pt_trainers (full_name, phone, specialty) VALUES (?, ?, ?)',
      [fullName, phone || null, specialty || null]
    );
    res.status(201).json({ message: 'Thêm HLV thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

router.put('/trainers/:id', async (req, res) => {
  try {
    const { fullName, phone, specialty } = req.body;
    await pool.query(
      'UPDATE pt_trainers SET full_name = ?, phone = ?, specialty = ? WHERE trainer_id = ?',
      [fullName, phone, specialty, req.params.id]
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

module.exports = router;