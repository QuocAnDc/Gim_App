const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db');

const router = express.Router();

// Đăng ký hội viên mới
router.post('/register', async (req, res) => {
  try {
    const { fullName, phone, email, password } = req.body;
    if (!fullName || !phone || !password) {
      return res.status(400).json({ error: 'Thiếu thông tin bắt buộc' });
    }

    const [existing] = await pool.query('SELECT user_id FROM users WHERE phone = ?', [phone]);
    if (existing.length > 0) {
      return res.status(409).json({ error: 'Số điện thoại đã được đăng ký' });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const [result] = await pool.query(
          'INSERT INTO users (full_name, phone, email, password_hash, role_id) VALUES (?, ?, ?, ?, 3)',
          [fullName, phone, email || null, passwordHash]
        );
        const userId = result.insertId;

        // Tạo hồ sơ hội viên gắn với user này
        const [memberResult] = await pool.query('INSERT INTO members (user_id) VALUES (?)', [userId]);
        const memberId = memberResult.insertId;

        // Sinh mã hội viên riêng, dạng HV00001, HV00002...
        const memberCode = 'HV' + String(memberId).padStart(5, '0');
        await pool.query('UPDATE members SET member_code = ? WHERE member_id = ?', [memberCode, memberId]);

    res.status(201).json({ message: 'Đăng ký thành công', userId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// Đăng nhập
router.post('/login', async (req, res) => {
  try {
    const { phone, password } = req.body;

    const [rows] = await pool.query(
      `SELECT u.user_id, u.full_name, u.password_hash, u.role_id, r.role_name, m.member_id
       FROM users u
       JOIN roles r ON u.role_id = r.role_id
       LEFT JOIN members m ON m.user_id = u.user_id
       WHERE u.phone = ? AND u.status = 'active'`,
      [phone]
    );

    if (rows.length === 0) {
      return res.status(401).json({ error: 'Sai số điện thoại hoặc mật khẩu' });
    }

    const user = rows[0];
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Sai số điện thoại hoặc mật khẩu' });
    }

   const token = jwt.sign(
     { userId: user.user_id, roleId: user.role_id, memberId: user.member_id },
     process.env.JWT_SECRET,
     { expiresIn: '7d' }
   );

    res.json({
      token,
      user: { userId: user.user_id, fullName: user.full_name, role: user.role_name },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});
const { requireAuth } = require('../middleware/auth');

// PUT /api/auth/change-password -> Đổi mật khẩu (yêu cầu đăng nhập)
router.put('/change-password', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { oldPassword, newPassword } = req.body;

    if (!oldPassword || !newPassword) {
      return res.status(400).json({ error: 'Thiếu mật khẩu cũ hoặc mật khẩu mới' });
    }
    if (newPassword.length < 6) {
      return res.status(400).json({ error: 'Mật khẩu mới phải có ít nhất 6 ký tự' });
    }

    const [rows] = await pool.query('SELECT password_hash FROM users WHERE user_id = ?', [userId]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy tài khoản' });
    }

    const isMatch = await bcrypt.compare(oldPassword, rows[0].password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Mật khẩu cũ không đúng' });
    }

    const newHash = await bcrypt.hash(newPassword, 10);
    await pool.query('UPDATE users SET password_hash = ? WHERE user_id = ?', [newHash, userId]);

    res.json({ message: 'Đổi mật khẩu thành công' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;
