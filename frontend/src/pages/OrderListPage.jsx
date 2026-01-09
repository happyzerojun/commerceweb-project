import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const OrderListPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

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

  if (loading) return <div style={{ textAlign: 'center', padding: '100px', color: '#666' }}>로딩 중...</div>;

  return (
    <div style={{ backgroundColor: '#f8f9fa', minHeight: '100vh', paddingBottom: '50px' }}>
      <div style={{ maxWidth: '600px', margin: '0 auto', padding: '40px 20px' }}>

        <h1 style={{ textAlign: 'center', marginBottom: '30px', fontSize: '1.6rem', fontWeight: '800', color: '#333' }}>
          📜 내 주문 관리
        </h1>

        {/* 상단 총액 카드 */}
        <div style={{
          backgroundColor: '#fff',
          borderRadius: '24px',
          padding: '30px',
          marginBottom: '30px',
          textAlign: 'center',
          boxShadow: '0 10px 20px rgba(0,0,0,0.05)',
          border: '1px solid rgba(0,0,0,0.03)'
        }}>
          <p style={{ color: '#888', fontSize: '0.95rem', marginBottom: '8px' }}>누적 결제 금액</p>
          <h2 style={{ color: '#00c73c', fontSize: '2.4rem', margin: 0, fontWeight: '800' }}>
            {totalAmount.toLocaleString()}<span style={{ fontSize: '1.2rem', marginLeft: '4px' }}>원</span>
          </h2>
        </div>

        {/* 주문 목록 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {orders.length > 0 ? (
            orders.map(order => {
              const pId = order.productId || (order.product && order.product.id);

              return (
                <div key={order.id} style={enhancedCardStyle}>
                  {/* 상단: 상품명 및 날짜 */}
                  <div style={{ marginBottom: '15px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <h3 style={{ margin: 0, fontSize: '1.15rem', color: '#333', fontWeight: '700', flex: 1 }}>
                        {order.productName || (order.product && order.product.name)}
                      </h3>
                      <span style={statusBadgeStyle}>결제완료</span>
                    </div>
                    <p style={{ color: '#999', fontSize: '0.85rem', marginTop: '6px' }}>
                      📅 {new Date(order.orderDate).toLocaleDateString()}  ·  📦 {order.quantity}개
                    </p>
                  </div>

                  {/* 중단: 가격 표시 */}
                  <div style={{
                    padding: '15px 0',
                    borderTop: '1px dashed #eee',
                    borderBottom: '1px dashed #eee',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <span style={{ color: '#666', fontSize: '0.9rem' }}>결제 금액</span>
                    <span style={{ fontSize: '1.2rem', fontWeight: '700', color: '#222' }}>
                      {order.totalPrice.toLocaleString()}원
                    </span>
                  </div>

                  {/* 하단: 버튼 그룹 (간격 및 배치 유지) */}
                  <div style={{ marginTop: '15px', display: 'flex', gap: '10px' }}>
                    <button
                      onClick={() => {
                        if(!pId) return alert("상품 정보를 찾을 수 없습니다.");
                        navigate(`/review-management?productId=${pId}&orderId=${order.id}`);
                      }}
                      style={reviewBtnStyle}
                    >
                      ✍️ 리뷰 작성
                    </button>
                    {/* ✅ 취소하기 버튼 디자인 복구 */}
                    <button
                      onClick={() => handleCancel(order.id)}
                      style={oldCancelBtnStyle}
                    >
                      취소하기
                    </button>
                  </div>
                </div>
              );
            })
          ) : (
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#bbb' }}>
              <p style={{ fontSize: '3rem', marginBottom: '10px' }}>📦</p>
              <p>주문 내역이 아직 없어요.</p>
            </div>
          )}
        </div>

        {/* 하단 홈 버튼 섹션 */}
        <div style={{ textAlign: 'center', marginTop: '50px' }}>
          {/* ✅ 홈으로 돌아가기 버튼 디자인 복구 */}
          <button
            onClick={() => navigate('/')}
            style={oldHomeBtnStyle}
            onMouseEnter={(e) => {
                e.target.style.backgroundColor = '#00c73c';
                e.target.style.color = '#fff';
            }}
            onMouseLeave={(e) => {
                e.target.style.backgroundColor = '#fff';
                e.target.style.color = '#00c73c';
            }}
          >
            🏠 홈으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 스타일 정의 ---

const enhancedCardStyle = {
  backgroundColor: '#fff',
  padding: '24px',
  borderRadius: '20px',
  boxShadow: '0 4px 12px rgba(0,0,0,0.03)',
  border: '1px solid #f0f0f0'
};

const statusBadgeStyle = {
  padding: '4px 10px',
  borderRadius: '8px',
  fontSize: '0.7rem',
  backgroundColor: '#f1fcf4',
  color: '#00c73c',
  fontWeight: '700',
  marginLeft: '10px'
};

const reviewBtnStyle = {
  flex: 1,
  padding: '12px',
  borderRadius: '12px',
  fontSize: '0.9rem',
  cursor: 'pointer',
  backgroundColor: '#007bff',
  border: 'none',
  color: '#fff',
  fontWeight: '600'
};

// 🔙 이전 스타일 복구: 취소하기 버튼
const oldCancelBtnStyle = {
  flex: 1,
  padding: '12px',
  borderRadius: '8px',
  border: '1px solid #ff4d4f',
  color: '#ff4d4f',
  cursor: 'pointer',
  backgroundColor: '#fff',
  fontSize: '0.9rem'
};

// 🔙 이전 스타일 복구: 홈으로 돌아가기 버튼
const oldHomeBtnStyle = {
  padding: '12px 40px',
  borderRadius: '30px',
  border: '2px solid #00c73c',
  cursor: 'pointer',
  backgroundColor: '#fff',
  color: '#00c73c',
  fontSize: '1rem',
  fontWeight: 'bold',
  transition: 'all 0.2s'
};

export default OrderListPage;