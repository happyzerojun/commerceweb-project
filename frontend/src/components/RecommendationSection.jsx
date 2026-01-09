// frontend/src/components/RecommendationSection.jsx

import React, { useEffect, useState } from 'react';
import ProductCard from './ProductCard';
import api from '../services/api';
import '../styles/RecommendationSection.css';

/**
 * 추천 상품 섹션을 표시하는 컴포넌트
 *
 * Props:
 * - onRate: 평점 저장 콜백 함수
 */
export default function RecommendationSection({ onRate }) {
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

      setRecommendations(response.data);
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
          <p className="empty-hint">상품에 평점을 남기면 추천이 시작됩니다!</p>
        </div>
      )}

      {!loading && recommendations.length > 0 && (
        <div className="product-grid">
          {recommendations.map(product => (
            <ProductCard
              key={product.id}
              product={product}
              onRate={onRate}
            />
          ))}
        </div>
      )}
    </section>
  );
}