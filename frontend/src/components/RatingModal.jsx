// frontend/src/components/RatingModal.jsx

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { reviewService } from '../services/reviewService';
import '../styles/RatingModal.css';

/**
 * 구매한 주문에 리뷰(별점 + 내용)를 남기는 모달
 *
 * 예전에는 별점만 받아 부모의 onRate 콜백에 넘겼는데, 그 콜백을 받는 백엔드 API가 없어
 * "저장된 것처럼 보이지만 아무것도 저장되지 않는" UI였어요 (TS-14).
 * 이제는 실제 리뷰 API(POST /api/reviews)를 부르고, 백엔드 규칙을 화면에서도 그대로 따라요.
 * - 리뷰는 주문 단위: 이 상품을 산 내 주문을 골라야 해요(없으면 주문 내역으로 안내).
 * - 주문당 1건: 이미 리뷰를 쓴 주문은 목록에서 빼고, 모두 썼으면 리뷰 관리(수정)로 안내.
 * - 내용 필수(review.content NOT NULL), 별점 1~5.
 *
 * Props:
 * - product: { id, name } 리뷰를 쓸 상품
 * - onReviewed: 저장 성공 콜백 (저장된 리뷰)
 * - onClose: 닫기 콜백
 */
export default function RatingModal({ product, onReviewed, onClose }) {
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading'); // loading | ready | error
  const [orders, setOrders] = useState([]);
  const [reviewedCount, setReviewedCount] = useState(0);
  const [orderId, setOrderId] = useState(null);
  const [score, setScore] = useState(0);
  const [hoverScore, setHoverScore] = useState(0);
  const [review, setReview] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    reviewService
      .getReviewableOrders(product.id)
      .then((result) => {
        if (cancelled) return;
        setOrders(result.orders);
        setReviewedCount(result.reviewedCount);
        setOrderId(result.orders[0]?.id ?? null);
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    // 모달을 빨리 닫으면 늦게 온 응답이 상태를 바꾸지 않게 막아요.
    return () => {
      cancelled = true;
    };
  }, [product.id]);

  const handleSubmit = async () => {
    if (!orderId) {
      setError('리뷰를 남길 주문을 선택해주세요.');
      return;
    }
    if (score === 0) {
      setError('별점을 선택해주세요.');
      return;
    }
    if (!review.trim()) {
      setError('리뷰 내용을 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const res = await reviewService.createReview({
        productId: product.id,
        orderId,
        rating: score,
        content: review.trim(),
      });
      onReviewed?.(res.data);
    } catch (err) {
      // 403(남의 주문)·409(이미 리뷰 작성)·400(별점 범위)은 서버 메시지를 그대로 보여줘요.
      setError(err.response?.data?.message ?? '리뷰 저장에 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      onClose();
    }
  };

  const goTo = (path) => {
    onClose();
    navigate(path);
  };

  const renderBody = () => {
    if (status === 'loading') {
      return <p className="modal-notice">구매 내역을 확인하고 있습니다...</p>;
    }
    if (status === 'error') {
      return <div className="error-message">구매 내역을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</div>;
    }
    if (orders.length === 0) {
      return reviewedCount > 0 ? (
        <>
          <p className="modal-notice">이 상품의 모든 주문에 이미 리뷰를 남기셨어요. 리뷰 수정은 리뷰 관리에서 할 수 있어요.</p>
          <button className="btn btn-primary btn-block" onClick={() => goTo(`/review-management?productId=${product.id}`)}>
            리뷰 관리로 가기
          </button>
        </>
      ) : (
        <>
          <p className="modal-notice">리뷰는 이 상품을 구매한 주문에만 남길 수 있어요.</p>
          <button className="btn btn-primary btn-block" onClick={() => goTo('/orders')}>
            주문 내역 보기
          </button>
        </>
      );
    }

    return (
      <>
        {/* 주문 선택: 리뷰는 주문 단위로 1건 */}
        <label className="order-select-label" htmlFor="review-order">리뷰를 남길 주문</label>
        <select
          id="review-order"
          className="order-select"
          value={orderId ?? ''}
          onChange={(e) => setOrderId(Number(e.target.value))}
        >
          {orders.map((order) => (
            <option key={order.id} value={order.id}>
              주문 #{order.id} · {order.quantity}개
            </option>
          ))}
        </select>

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

        {/* 리뷰 입력 (DB 컬럼이 NOT NULL이라 필수) */}
        <textarea
          placeholder="리뷰를 입력해주세요"
          value={review}
          onChange={(e) => setReview(e.target.value)}
          maxLength="500"
          className="review-textarea"
        />
        <p className="review-count">
          {review.length}/500
        </p>
      </>
    );
  };

  const canSubmit = status === 'ready' && orders.length > 0;

  return (
    <div className="modal-overlay" onClick={onClose} onKeyDown={handleKeyDown}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {/* 제목 */}
        <h2 className="modal-title">
          리뷰를 남겨주세요
        </h2>
        <p className="modal-subtitle">
          {product.name}
        </p>

        {renderBody()}

        {/* 에러 메시지 */}
        {error && (
          <div className="error-message">{error}</div>
        )}

        {/* 버튼 */}
        <div className="modal-actions">
          <button
            className="btn btn-secondary"
            onClick={onClose}
            disabled={submitting}
          >
            {canSubmit ? '취소' : '닫기'}
          </button>
          {canSubmit && (
            <button
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={submitting || score === 0 || !review.trim()}
            >
              {submitting ? '저장 중...' : '리뷰 등록'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
