function requireAdmin(req, res, next) {
  if (req.user.roleId !== 1) {
    return res.status(403).json({ error: 'Chỉ Admin mới có quyền thực hiện thao tác này' });
  }
  next();
}

module.exports = { requireAdmin };