const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT package_id AS packageId, name, duration_days AS durationDays, price, description
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

        // Kiểm tra đã có gói đang active và chưa hết hạn chưa
        const [activeCards] = await connection.query(
          `SELECT card_id, expiry_date FROM membership_cards
           WHERE member_id = ? AND status = 'active' AND expiry_date >= CURDATE()
           ORDER BY expiry_date DESC LIMIT 1`,
          [memberId]
        );
        if (activeCards.length > 0) {
          await connection.rollback();
          connection.release();
          const expiryDate = activeCards[0].expiry_date.toISOString().split('T')[0];
          return res.status(400).json({
            error: `Bạn đang có gói tập còn hiệu lực đến ${expiryDate}. Vui lòng đợi hết hạn mới có thể mua gói mới.`
          });
        }

    // Lấy ví (khoá dòng lại để tránh 2 giao dịch cùng lúc)
    let [walletRows] = await connection.query(
      'SELECT * FROM wallets WHERE member_id = ? FOR UPDATE', [memberId]
    );
    let wallet;
    if (walletRows.length === 0) {
      const [result] = await connection.query(
        'INSERT INTO wallets (member_id, balance) VALUES (?, 0)', [memberId]
      );
      wallet = { wallet_id: result.insertId, balance: 0 };
    } else {
      wallet = walletRows[0];
    }

    // Kiểm tra đủ tiền không
    if (Number(wallet.balance) < Number(pkg.price)) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Số dư ví không đủ, vui lòng nạp thêm tiền' });
    }

    // Trừ tiền ví
    await connection.query(
      'UPDATE wallets SET balance = balance - ? WHERE wallet_id = ?',
      [pkg.price, wallet.wallet_id]
    );
    await connection.query(
      'INSERT INTO wallet_transactions (wallet_id, amount, type, note) VALUES (?, ?, "purchase", ?)',
      [wallet.wallet_id, pkg.price, `Mua gói ${pkg.name}`]
    );

    // Tạo thẻ mới
    const cardCode = 'CARD-' + Date.now();
    const [cardResult] = await connection.query(
      `INSERT INTO membership_cards
         (member_id, package_id, card_code, activation_date, expiry_date, status)
       VALUES (?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY), 'active')`,
      [memberId, packageId, cardCode, pkg.duration_days]
    );
    const cardId = cardResult.insertId;

    // Ghi nhận thanh toán bằng ví
    await connection.query(
      `INSERT INTO payments (member_id, card_id, amount, method, status)
       VALUES (?, ?, ?, 'wallet', 'success')`,
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

// POST /api/packages/renew -> Gia hạn gói đang dùng (chỉ khi còn <= 7 ngày)
router.post('/renew', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;

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

    // Lấy thẻ active mới nhất
   const [cardRows] = await connection.query(
         `SELECT c.*, p.duration_days, p.price
          FROM membership_cards c
          JOIN membership_packages p ON p.package_id = c.package_id
          WHERE c.member_id = ? AND c.status = 'active'
          ORDER BY c.card_id DESC LIMIT 1 FOR UPDATE`,
         [memberId]
       );
    if (cardRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Bạn chưa có gói tập nào để gia hạn' });
    }
    const card = cardRows[0];

    // Kiểm tra còn <= 7 ngày mới cho gia hạn
    const [daysCheck] = await connection.query(
      'SELECT DATEDIFF(?, CURDATE()) AS daysLeft', [card.expiry_date]
    );
    const daysLeft = daysCheck[0].daysLeft;
    if (daysLeft > 7) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: `Chỉ có thể gia hạn khi còn tối đa 7 ngày (hiện còn ${daysLeft} ngày)` });
    }

    // Lấy ví, kiểm tra đủ tiền
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

    if (Number(wallet.balance) < Number(card.price)) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Số dư ví không đủ để gia hạn' });
    }

    // Trừ tiền ví
    await connection.query('UPDATE wallets SET balance = balance - ? WHERE wallet_id = ?', [card.price, wallet.wallet_id]);
    await connection.query(
      'INSERT INTO wallet_transactions (wallet_id, amount, type, note) VALUES (?, ?, "purchase", "Gia hạn gói tập")',
      [wallet.wallet_id, card.price]
    );

    // Gia hạn: cộng thêm duration_days vào ngày hết hạn hiện tại (không phải từ hôm nay)
    await connection.query(
      `UPDATE membership_cards
       SET expiry_date = DATE_ADD(expiry_date, INTERVAL ? DAY)
       WHERE card_id = ?`,
      [card.duration_days, card.card_id]
    );

    // Ghi nhận thanh toán
    await connection.query(
      `INSERT INTO payments (member_id, card_id, amount, method, status)
       VALUES (?, ?, ?, 'wallet', 'success')`,
      [memberId, card.card_id, card.price]
    );

    await connection.commit();
    connection.release();

    res.json({ message: 'Gia hạn gói tập thành công' });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;