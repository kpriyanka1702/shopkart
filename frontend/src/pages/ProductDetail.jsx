import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { discountPct } from '../components/ProductCard.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [qty, setQty] = useState(1);
  const { user } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    api.get(`/products/${id}`).then(({ data }) => setProduct(data));
  }, [id]);

  const handleAdd = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    try {
      await api.post('/cart', { productId: product.id, quantity: Number(qty) });
      showToast(`Added "${product.name}" to cart`);
    } catch (err) {
      showToast('Could not add to cart. Please try again.', 'error');
    }
  };

  if (!product) return <p>Loading...</p>;

  return (
    <div className="detail">
      <img src={product.imageUrl} alt={product.name} className="detail-img" />
      <div className="detail-info">
        <h1>{product.name}</h1>
        <p className="card-category">{product.category}</p>
        <p className="detail-desc">{product.description}</p>
        <div className="price-row detail-price">
          <span className="card-price">₹{Number(product.price).toFixed(0)}</span>
          {discountPct(product.price, product.mrp) > 0 && (
            <>
              <span className="mrp">M.R.P ₹{Number(product.mrp).toFixed(0)}</span>
              <span className="off-text">{discountPct(product.price, product.mrp)}% off</span>
            </>
          )}
        </div>
        <p className={product.stock > 0 ? 'in-stock' : 'out-stock'}>
          {product.stock > 0 ? `In stock: ${product.stock}` : 'Out of stock'}
        </p>
        <div className="qty-row">
          <input
            type="number"
            min="1"
            max={product.stock}
            value={qty}
            onChange={(e) => setQty(e.target.value)}
          />
          <button className="btn" onClick={handleAdd} disabled={product.stock <= 0}>
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
}
