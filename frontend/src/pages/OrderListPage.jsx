import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const OrderListPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const navigate = useNavigate();

  // ✅ 다크모드 감지
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    setIsDarkMode(mediaQuery.matches);

    const listener = (e) => setIsDarkMode(e.matches);
    mediaQuery.addEventListener('change', listener);

    return () => mediaQuery.removeEventListener('change', listener);
  }, []);

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = () => {
    api.get('/api/orders/my')
        .then(res => {
          setOrders(res.data);
          setLoading(false);
        })
        .catch(err => {
          console.error("주문 내역 로딩 실패:", err);
          setLoading(false);
        });
  };

  const totalAmount = orders.reduce((acc, cur) => acc + cur.totalPrice, 0);

  const handleCancel = async (orderId) => {
    if (!window.confirm("정말 주문을 취소하시겠습니까?")) return;
    try {
      await api.delete(`/api/orders/${orderId}`);
      alert("주문이 취소되었습니다.");
      setOrders(prevOrders => prevOrders.filter(order => order.id !== orderId));
    } catch (err) {
      console.error(err);
      alert("취소 처리 중 오류 발생");
    }
  };

  if (loading) {
    return (
        <div style={{
          textAlign: 'center',
          padding: '100px 20px',
          color: isDarkMode ? '#ccc' : '#666',
          backgroundColor: isDarkMode ? '#0f0f0f' : '#f8f9fa',
          minHeight: '100vh'
        }}>
          로딩 중...
        </div>
    );
  }

  // ✅ 다크모드 테마
  const theme = {
    bgPrimary: isDarkMode ? '#0f0f0f' : '#f8f9fa',
    bgCard: isDarkMode ? '#1a1a1a' : '#fff',
    textPrimary: isDarkMode ? '#ffffff' : '#333',
    textSecondary: isDarkMode ? '#999' : '#888',
    textTertiary: isDarkMode ? '#666' : '#999',
    accent: '#00c73c',
    accentLight: isDarkMode ? '#1a3a1a' : '#f1fcf4',
    border: isDarkMode ? '#2a2a2a' : '#f0f0f0',
    borderDashed: isDarkMode ? '#333' : '#eee',
    shadow: isDarkMode ? '0 4px 12px rgba(0,0,0,0.2)' : '0 4px 12px rgba(0,0,0,0.03)',
    errorColor: '#ff4d4f',
    errorBg: isDarkMode ? '#3a1a1a' : '#fff1f1'
  };

  return (
      <div style={{ backgroundColor: theme.bgPrimary, minHeight: '100vh', paddingBottom: '50px' }}>
        <div style={{ maxWidth: '600px', margin: '0 auto', padding: '40px 20px' }}>

          <h1 style={{
            textAlign: 'center',
            marginBottom: '30px',
            fontSize: '1.8rem',
            fontWeight: '800',
            color: theme.textPrimary
          }}>
            📜 내 주문 관리
          </h1>

          {/* 상단 총액 카드 */}
          <div style={{
            backgroundColor: theme.bgCard,
            borderRadius: '24px',
            padding: '35px 30px',
            marginBottom: '35px',
            textAlign: 'center',
            boxShadow: theme.shadow,
            border: `1px solid ${theme.border}`
          }}>
            <p style={{ color: theme.textSecondary, fontSize: '1rem', marginBottom: '12px' }}>
              누적 결제 금액
            </p>
            <h2 style={{
              color: theme.accent,
              fontSize: '2.8rem',
              margin: 0,
              fontWeight: '800'
            }}>
              {totalAmount.toLocaleString()}
              <span style={{ fontSize: '1.4rem', marginLeft: '8px' }}>원</span>
            </h2>
          </div>

          {/* 주문 목록 */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {orders.length > 0 ? (
                orders.map(order => {
                  const pId = order.productId || (order.product && order.product.id);

                  const cardStyle = {
                    backgroundColor: theme.bgCard,
                    padding: '28px',
                    borderRadius: '20px',
                    boxShadow: theme.shadow,
                    border: `1px solid ${theme.border}`
                  };

                  const statusBadgeStyle = {
                    padding: '6px 14px',
                    borderRadius: '10px',
                    fontSize: '0.75rem',
                    backgroundColor: theme.accentLight,
                    color: theme.accent,
                    fontWeight: '700',
                    marginLeft: '12px',
                    whiteSpace: 'nowrap'
                  };

                  return (
                      <div key={order.id} style={cardStyle}>
                        {/* 상단: 상품명 및 날짜 */}
                        <div style={{ marginBottom: '18px' }}>
                          <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'flex-start',
                            gap: '12px'
                          }}>
                            <h3 style={{
                              margin: 0,
                              fontSize: '1.25rem',
                              color: theme.textPrimary,
                              fontWeight: '700',
                              flex: 1,
                              wordBreak: 'break-word',
                              lineHeight: '1.4'
                            }}>
                              {order.productName || (order.product && order.product.name)}
                            </h3>
                            <span style={statusBadgeStyle}>결제완료</span>
                          </div>
                          <p style={{
                            color: theme.textSecondary,
                            fontSize: '0.95rem',
                            marginTop: '10px',
                            lineHeight: '1.5'
                          }}>
                            📅 {new Date(order.orderDate).toLocaleDateString()}  ·  📦 {order.quantity}개
                          </p>
                        </div>

                        {/* 중단: 가격 표시 */}
                        <div style={{
                          padding: '18px 0',
                          borderTop: `1px dashed ${theme.borderDashed}`,
                          borderBottom: `1px dashed ${theme.borderDashed}`,
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center'
                        }}>
                    <span style={{ color: theme.textSecondary, fontSize: '1rem' }}>
                      결제 금액
                    </span>
                          <span style={{
                            fontSize: '1.35rem',
                            fontWeight: '700',
                            color: theme.textPrimary
                          }}>
                      {order.totalPrice.toLocaleString()}원
                    </span>
                        </div>

                        {/* ✅ 수정된 하단: 버튼 그룹 (같은 크기) */}
                        <div style={{ marginTop: '18px', display: 'flex', gap: '10px' }}>
                          <button
                              onClick={() => {
                                if (!pId) return alert("상품 정보를 찾을 수 없습니다.");
                                navigate(`/review-management?productId=${pId}&orderId=${order.id}`);
                              }}
                              style={{
                                flex: 1,
                                padding: '14px 12px',
                                borderRadius: '12px',
                                fontSize: '0.9rem',
                                cursor: 'pointer',
                                backgroundColor: '#007bff',
                                border: 'none',
                                color: '#fff',
                                fontWeight: '700',
                                transition: 'all 0.2s',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis'
                              }}
                              onMouseEnter={(e) => e.target.style.backgroundColor = '#0056b3'}
                              onMouseLeave={(e) => e.target.style.backgroundColor = '#007bff'}
                          >
                            ✍️ 리뷰 작성
                          </button>

                          <button
                              onClick={() => handleCancel(order.id)}
                              style={{
                                flex: 1,
                                padding: '14px 12px',
                                borderRadius: '12px',
                                border: `2px solid ${theme.errorColor}`,
                                color: theme.errorColor,
                                cursor: 'pointer',
                                backgroundColor: theme.errorBg,
                                fontSize: '0.9rem',
                                fontWeight: '700',
                                transition: 'all 0.2s',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis'
                              }}
                              onMouseEnter={(e) => {
                                e.target.style.backgroundColor = theme.errorColor;
                                e.target.style.color = '#fff';
                              }}
                              onMouseLeave={(e) => {
                                e.target.style.backgroundColor = theme.errorBg;
                                e.target.style.color = theme.errorColor;
                              }}
                          >
                            취소하기
                          </button>
                        </div>
                      </div>
                  );
                })
            ) : (
                <div style={{ textAlign: 'center', padding: '80px 20px', color: theme.textSecondary }}>
                  <p style={{ fontSize: '3.5rem', marginBottom: '15px' }}>📦</p>
                  <p style={{ fontSize: '1.1rem' }}>주문 내역이 아직 없어요.</p>
                </div>
            )}
          </div>

          {/* 하단 홈 버튼 섹션 */}
          <div style={{ textAlign: 'center', marginTop: '60px' }}>
            <button
                onClick={() => navigate('/')}
                style={{
                  padding: '16px 50px',
                  borderRadius: '30px',
                  border: `2px solid ${theme.accent}`,
                  cursor: 'pointer',
                  backgroundColor: 'transparent',
                  color: theme.accent,
                  fontSize: '1.1rem',
                  fontWeight: 'bold',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = theme.accent;
                  e.target.style.color = '#fff';
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = 'transparent';
                  e.target.style.color = theme.accent;
                }}
            >
              🏠 홈으로 돌아가기
            </button>
          </div>
        </div>
      </div>
  );
};

export default OrderListPage;