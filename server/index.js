require('dotenv').config();
const express = require('express');
const cors = require('cors');
const pool = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

// API test đầu tiên: lấy danh sách roles
app.get('/api/roles', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM roles');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = 3000;
app.listen(PORT, () => console.log(`API đang chạy tại http://localhost:${PORT}`));

const authRoutes = require('./routes/auth');   // thêm dòng này ở đầu file, sau các require khác
app.use('/api/auth', authRoutes);               // thêm dòng này, sau app.use(express.json())
const memberRoutes = require('./routes/members');   // thêm sau dòng require authRoutes
app.use('/api/members', memberRoutes);                // thêm sau app.use('/api/auth', authRoutes)
const packageRoutes = require('./routes/packages');
app.use('/api/packages', packageRoutes);
const checkinRoutes = require('./routes/checkin');
app.use('/api/checkin', checkinRoutes);
const classRoutes = require('./routes/classes');
app.use('/api/classes', classRoutes);
const ptRoutes = require('./routes/pt');
app.use('/api/pt', ptRoutes);
const walletRoutes = require('./routes/wallet');
app.use('/api/wallet', walletRoutes);
const reportRoutes = require('./routes/reports');
app.use('/api/reports', reportRoutes);
const adminRoutes = require('./routes/admin');
app.use('/api/admin', adminRoutes);