import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';

export default function Cart() {
  const [cart, setCart] = useState({ items: [], total: 0 });
  const { showToast } = useToast();
  const navigate = useNavigate();

  const load = async () => {
    const { data } = await api.get('/cart');
    setCart(data);
  };

  useEffect(() => {
    load();
  }, []);

  const updateQty = async (productId, quantity) => {
    const { data } = await api.put(`/cart/${productId}`, null, { params: { quantity } });
    setCart(data);
  };

  const remove = async (productId) => {
    const { data } = await api.delete(`/cart/${productId}`);
    setCart(data);
    showToast('Item removed from cart');
  };

  const checkout = async () => {
    try {
      const { data } = await api.post('/orders/checkout');
      navigate(`/payment/${data.orderId}`, { state: data });
    } catch (err) {
      showToast(err.response?.data?.message || 'Unable to start checkout', 'error');
    }
  };

  return (
    <div>
      <h1>Your Cart</h1>
      {cart.items.length === 0 ? (
        <p>Your cart is empty.</p>
      ) : (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Price</th>
                <th>Qty</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {cart.items.map((item) => (
                <tr key={item.id}>
                  <td className="cart-product">
                    <img src={item.imageUrl} alt={item.productName} />
                    {item.productName}
                  </td>
                  <td>₹{Number(item.price).toFixed(2)}</td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      value={item.quantity}
                      onChange={(e) => updateQty(item.productId, Number(e.target.value))}
                      className="qty-input"
                    />
                  </td>
                  <td>₹{Number(item.lineTotal).toFixed(2)}</td>
                  <td>
                    <button className="btn-link" onClick={() => remove(item.productId)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="cart-footer">
            <h2>Total: ₹{Number(cart.total).toFixed(2)}</h2>
            <button className="btn btn-lg" onClick={checkout}>Proceed to Pay</button>
          </div>
        </>
      )}
    </div>
  );
}
