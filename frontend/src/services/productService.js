import api from './api';

export const productService = {
  // 전체 상품 목록 가져오기
  getProducts: () => api.get('/products'),
  // 상품 상세 정보 가져오기
  getProductById: (id) => api.get(`/products/${id}`),
};