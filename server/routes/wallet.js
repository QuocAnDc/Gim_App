const express = require('express');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// Hàm phụ: lấy hoặc tự tạo ví cho hội viên (nếu chưa có)
async function getOrCreateWallet(connection, memberId) {
  const [rows] = await connection.query(
    'SELECT * FROM wallets WHERE member_id = ? FOR UPDATE', [memberId]
  );
  if (rows.length > 0) return rows[0];

  const [result] = await connection.query(
    'INSERT INTO wallets (member_id, balance) VALUES (?, 0)', [memberId]
  );
  return { wallet_id: result.insertId, member_id: memberId, balance: 0 };
}

// GET /api/wallet -> Xem số dư ví
router.get('/', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const [memberRows] = await connection.query('SELECT member_id FROM members WHERE user_id = ?', [userId]);
    const memberId = memberRows[0].member_id;

    const wallet = await getOrCreateWallet(connection, memberId);
    connection.release();

    res.json({ balance: wallet.balance });
  } catch (err) {
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/wallet/topup -> Nạp tiền vào ví (giả lập, coi như nạp thành công ngay)
router.post('/topup', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { amount } = req.body;

    if (!amount || amount <= 0) {
      connection.release();
      return res.status(400).json({ error: 'Số tiền không hợp lệ' });
    }

    await connection.beginTransaction();

    const [memberRows] = await connection.query('SELECT member_id FROM members WHERE user_id = ?', [userId]);
    const memberId = memberRows[0].member_id;
    const wallet = await getOrCreateWallet(connection, memberId);

    await connection.query('UPDATE wallets SET balance = balance + ? WHERE wallet_id = ?', [amount, wallet.wallet_id]);
    await connection.query(
      'INSERT INTO wallet_transactions (wallet_id, amount, type, note) VALUES (?, ?, "topup", "Nạp tiền")',
      [wallet.wallet_id, amount]
    );

    await connection.commit();
    connection.release();
    res.json({ message: 'Nạp tiền thành công' });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// GET /api/wallet/products -> Danh sách sản phẩm quầy bar
router.get('/products', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT product_id AS productId, name, price, stock FROM products');
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// POST /api/wallet/buy -> Mua sản phẩm bằng số dư ví
router.post('/buy', requireAuth, async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const userId = req.user.userId;
    const { productId, quantity } = req.body;
    const qty = quantity || 1;

    if (!productId) {
      connection.release();
      return res.status(400).json({ error: 'Thiếu productId' });
    }

    await connection.beginTransaction();

    const [memberRows] = await connection.query('SELECT member_id FROM members WHERE user_id = ?', [userId]);
    const memberId = memberRows[0].member_id;
    const wallet = await getOrCreateWallet(connection, memberId);

    const [productRows] = await connection.query('SELECT * FROM products WHERE product_id = ? FOR UPDATE', [productId]);
    if (productRows.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'Sản phẩm không tồn tại' });
    }
    const product = productRows[0];
    const totalCost = product.price * qty;

    if (product.stock < qty) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Sản phẩm không đủ số lượng trong kho' });
    }
    if (wallet.balance < totalCost) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: 'Số dư ví không đủ, vui lòng nạp thêm tiền' });
    }

    await connection.query('UPDATE wallets SET balance = balance - ? WHERE wallet_id = ?', [totalCost, wallet.wallet_id]);
    await connection.query('UPDATE products SET stock = stock - ? WHERE product_id = ?', [qty, productId]);
    await connection.query(
      'INSERT INTO wallet_transactions (wallet_id, amount, type, note) VALUES (?, ?, "purchase", ?)',
      [wallet.wallet_id, totalCost, `Mua ${qty} x ${product.name}`]
    );

    await connection.commit();
    connection.release();
    res.status(201).json({ message: `Mua ${product.name} thành công`, totalCost });
  } catch (err) {
    await connection.rollback();
    connection.release();
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

// GET /api/wallet/transactions -> Lịch sử giao dịch ví
router.get('/transactions', requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;

    const [memberRows] = await pool.query('SELECT member_id FROM members WHERE user_id = ?', [userId]);
    if (memberRows.length === 0) {
      return res.json([]);
    }
    const memberId = memberRows[0].member_id;

    const [rows] = await pool.query(
      `SELECT t.tx_id AS txId, t.amount, t.type, t.note, t.created_at AS createdAt
       FROM wallet_transactions t
       JOIN wallets w ON w.wallet_id = t.wallet_id
       WHERE w.member_id = ?
       ORDER BY t.tx_id DESC
       LIMIT 50`,
      [memberId]
    );

    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Lỗi máy chủ' });
  }
});

module.exports = router;