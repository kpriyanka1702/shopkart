import { Link } from 'react-router-dom';

export function discountPct(price, mrp) {
  if (!mrp || Number(mrp) <= Number(price)) return 0;
  return Math.round(((Number(mrp) - Number(price)) / Number(mrp)) * 100);
}

export default function ProductCard({ product, onAdd }) {
  const off = discountPct(product.price, product.mrp);
  return (
    <div className="card">
      <Link to={`/products/${product.id}`}>
        <img src={product.imageUrl} alt={product.name} className="card-img" />
        {off > 0 && <span className="off-badge">{off}% OFF</span>}
      </Link>
      <div className="card-body">
        <Link to={`/products/${product.id}`} className="card-title">{product.name}</Link>
        <p className="card-category">{product.category}</p>
        <div className="price-row">
          <span className="card-price">₹{Number(product.price).toFixed(0)}</span>
          {off > 0 && <span className="mrp">₹{Number(product.mrp).toFixed(0)}</span>}
          {off > 0 && <span className="off-text">{off}% off</span>}
        </div>
        <button className="btn" onClick={() => onAdd(product)} disabled={product.stock <= 0}>
          {product.stock > 0 ? 'Add to Cart' : 'Out of Stock'}
        </button>
      </div>
    </div>
  );
}
