const {
  pool,
  findPlantByTrueId,
  findPlantUuidByTrueId,
  findOrderByUuid,
  listOrderItemsWithPlants,
  serializeOrder,
  findUserIdByTrueId
} = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const currentUser = getUserFromToken(req);
    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Convertir true_id en id INTEGER pour la table user
    const userId = await findUserIdByTrueId(currentUser.id);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    const { items } = req.body;

    if (!items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ message: 'Items array is required' });
    }

    // Validate items format
    for (const item of items) {
      if (!item.plantId || !item.quantity || item.quantity < 1) {
        return res.status(400).json({ message: 'Each item must have plantId and quantity >= 1' });
      }
    }

    // Create the order directement en SQL car Manifest ne gère pas userId correctement
    const { rows: orderRows } = await pool.query(
      'INSERT INTO "order" ("userId", status, "totalPrice", "createdAt", "updatedAt") VALUES ($1, $2, $3, NOW(), NOW()) RETURNING id',
      [userId, 'pending', 0]
    );
    const orderId = orderRows[0].id;

    let totalPrice = 0;
    const orderItems = [];

    // Create order items and calculate total
    for (const item of items) {
      const plantUuid = await findPlantUuidByTrueId(item.plantId);
      if (!plantUuid) {
        await pool.query('DELETE FROM "order" WHERE id = $1', [orderId]);
        return res.status(400).json({ message: `Plant with ID ${item.plantId} not found` });
      }

      const plant = await findPlantByTrueId(item.plantId);

      if (!plant) {
        // Rollback: delete the order
        await pool.query('DELETE FROM "order" WHERE id = $1', [orderId]);
        return res.status(400).json({ message: `Plant with ID ${item.plantId} not found` });
      }

      const plantPrice = Number(plant.price);
      const plantStock = Number(plant.stock);

      if (plantStock < item.quantity) {
        // Rollback: delete the order
        await pool.query('DELETE FROM "order" WHERE id = $1', [orderId]);
        return res.status(400).json({ message: `Insufficient stock for plant: ${plant.name}` });
      }

      // Create order item directement en SQL
      const { rows: itemRows } = await pool.query(
        'INSERT INTO "order_item" ("orderId", "plantId", quantity, "createdAt", "updatedAt") VALUES ($1, $2, $3, NOW(), NOW()) RETURNING id',
        [orderId, plantUuid, item.quantity]
      );

      // Update plant stock
      await pool.query(
        'UPDATE "plant" SET stock = $1, "updatedAt" = NOW() WHERE id = $2',
        [plantStock - item.quantity, plantUuid]
      );

      totalPrice += plantPrice * item.quantity;
      orderItems.push({ id: itemRows[0].id });
    }

    // Update order with total price
    await pool.query(
      'UPDATE "order" SET "totalPrice" = $1, "updatedAt" = NOW() WHERE id = $2',
      [totalPrice, orderId]
    );

    // Get complete order with items and plants via SQL helpers
    const orderRow = await findOrderByUuid(orderId);
    const itemsRows = await listOrderItemsWithPlants(orderId);
    const completeOrder = serializeOrder(orderRow, itemsRows);

    res.status(201).json(completeOrder);
  } catch (error) {
    console.error('Create order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
