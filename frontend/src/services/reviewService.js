import api from './api';

/**
 * 리뷰 작성 흐름에서 쓰는 API 모음.
 *
 * 백엔드 규칙(ReviewService.createReview)과 맞춰야 해요.
 * - 리뷰는 상품이 아니라 "내가 구매한 주문"에 달려요: orderId 필수, 주문 주인이 아니면 403.
 * - 주문당 리뷰 1건(uk_review_order): 이미 쓴 주문이면 409.
 * - 별점 1~5, 내용은 NOT NULL 컬럼이라 비워 보낼 수 없어요.
 * 예전 ProductCard/RatingModal은 orderId 없이 onRate 콜백만 불러 실제로는 저장되지 않았어요 (TS-14).
 */
export const reviewService = {
  /**
   * 이 상품을 산 내 주문 중 아직 리뷰를 쓰지 않은 주문을 돌려줘요.
   * 상품 리뷰 목록(공개 API)에서 내 주문 ID에 이미 달린 리뷰를 빼요.
   * 서버의 409가 최종 보장이고, 이 필터는 고를 수 없는 주문을 미리 숨기는 용도예요.
   * @returns {Promise<{ orders: Array, reviewedCount: number }>}
   */
  getReviewableOrders: async (productId) => {
    const [ordersRes, reviewsRes] = await Promise.all([
      api.get('/api/orders/my'),
      api.get(`/api/reviews/product/${productId}`),
    ]);
    const myOrders = (ordersRes.data ?? []).filter(
      (order) => Number(order.productId) === Number(productId)
    );
    const reviewedOrderIds = new Set((reviewsRes.data ?? []).map((review) => review.orderId));
    const orders = myOrders.filter((order) => !reviewedOrderIds.has(order.id));
    return { orders, reviewedCount: myOrders.length - orders.length };
  },

  /** 리뷰 작성. 작성자(userId)는 서버가 토큰에서 정하므로 보내지 않아요. */
  createReview: ({ productId, orderId, rating, content }) =>
    api.post('/api/reviews', { productId, orderId, rating, content }),
};
