// frontend/src/components/RatingModal.jsx

import React, { useState } from 'react';
import '../styles/RatingModal.css';

/**
 * 평점 입력 모달 컴포넌트
 *
 * Props:
 * - productName: 상품명
 * - onSubmit: 제출 콜백 함수 (score, review)
 * - onClose: 닫기 콜백 함수
 */
export default function RatingModal({ productName, onSubmit, onClose }) {
  const [score, setScore] = useState(0);
  const [hoverScore, setHoverScore] = useState(0);
  const [review, setReview] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    if (score === 0) {
      setError('평점을 선택해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await onSubmit(score, review);
    } catch (err) {
      setError('평점 저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} onKeyDown={handleKeyDown}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {/* 제목 */}
        <h2 className="modal-title">
          평점을 남겨주세요
        </h2>
        <p className="modal-subtitle">
          {productName}
        </p>

        {/* 별점 선택 */}
        <div className="star-rating">
          {[1, 2, 3, 4, 5].map(star => (
            <span
              key={star}
              className={`star ${star <= (hoverScore || score) ? 'active' : ''}`}
              onMouseEnter={() => setHoverScore(star)}
              onMouseLeave={() => setHoverScore(0)}
              onClick={() => setScore(star)}
            >
              ⭐
            </span>
          ))}
          <span className="score-display">
            {score === 0 ? '선택해주세요' : `${score}점`}
          </span>
        </div>

        {/* 리뷰 입력 */}
        <textarea
          placeholder="리뷰를 입력해주세요 (선택사항)"
          value={review}
          onChange={(e) => setReview(e.target.value)}
          maxLength="500"
          className="review-textarea"
        />
        <p className="review-count">
          {review.length}/500
        </p>

        {/* 에러 메시지 */}
        {error && (
          <div className="error-message">{error}</div>
        )}

        {/* 버튼 */}
        <div className="modal-actions">
          <button
            className="btn btn-secondary"
            onClick={onClose}
            disabled={loading}
          >
            취소
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={loading || score === 0}
          >
            {loading ? '저장 중...' : '평점 저장'}
          </button>
        </div>
      </div>
    </div>
  );
}