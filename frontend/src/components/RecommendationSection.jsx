// frontend/src/components/RecommendationSection.jsx

import React, { useEffect, useState } from 'react';
import ProductCard from './ProductCard';
import api from '../services/api';
import '../styles/RecommendationSection.css';

/**
 * 추천 상품 섹션을 표시하는 컴포넌트
 *
 * 리뷰가 저장되면 추천 캐시가 비워지므로(TS-08) 추천을 다시 불러와요.
 */
export default function RecommendationSection() {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchRecommendations();
  }, []);

  const fetchRecommendations = async () => {
    setLoading(true);
    setError(null);

    try {
      // 성능 측정 (첫 요청 vs 캐시 히트)
      const startTime = performance.now();

      const response = await api.get('/api/recommendations?topN=5');

      const endTime = performance.now();
      const duration = (endTime - startTime).toFixed(2);

      console.log(`⏱️ 추천 조회 시간: ${duration}ms`);

      // 응답은 RecommendationResponse { products: [...] } 형태라 배열은 products에 있어요.
      // 예전처럼 response.data를 그대로 넣으면 배열이 아니어서 목록이 아무것도 그려지지 않았어요 (TS-14)
      setRecommendations(response.data?.products ?? []);
    } catch (err) {
      console.error('❌ 추천 조회 실패:', err);
      setError('추천 상품을 불러올 수 없습니다.');
      // 실패해도 페이지는 표시 (Graceful Degradation)
      setRecommendations([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="recommendation-section">
      <div className="section-header">
        <h2>🎯 당신을 위한 추천</h2>
        <p className="section-subtitle">
          당신의 취향과 비슷한 사용자들이 선호하는 상품입니다
        </p>
      </div>

      {loading && (
        <div className="loading">
          <div className="spinner"></div>
          <p>추천 상품을 찾고 있습니다...</p>
        </div>
      )}

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {!loading && recommendations.length === 0 && !error && (
        <div className="empty-state">
          <p>아직 추천할 상품이 없습니다.</p>
          <p className="empty-hint">구매한 상품에 리뷰를 남기면 추천이 시작됩니다!</p>
        </div>
      )}

      {!loading && recommendations.length > 0 && (
        <div className="product-grid">
          {recommendations.map(product => (
            <ProductCard
              key={product.id}
              product={product}
              onReviewed={fetchRecommendations}
            />
          ))}
        </div>
      )}
    </section>
  );
}