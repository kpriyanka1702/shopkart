import { useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import api from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';

const TEST_HINTS = {
  CARD: [
    { label: 'Success card', value: '4111 1111 1111 1111' },
    { label: 'Decline card', value: '4000 0000 0000 0002' },
  ],
  UPI: [
    { label: 'Success UPI', value: 'success@upi' },
    { label: 'Failure UPI', value: 'failure@upi' },
  ],
  NETBANKING: [
    { label: 'HDFC (success)', value: 'HDFC' },
    { label: 'Force fail', value: 'FAIL' },
  ],
};

const PLACEHOLDER = {
  CARD: 'Card number',
  UPI: 'yourname@upi',
  NETBANKING: 'Bank code',
};

export default function Payment() {
  const { orderId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const intent = location.state;
  const [method, setMethod] = useState('CARD');
  const [instrument, setInstrument] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [processing, setProcessing] = useState(false);

  const amount = useMemo(() => (intent ? Number(intent.amount) : null), [intent]);

  if (!intent) {
    return (
      <div className="pay-wrap">
        <div className="banner error">Payment session expired. Please checkout again.</div>
        <button className="btn" onClick={() => navigate('/cart')}>Back to Cart</button>
      </div>
    );
  }

  const pay = async () => {
    setProcessing(true);
    try {
      const { data } = await api.post('/payments/pay', {
        orderId: Number(orderId),
        method,
        instrument: instrument.trim(),
      });
      if (data.status === 'SUCCESS') {
        navigate('/orders', {
          state: { paid: true, paymentId: data.paymentId, orderId: Number(orderId) },
        });
      } else {
        showToast(data.message || 'Payment failed. Try a success test instrument.', 'error');
      }
    } catch (err) {
      showToast(err.response?.data?.message || 'Payment failed. Please try again.', 'error');
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="pay-wrap">
      <div className="test-mode-banner">
        TEST MODE — simulated gateway. No real money is charged.
      </div>

      <div className="pay-card">
        <div className="pay-header">
          <div>
            <div className="pay-brand">ShopKart Secure Checkout</div>
            <div className="pay-order">Order #{orderId} · {intent.gatewayOrderId}</div>
          </div>
          <div className="pay-amount">₹{amount.toFixed(2)}</div>
        </div>

        <div className="pay-methods">
          {(intent.supportedMethods || ['CARD', 'UPI', 'NETBANKING']).map((m) => (
            <button
              key={m}
              className={`pay-tab ${method === m ? 'active' : ''}`}
              onClick={() => { setMethod(m); setInstrument(''); setError(''); }}
            >
              {m === 'NETBANKING' ? 'Net Banking' : m}
            </button>
          ))}
        </div>

        <div className="pay-body">
          <label className="pay-label">{PLACEHOLDER[method]}</label>
          <input
            className="pay-input"
            placeholder={PLACEHOLDER[method]}
            value={instrument}
            onChange={(e) => setInstrument(e.target.value)}
          />

          {method === 'CARD' && (
            <div className="pay-row">
              <input className="pay-input" placeholder="MM/YY" value={expiry}
                     onChange={(e) => setExpiry(e.target.value)} />
              <input className="pay-input" placeholder="CVV" value={cvv}
                     onChange={(e) => setCvv(e.target.value)} />
            </div>
          )}

          <div className="pay-hints">
            <span>Test values:</span>
            {TEST_HINTS[method].map((h) => (
              <button key={h.value} className="hint-chip" onClick={() => setInstrument(h.value)}>
                {h.label}: {h.value}
              </button>
            ))}
          </div>

          <button className="btn btn-lg pay-btn" onClick={pay} disabled={processing || !instrument.trim()}>
            {processing ? 'Processing…' : `Pay ₹${amount.toFixed(2)}`}
          </button>
          <button className="btn-link" onClick={() => navigate('/cart')} disabled={processing}>
            Cancel and return to cart
          </button>
        </div>
      </div>
    </div>
  );
}
