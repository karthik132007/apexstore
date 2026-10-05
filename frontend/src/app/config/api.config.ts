export const API_BASE = 'http://localhost:8080';

export const API_ENDPOINTS = {
  auth: {
    login: `${API_BASE}/api/auth/login`,
    register: `${API_BASE}/api/auth/register`,
  },
  users: {
    me: `${API_BASE}/api/users/me`,
    becomeSeller: `${API_BASE}/api/users/me/seller`,
  },
  products: {
    publicList: `${API_BASE}/api/products`,
    publicDetail: (id: string) => `${API_BASE}/api/products/${id}`,
    sellerList: `${API_BASE}/api/seller/products`,
    sellerCreate: `${API_BASE}/api/seller/products`,
    sellerUpdate: (id: string) => `${API_BASE}/api/seller/products/${id}`,
    sellerStock: (id: string) => `${API_BASE}/api/seller/products/${id}/stock`,
    sellerDelete: (id: string) => `${API_BASE}/api/seller/products/${id}`,
    adminList: `${API_BASE}/api/admin/products`,
    adminCreate: `${API_BASE}/api/admin/products`,
    adminUpdate: (id: string) => `${API_BASE}/api/admin/products/${id}`,
    adminStock: (id: string) => `${API_BASE}/api/admin/products/${id}/stock`,
    adminDelete: (id: string) => `${API_BASE}/api/admin/products/${id}`,
  },
  orders: {
    checkout: `${API_BASE}/api/orders`,
    myOrders: `${API_BASE}/api/orders`,
    detail: (id: string) => `${API_BASE}/api/orders/${id}`,
    cancel: (id: string) => `${API_BASE}/api/orders/${id}/cancel`,
    generateInvoice: (id: string) => `${API_BASE}/api/orders/${id}/invoice`,
    getInvoice: (id: string) => `${API_BASE}/api/orders/${id}/invoice`,
  }
};
