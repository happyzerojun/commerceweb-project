import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const ProductPage = () => {
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [quantities, setQuantities] = useState({});
  const [selectedCategory, setSelectedCategory] = useState('전체');
  const [showModal, setShowModal] = useState(false);
  const navigate = useNavigate();

  const categories = ['전체', '가전', '의류', '식품'];

  useEffect(() => {
    api.get('/api/products')
      .then(response => {
        setProducts(response.data);
        setFilteredProducts(response.data);

        const initialQuantities = {};
        response.data.forEach(product => {
          initialQuantities[product.id] = 1;
        });
        setQuantities(initialQuantities);
        setLoading(false);
      })
      .catch(error => {
        console.error("상품 로딩 에러:", error);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    if (selectedCategory === '전체') {
      setFilteredProducts(products);
    } else {
      setFilteredProducts(products.filter(p => p.category === selectedCategory));
    }
  }, [selectedCategory, products]);

  const updateQuantity = (productId, delta) => {
    setQuantities(prev => ({
      ...prev,
      [productId]: Math.max(1, (prev[productId] || 1) + delta)
    }));
  };

  const handleOrder = async (productId, productName) => {
    const quantity = quantities[productId] || 1;
    if (!window.confirm(`${productName} ${quantity}개를 구매하시겠습니까?`)) return;

    try {
      await api.post('/api/orders', { productId, quantity });
      setShowModal(true);
    } catch (error) {
      console.error("주문 실패:", error);
      alert("주문 중 오류가 발생했습니다.");
    }
  };

  const handleGoToOrders = () => {
    setShowModal(false);
    navigate('/orders');
  };

  const handleContinueShopping = () => {
    setShowModal(false);
  };

  if (loading) return <div style={{ padding: '20px', textAlign: 'center' }}>로딩 중...</div>;

  return (
    <div style={{ padding: '20px 15px', display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '100vh', backgroundColor: '#f9f9f9', position: 'relative' }}>

      <h1 style={{ marginBottom: '20px', color: '#111', fontWeight: '800', fontSize: '1.6rem' }}>📦 상품 목록</h1>

      <div style={categoryTabBarStyle}>
        {categories.map(cat => (
          <div
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            style={{
              cursor: 'pointer',
              padding: '8px 15px',
              fontSize: '1rem',
              fontWeight: selectedCategory === cat ? 'bold' : '500',
              color: selectedCategory === cat ? '#03C75A' : '#8e94a0', // 네이버 그린 적용
              borderBottom: selectedCategory === cat ? '3px solid #03C75A' : '3px solid transparent',
              transition: 'all 0.2s ease'
            }}
          >
            {cat}
          </div>
        ))}
      </div>

      <div style={gridStyle}>
        {filteredProducts.length > 0 ? (
          filteredProducts.map(product => (
            <div key={product.id} style={cardStyle}>
              <div style={imageContainerStyle}>
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} style={imageStyle} />
                ) : (
                  <div style={noImageStyle}>이미지 준비중</div>
                )}
              </div>

              <div style={{ padding: '20px', textAlign: 'center' }}>
                <h4 style={productNameStyle}>{product.name}</h4>
                <p style={descriptionStyle}>{product.description || "간단한 상품 설명이 없습니다."}</p>
                <p style={priceStyle}>{product.price.toLocaleString()}원</p>

                <div style={qtySectionStyle}>
                  <button type="button" onClick={() => updateQuantity(product.id, -1)} style={qtyBtnStyle}>-</button>
                  <span style={{ fontWeight: 'bold', fontSize: '1.2rem', minWidth: '40px' }}>{quantities[product.id] || 1}</span>
                  <button type="button" onClick={() => updateQuantity(product.id, 1)} style={qtyBtnStyle}>+</button>
                </div>

                <button onClick={() => handleOrder(product.id, product.name)} style={orderBtnStyle}>구매하기</button>
              </div>
            </div>
          ))
        ) : (
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
            <p>현재 '{selectedCategory}' 카테고리에</p>
            <p>등록된 상품이 없습니다.</p>
          </div>
        )}
      </div>

      <button onClick={() => navigate('/')} style={homeBtnStyle}>🏠 홈으로</button>

      {/* ✅ 수정된 모달 디자인 (네이버 스타일) */}
      {showModal && (
        <div style={modalOverlayStyle}>
          <div style={modalContentStyle}>
            {/* 체크 아이콘 추가로 직관성 높임 */}
            <div style={iconCircleStyle}>
              <span style={{ color: 'white', fontSize: '24px' }}>✔</span>
            </div>

            <h3 style={{ margin: '15px 0 10px', color: '#222', fontSize: '1.4rem' }}>주문 완료</h3>
            <p style={{ margin: '0 0 25px 0', color: '#888', fontSize: '0.95rem' }}>주문이 정상적으로 접수되었습니다.</p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', width: '100%' }}>
              {/* 쇼핑 계속하기 버튼 (강조) */}
              <button onClick={handleContinueShopping} style={modalPrimaryBtnStyle}>
                쇼핑 계속하기
              </button>

              {/* 주문 내역 보기 버튼 (보조) */}
              <button onClick={handleGoToOrders} style={modalSecondaryBtnStyle}>
                주문 내역 보기
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

// --- 스타일 객체들 ---

// 전체 테마 색상 (네이버 그린)
const THEME_COLOR = '#03C75A';

const categoryTabBarStyle = {
  display: 'flex',
  justifyContent: 'space-around',
  backgroundColor: '#fff',
  padding: '10px 5px',
  borderRadius: '8px', // 네이버 스타일은 둥근 정도가 심플함
  border: '1px solid #e1e3e8', // 얇은 테두리 추가
  width: '100%',
  maxWidth: '450px',
  marginBottom: '20px',
  position: 'sticky',
  top: '10px',
  zIndex: 90
};

const gridStyle = { display: 'grid', gridTemplateColumns: '1fr', gap: '20px', width: '100%', maxWidth: '450px', marginBottom: '40px' };
const cardStyle = { borderRadius: '12px', backgroundColor: '#fff', display: 'flex', flexDirection: 'column', overflow: 'hidden', border: '1px solid #eee' };
const imageContainerStyle = { width: '100%', height: '220px', backgroundColor: '#f4f4f4' };
const imageStyle = { width: '100%', height: '100%', objectFit: 'cover' };
const noImageStyle = { width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc' };
const productNameStyle = { margin: '0 0 5px 0', fontSize: '1.2rem', color: '#333', fontWeight: 'bold' };
const descriptionStyle = { margin: '0 0 10px 0', fontSize: '0.9rem', color: '#777', lineHeight: '1.4', wordBreak: 'keep-all' };
const priceStyle = { color: '#000', fontWeight: '800', fontSize: '1.4rem', margin: '5px 0' }; // 가격은 검정으로 강하게
const qtySectionStyle = { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '15px', margin: '15px 0', padding: '10px 0', borderTop: '1px solid #f2f2f2' };
const qtyBtnStyle = { width: '32px', height: '32px', borderRadius: '50%', border: '1px solid #ddd', backgroundColor: '#fff', color: '#333', fontSize: '1.2rem', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' };
const orderBtnStyle = { backgroundColor: THEME_COLOR, color: 'white', border: 'none', padding: '16px', borderRadius: '6px', cursor: 'pointer', width: '100%', fontWeight: 'bold', fontSize: '1.1rem' };
const homeBtnStyle = { padding: '12px 30px', borderRadius: '25px', border: '1px solid #ddd', backgroundColor: '#fff', color: '#333', fontWeight: 'bold', cursor: 'pointer', marginBottom: '20px' };

// ✅ 모달 스타일 (네이버 스타일 적용)
const modalOverlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.6)', // 배경을 조금 더 어둡게 해서 모달에 집중
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 1000,
  backdropFilter: 'blur(3px)' // 배경 흐림 효과 추가
};

const modalContentStyle = {
  backgroundColor: '#fff',
  padding: '35px 25px',
  borderRadius: '16px',
  textAlign: 'center',
  width: '85%',
  maxWidth: '320px',
  boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  animation: 'fadeIn 0.2s ease-out'
};

const iconCircleStyle = {
  width: '50px',
  height: '50px',
  borderRadius: '50%',
  backgroundColor: THEME_COLOR,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  marginBottom: '5px'
};

// 쇼핑 계속하기 (Primary) - 강조됨
const modalPrimaryBtnStyle = {
  backgroundColor: THEME_COLOR, // 네이버 그린
  color: '#fff',
  border: 'none',
  padding: '14px',
  borderRadius: '8px',
  cursor: 'pointer',
  fontWeight: 'bold',
  fontSize: '1rem',
  width: '100%', // 꽉 차게
  marginBottom: '5px'
};

// 주문 내역 보기 (Secondary) - 덜 강조됨
const modalSecondaryBtnStyle = {
  backgroundColor: '#fff',
  color: '#555',
  border: '1px solid #ddd', // 옅은 테두리
  padding: '14px',
  borderRadius: '8px',
  cursor: 'pointer',
  fontWeight: 'bold',
  fontSize: '1rem',
  width: '100%'
};

export default ProductPage;
