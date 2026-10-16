// frontend/src/components/ProductCard.jsx

import React, { useState } from 'react';
import '../styles/ProductCard.css';
import RatingModal from './RatingModal';

/**
 * 개별 상품을 카드 형태로 표시하는 컴포넌트
 *
 * "리뷰 쓰기"는 RatingModal에서 실제 리뷰 API(주문 단위)로 저장해요.
 * 예전 onRate 콜백은 받는 백엔드 API가 없어 저장된 척만 했어요 (TS-14).
 *
 * Props:
 * - product: 상품 객체
 * - onReviewed: 리뷰 저장 후 콜백 (평점·추천이 바뀌므로 부모가 다시 불러오기)
 */
export default function ProductCard({ product, onReviewed }) {
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [message, setMessage] = useState(null);

  const handleOpenRatingModal = () => {
    setMessage(null);
    setShowRatingModal(true);
  };

  const handleCloseRatingModal = () => {
    setShowRatingModal(false);
  };

  const handleReviewed = (savedReview) => {
    setShowRatingModal(false);
    setMessage('리뷰가 등록되었습니다.');
    onReviewed?.(savedReview);
  };

  return (
    <div className="product-card">
      {/* 이미지 */}
      <div className="product-image-wrapper">
        <img
          src={product.imageUrl || '/placeholder.jpg'}
          alt={product.name}
          className="product-image"
        />
        {product.averageRating >= 4.5 && (
          <div className="badge-top-rated">⭐ 인기상품</div>
        )}
      </div>

      {/* 상품 정보 */}
      <div className="product-info">
        {/* 카테고리 */}
        <span className="product-category">{product.category || '기타'}</span>

        {/* 상품명 */}
        <h3 className="product-name">{product.name}</h3>

        {/* 가격 */}
        <div className="product-price">
          ₩{product.price?.toLocaleString()}
        </div>

        {/* 설명 (3줄 제한) */}
        <p className="product-description">
          {product.description?.substring(0, 100)}...
        </p>

        {/* 평점 정보 */}
        <div className="product-rating">
          <div className="rating-stars">
            {'⭐'.repeat(Math.round(product.averageRating))}
            <span className="rating-value">
              {product.averageRating?.toFixed(1)}
              <span className="rating-count">
                ({product.ratingCount || 0})
              </span>
            </span>
          </div>
        </div>

        {/* 버튼 */}
        <div className="product-actions">
          <button
            className="btn btn-primary"
            onClick={handleOpenRatingModal}
          >
            ⭐ 리뷰 쓰기
          </button>
        </div>
        {message && <p className="product-review-message" role="status">{message}</p>}
      </div>

      {/* 리뷰 작성 모달 (구매한 주문 선택 → POST /api/reviews) */}
      {showRatingModal && (
        <RatingModal
          product={product}
          onReviewed={handleReviewed}
          onClose={handleCloseRatingModal}
        />
      )}
    </div>
  );
}