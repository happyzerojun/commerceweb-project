// frontend/src/components/ProductCard.jsx

import React, { useState } from 'react';
import '../styles/ProductCard.css';
import RatingModal from './RatingModal';

/**
 * 개별 상품을 카드 형태로 표시하는 컴포넌트
 *
 * Props:
 * - product: 상품 객체
 * - onRate: 평점 저장 콜백 함수
 */
export default function ProductCard({ product, onRate }) {
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleOpenRatingModal = () => {
    setShowRatingModal(true);
  };

  const handleCloseRatingModal = () => {
    setShowRatingModal(false);
  };

  const handleRateSubmit = async (score, review) => {
    setLoading(true);
    try {
      await onRate(product.id, score, review);
      setShowRatingModal(false);
      console.log(`✅ 평점 저장 완료: ${score}점`);
    } catch (error) {
      console.error('❌ 평점 저장 실패:', error);
    } finally {
      setLoading(false);
    }
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
            disabled={loading}
          >
            {loading ? '평점 저장 중...' : '⭐ 평점 작성'}
          </button>
        </div>
      </div>

      {/* 평점 모달 */}
      {showRatingModal && (
        <RatingModal
          productName={product.name}
          onSubmit={handleRateSubmit}
          onClose={handleCloseRatingModal}
        />
      )}
    </div>
  );
}