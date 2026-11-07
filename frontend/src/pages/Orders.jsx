import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import api from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';

const CANCELLABLE = ['PLACED', 'PENDING_PAYMENT', 'PAYMENT_FAILED'];

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const location = useLocation();
  const { showToast } = useToast();
  const paidShown = useRef(false);

  const load = () => api.get('/orders').then(({ data }) => setOrders(data));

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (location.state?.paid && !paidShown.current) {
      paidShown.current = true;
      showToast(
        `Payment successful! Order #${location.state.orderId} confirmed · Payment ID ${location.state.paymentId}`
      );
    }
  }, [location.state, showToast]);

  const cancelOrder = async (id) => {
    try {
      const { data } = await api.post(`/orders/${id}/cancel`);
      setOrders((prev) => prev.map((o) => (o.id === id ? data : o)));
      showToast(`Order #${id} cancelled`);
    } catch (err) {
      showToast(err.response?.data?.message || 'Unable to cancel order', 'error');
    }
  };

  return (
    <div>
      <h1>Your Orders</h1>
      {orders.length === 0 ? (
        <p>No orders yet.</p>
      ) : (
        orders.map((order) => (
          <div key={order.id} className="order-card">
            <div className="order-head">
              <span>Order #{order.id}</span>
              <span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span>
              <span>{new Date(order.createdAt).toLocaleString()}</span>
              <strong>₹{Number(order.total).toFixed(2)}</strong>
              {CANCELLABLE.includes(order.status) && (
                <button className="btn-cancel" onClick={() => cancelOrder(order.id)}>
                  Cancel Order
                </button>
              )}
            </div>
            <ul className="order-items">
              {order.items.map((item, idx) => (
                <li key={idx}>
                  {item.productName} × {item.quantity} — ₹{Number(item.price).toFixed(2)}
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </div>
  );
}
