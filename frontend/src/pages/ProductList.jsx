import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client.js';
import ProductCard from '../components/ProductCard.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';

const CATEGORIES = ['All', 'Electronics', 'Fashion', 'Home', 'Sports', 'Books', 'Beauty'];

export default function ProductList() {
  const [products, setProducts] = useState([]);
  const [category, setCategory] = useState('All');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const load = async (signal) => {
    setLoading(true);
    try {
      const params = { size: 100 };
      // A typed query searches across ALL categories; otherwise filter by category.
      if (search.trim()) params.search = search.trim();
      else if (category !== 'All') params.category = category;
      const { data } = await api.get('/products', { params, signal });
      setProducts(data.content || []);
    } catch (err) {
      if (err.name !== 'CanceledError') setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  // Live search: debounce typing + category changes, cancel in-flight requests.
  useEffect(() => {
    const controller = new AbortController();
    const t = setTimeout(() => load(controller.signal), 250);
    return () => {
      clearTimeout(t);
      controller.abort();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [category, search]);

  const handleAdd = async (product) => {
    if (!user) {
      navigate('/login');
      return;
    }
    try {
      await api.post('/cart', { productId: product.id, quantity: 1 });
      showToast(`Added "${product.name}" to cart`);
    } catch (err) {
      showToast('Could not add to cart. Please try again.', 'error');
    }
  };

  return (
    <div>
      <div className="toolbar">
        <div className="categories">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              className={`chip ${category === c ? 'chip-active' : ''}`}
              onClick={() => setCategory(c)}
            >
              {c}
            </button>
          ))}
        </div>
        <form
          className="search"
          onSubmit={(e) => {
            e.preventDefault();
            load();
          }}
        >
          <input
            placeholder="Search products..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button className="btn" type="submit">Search</button>
        </form>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : (
        <div className="grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} onAdd={handleAdd} />
          ))}
          {products.length === 0 && <p>No products found.</p>}
        </div>
      )}
    </div>
  );
}
