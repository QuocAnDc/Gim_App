const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/reports/overview -> Tổng quan hệ thống (dành cho Admin)
router.get('/overview', requireAuth, async (req, res) => {
  try {
    // Chỉ role_id = 1 (ADMIN) mới được xem báo cáo
    if (req.user.roleId !== 1) {
      return res.status(403).json({ error: 'Bạn không có quyền xem báo cáo' });
    }

    const [[memberCount]] = await pool.query(
      `SELECT COUNT(*) AS total FROM members`
    );

    const [[activeCards]] = await pool.query(
      `SELECT COUNT(*) AS total FROM membership_cards WHERE status = 'active'`
    );

    const [[revenueToday]] = await pool.query(
      `SELECT COALESCE(SUM(amount), 0) AS total FROM payments
       WHERE status = 'success' AND DATE(created_at) = CURDATE()`
    );

    const [[revenueMonth]] = await pool.query(
      `SELECT COALESCE(SUM(amount), 0) AS total FROM payments
       WHERE status = 'success' AND MONTH(created_at) = MONTH(CURDATE())
             AND YEAR(created_at) = YEAR(CURDATE())`
    );

    const [[checkinsToday]] = await pool.query(
      `SELECT COUNT(*) AS total FROM checkin_logs WHERE DATE(checkin_time) = CURDATE()`
    );

    const [topPackages] = await pool.query(
      `SELECT p.name, COUNT(*) AS soldCount
       FROM membership_cards c
       JOIN membership_packages p ON p.package_id = c.package_id
       GROUP BY p.package_id
       ORDER BY soldCount DESC
       LIMIT 5`
    );

    res.json({
      totalMembers: memberCount.total,
      activeCards: activeCards.total,
      revenueToday: revenueToday.total,
      revenueThisMonth: revenueMonth.total,
      checkinsToday: checkinsToday.total,
      topPackages
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;