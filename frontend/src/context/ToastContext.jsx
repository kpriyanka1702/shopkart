import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';

const ToastContext = createContext(null);

const AUTO_DISMISS_MS = 5000;

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null); // { id, message, type }
  const timerRef = useRef(null);

  const hideToast = useCallback(() => {
    setToast(null);
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const showToast = useCallback((message, type = 'success') => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setToast({ id: Date.now(), message, type });
    timerRef.current = setTimeout(() => setToast(null), AUTO_DISMISS_MS);
  }, []);

  useEffect(() => () => timerRef.current && clearTimeout(timerRef.current), []);

  return (
    <ToastContext.Provider value={{ showToast, hideToast }}>
      {children}
      {toast && (
        <div className={`snackbar snackbar-${toast.type}`} role="status" aria-live="polite">
          <span className="snackbar-msg">{toast.message}</span>
          <button className="snackbar-close" onClick={hideToast} aria-label="Dismiss">×</button>
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}
