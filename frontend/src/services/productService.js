import api from './api';

export const productService = {
  /**
   * 전체 상품 목록 조회 (검색 및 카테고리 필터링 지원)
   * @param {Object} params - { category, name } 형태의 검색 조건
   * 예: getProducts({ category: '의류', name: '티셔츠' })
   */
  getProducts: (params = {}) => {
    // '전체' 카테고리일 경우 파라미터에서 제외하여 전체 목록을 불러오도록 처리
    const requestParams = { ...params };
    if (requestParams.category === '전체') {
      delete requestParams.category;
    }
    return api.get('/api/products', { params: requestParams });
  },

  /**
   * 상품 상세 정보 조회
   * @param {number} id - 상품 ID
   */
  getProductById: (id) => api.get(`/api/products/${id}`),

  /**
   * 인기 상품 조회 (평점 높은 순 등)
   */
  getPopularProducts: () => api.get('/api/products/trending/popular'),

  /**
   * 트렌딩 상품 조회 (요즘 뜨는 상품)
   */
  getTrendingProducts: () => api.get('/api/products/trending/trending'),

  /**
   * (선택 사항) 상품 등록 - 관리자용
   */
  createProduct: (productData) => api.post('/api/products', productData),
};
