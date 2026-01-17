import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api'; // Axios 인스턴스 (JWT 토큰 헤더 포함 필수)

const ProductPage = () => {
    // -------------------------------------------------------------------------
    // 1. 상태 관리 (State Management)
    // -------------------------------------------------------------------------
    const [products, setProducts] = useState([]);         // 전체 상품 목록 (기본 캐시용)
    const [filteredProducts, setFilteredProducts] = useState([]); // 실제 화면에 보여줄 상품 목록
    const [loading, setLoading] = useState(true);         // 로딩 상태
    const [quantities, setQuantities] = useState({});     // 상품별 구매 수량
    const [selectedCategory, setSelectedCategory] = useState('전체'); // 현재 선택된 탭
    const [showModal, setShowModal] = useState(false);    // 주문 완료 모달 표시 여부
    const [isDarkMode, setIsDarkMode] = useState(false);  // 다크 모드 상태
    const navigate = useNavigate();

    // -------------------------------------------------------------------------
    // 2. 상수 데이터 (Constants)
    // -------------------------------------------------------------------------
    // [변경] '나만의 추천' 카테고리를 추가했습니다.
    const categories = ['전체', '나만의 추천', '인기 상품', '트렌딩 상품', '가전', '의류', '식품'];

    // [변경] 탭별 설명 문구 추가
    const categoryDescriptions = {
        '전체': '영무마켓의 모든 상품을 한눈에 확인해보세요.',
        '나만의 추천': '✨ 고객님의 취향을 분석하여 AI가 엄선한 추천 상품입니다.', // 추가된 설명
        '인기 상품': '🔥 고객님들에게 가장 많은 사랑을 받은 베스트셀러 TOP 3!',
        '트렌딩 상품': '⚡ 요즘 가장 핫한 급상승 트렌드 상품을 모았습니다.',
        '가전': '📱 생활을 편리하게 만드는 최신 스마트 가전입니다.',
        '의류': '👕 나만의 스타일을 완성할 이번 시즌 트렌드 룩.',
        '식품': '🍎 산지의 신선함을 그대로 담은 맛있는 먹거리.'
    };

    // -------------------------------------------------------------------------
    // 3. 다크 모드 감지 (Effects)
    // -------------------------------------------------------------------------
    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        setIsDarkMode(mediaQuery.matches);

        const listener = (e) => setIsDarkMode(e.matches);
        mediaQuery.addEventListener('change', listener);

        return () => mediaQuery.removeEventListener('change', listener);
    }, []);

    // -------------------------------------------------------------------------
    // 4. 데이터 로딩 (Data Fetching)
    // -------------------------------------------------------------------------
    // 초기 로딩: 전체 상품 목록을 가져옴 (기본 데이터)
    useEffect(() => {
        api.get('/api/products')
            .then(response => {
                setProducts(response.data);
                // 초기 화면은 '전체' 탭이므로 전체 데이터 표시
                if (selectedCategory === '전체') {
                    setFilteredProducts(response.data);
                }

                // 수량 초기화 (각 상품 1개)
                const initialQuantities = {};
                response.data.forEach(product => {
                    initialQuantities[product.id] = 1;
                });
                setQuantities(initialQuantities);
                setLoading(false);
            })
            .catch(error => {
                console.error('상품 로딩 에러:', error);
                setLoading(false);
            });
    }, []); // 최초 1회만 실행

    // 탭 변경 시 데이터 처리 (API 연동 핵심 로직)
    useEffect(() => {
        setLoading(true);

        // A. '전체' 탭: 이미 로드된 products 사용
        if (selectedCategory === '전체') {
            setFilteredProducts(products);
            setLoading(false);
        }
        // [추가] B. '나만의 추천' 탭: 백엔드 RecommendationController 호출
        else if (selectedCategory === '나만의 추천') {
            // RecommendationController.java의 @GetMapping("/recommendations") 호출
            api.get('/api/recommendations', { params: { topN: 5 } })
                .then(response => {
                    // 백엔드 반환 타입: RecommendationResponse { products: [...] }
                    // 따라서 response.data가 아니라 response.data.products를 써야 함
                    if (response.data && response.data.products) {
                        setFilteredProducts(response.data.products);
                    } else {
                        setFilteredProducts([]);
                    }
                    setLoading(false);
                })
                .catch(err => {
                    console.error("추천 시스템 에러:", err);
                    // 로그인하지 않았거나 에러 발생 시 빈 배열 처리 혹은 안내 메시지
                    alert("로그인이 필요한 서비스이거나, 추천 데이터를 불러오지 못했습니다.");
                    setFilteredProducts([]);
                    setLoading(false);
                });
        }
        // C. '인기 상품' 탭
        else if (selectedCategory === '인기 상품') {
            api.get('/api/products/trending/popular')
                .then(response => {
                    setFilteredProducts(response.data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error(err);
                    setFilteredProducts([]);
                    setLoading(false);
                });
        }
        // D. '트렌딩 상품' 탭
        else if (selectedCategory === '트렌딩 상품') {
            api.get('/api/products/trending/trending')
                .then(response => {
                    setFilteredProducts(response.data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error(err);
                    setFilteredProducts([]);
                    setLoading(false);
                });
        }
        // E. 일반 카테고리 탭: 클라이언트 필터링
        else {
            const filtered = products.filter(p => p.category === selectedCategory);
            setFilteredProducts(filtered);
            setLoading(false);
        }
    }, [selectedCategory, products]);

    // -------------------------------------------------------------------------
    // 5. 이벤트 핸들러 (Event Handlers)
    // -------------------------------------------------------------------------
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
            console.error('주문 실패:', error);
            alert('주문 중 오류가 발생했습니다.');
        }
    };

    const handleGoToOrders = () => {
        setShowModal(false);
        navigate('/orders');
    };

    const handleContinueShopping = () => {
        setShowModal(false);
    };

    // -------------------------------------------------------------------------
    // 6. 헬퍼 함수 (Helpers)
    // -------------------------------------------------------------------------
    // 순위 배지 렌더링 (인기/트렌딩 탭에서 1~3위만 표시)
    const renderRankBadge = (index) => {
        // [수정] 나만의 추천도 순위를 매길지 결정 (여기선 제외하고 인기/트렌딩만 표시)
        const isRankedCategory = ['인기 상품', '트렌딩 상품'].includes(selectedCategory);

        if (!isRankedCategory) return null;
        if (index > 2) return null; // 4위부터는 배지 없음

        const badges = [
            { color: '#FFD700', icon: '🥇', label: '1위' }, // Gold
            { color: '#C0C0C0', icon: '🥈', label: '2위' }, // Silver
            { color: '#CD7F32', icon: '🥉', label: '3위' }  // Bronze
        ];

        const badge = badges[index];

        return (
            <div
                style={{
                    position: 'absolute',
                    top: '10px',
                    left: '10px',
                    backgroundColor: badge.color,
                    color: '#fff',
                    padding: '6px 10px',
                    borderRadius: '20px',
                    fontWeight: '800',
                    fontSize: '0.85rem',
                    boxShadow: '0 2px 5px rgba(0,0,0,0.3)',
                    zIndex: 10,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    textShadow: '0 1px 2px rgba(0,0,0,0.2)'
                }}
            >
                <span style={{ fontSize: '1rem' }}>{badge.icon}</span>
                <span>{badge.label}</span>
            </div>
        );
    };

    // -------------------------------------------------------------------------
    // 7. 스타일 및 테마 (Styles)
    // -------------------------------------------------------------------------
    if (loading) {
        return (
            <div style={{ padding: '20px', textAlign: 'center', color: isDarkMode ? '#fff' : '#333' }}>
                로딩 중...
            </div>
        );
    }

    const theme = {
        bgPrimary: isDarkMode ? '#0f0f0f' : '#ffffff',
        bgCard: isDarkMode ? '#1a1a1a' : '#ffffff',
        textPrimary: isDarkMode ? '#ffffff' : '#000000',
        textSecondary: isDarkMode ? '#999' : '#666666',
        textPrice: isDarkMode ? '#4fc3f7' : '#d63031',
        textQty: isDarkMode ? '#ffffff' : '#000000',
        border: isDarkMode ? '#2a2a2a' : '#e0e0e0',
        shadow: isDarkMode ? '0 2px 4px rgba(0,0,0,0.4)' : '0 1px 3px rgba(0,0,0,0.08)',
        btnBg: isDarkMode ? '#262626' : '#f5f5f5',
        btnBorder: isDarkMode ? '#444' : '#d9d9d9',
        categoryActive: '#03C75A',
        sectionBg: isDarkMode ? '#1f1f1f' : '#f0f4f8'
    };

    return (
        <div style={{ backgroundColor: theme.bgPrimary, color: theme.textPrimary, minHeight: '100vh' }}>
            {/* 헤더 */}
            <div
                style={{
                    backgroundColor: '#03C75A',
                    textAlign: 'center',
                    padding: '20px 15px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                }}
            >
                <h1 style={{ margin: '0', color: 'white', fontWeight: '900', fontSize: '1.8rem' }}>
                    📦 상품 목록
                </h1>
            </div>

            {/* 카테고리 탭 (가로 스크롤) */}
            <div
                style={{
                    display: 'flex',
                    backgroundColor: theme.bgCard,
                    borderBottom: `2px solid ${theme.border}`,
                    position: 'sticky',
                    top: '0',
                    zIndex: 90,
                    overflowX: 'auto',
                    whiteSpace: 'nowrap'
                }}
            >
                {categories.map(cat => (
                    <div
                        key={cat}
                        onClick={() => setSelectedCategory(cat)}
                        style={{
                            cursor: 'pointer',
                            padding: '16px 20px',
                            fontSize: '0.95rem',
                            fontWeight: selectedCategory === cat ? '700' : '500',
                            color: selectedCategory === cat ? theme.categoryActive : theme.textSecondary,
                            borderBottom:
                                selectedCategory === cat ? `3px solid ${theme.categoryActive}` : '3px solid transparent',
                            transition: 'all 0.2s ease',
                            flex: '0 0 auto',
                            textAlign: 'center',
                            minWidth: '80px'
                        }}
                    >
                        {cat}
                    </div>
                ))}
            </div>

            {/* 현재 섹션 설명 영역 */}
            <div
                style={{
                    backgroundColor: theme.sectionBg,
                    padding: '25px 20px',
                    textAlign: 'center',
                    borderBottom: `1px solid ${theme.border}`
                }}
            >
                <h2 style={{
                    margin: '0 0 8px 0',
                    fontSize: '1.4rem',
                    fontWeight: '800',
                    color: theme.textPrimary
                }}>
                    {selectedCategory}
                </h2>
                <p style={{
                    margin: '0',
                    fontSize: '0.95rem',
                    color: theme.textSecondary,
                    lineHeight: '1.5'
                }}>
                    {categoryDescriptions[selectedCategory] || '상품을 확인해보세요.'}
                </p>
            </div>

            {/* 상품 리스트 */}
            <div
                style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    padding: '20px 15px'
                }}
            >
                <div style={{ width: '100%', maxWidth: '600px' }}>
                    {filteredProducts.length > 0 ? (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '12px' }}>
                            {filteredProducts.map((product, index) => (
                                <div
                                    key={product.id}
                                    style={{
                                        borderRadius: '8px',
                                        backgroundColor: theme.bgCard,
                                        display: 'flex',
                                        flexDirection: 'column',
                                        overflow: 'hidden',
                                        border: `1px solid ${theme.border}`,
                                        boxShadow: theme.shadow,
                                        position: 'relative' // 배지 위치 기준점
                                    }}
                                >
                                    {/* 상품 이미지 영역 */}
                                    <div
                                        style={{
                                            width: '100%',
                                            aspectRatio: '4 / 3',
                                            minHeight: '200px',
                                            backgroundColor: isDarkMode ? '#222' : '#f8f8f8',
                                            overflow: 'hidden',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            position: 'relative' // 이미지 위 배지 배치용
                                        }}
                                    >
                                        {/* 🏆 순위 배지 표시 (조건부 렌더링) */}
                                        {renderRankBadge(index)}

                                        {product.imageUrl ? (
                                            <img
                                                src={product.imageUrl}
                                                alt={product.name}
                                                style={{
                                                    width: '100%',
                                                    height: '100%',
                                                    objectFit: 'cover',
                                                    display: 'block'
                                                }}
                                            />
                                        ) : (
                                            <div
                                                style={{
                                                    width: '100%',
                                                    height: '100%',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    color: theme.textSecondary,
                                                    fontSize: '0.9rem'
                                                }}
                                            >
                                                이미지 준비중
                                            </div>
                                        )}
                                    </div>

                                    <div style={{ padding: '16px' }}>
                                        <h4 style={{ margin: '0 0 8px 0', fontSize: '1.1rem', color: theme.textPrimary, fontWeight: '600' }}>
                                            {product.name}
                                        </h4>

                                        <p style={{ margin: '0 0 12px 0', fontSize: '0.85rem', color: theme.textSecondary, lineHeight: '1.4', wordBreak: 'keep-all' }}>
                                            {product.description || '간단한 상품 설명이 없습니다.'}
                                        </p>

                                        <p style={{ color: theme.textPrice, fontWeight: '700', fontSize: '1.3rem', margin: '8px 0 12px 0' }}>
                                            {product.price.toLocaleString()}원
                                        </p>

                                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', margin: '12px 0', padding: '12px 0', borderTop: `1px solid ${theme.border}`, borderBottom: `1px solid ${theme.border}` }}>
                                            <button
                                                type="button"
                                                onClick={() => updateQuantity(product.id, -1)}
                                                style={{
                                                    width: '36px',
                                                    height: '36px',
                                                    borderRadius: '4px',
                                                    border: `1px solid ${theme.btnBorder}`,
                                                    backgroundColor: theme.btnBg,
                                                    color: theme.textQty,
                                                    fontSize: '1.1rem',
                                                    fontWeight: 'bold',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    cursor: 'pointer'
                                                }}
                                            >
                                                −
                                            </button>

                                            <span style={{ fontWeight: '600', fontSize: '1.1rem', minWidth: '40px', padding: '6px 10px', backgroundColor: isDarkMode ? '#222' : '#f5f5f5', color: theme.textQty, borderRadius: '4px', textAlign: 'center' }}>
                                                {quantities[product.id] || 1}
                                            </span>

                                            <button
                                                type="button"
                                                onClick={() => updateQuantity(product.id, 1)}
                                                style={{
                                                    width: '36px',
                                                    height: '36px',
                                                    borderRadius: '4px',
                                                    border: `1px solid ${theme.btnBorder}`,
                                                    backgroundColor: theme.btnBg,
                                                    color: theme.textQty,
                                                    fontSize: '1.1rem',
                                                    fontWeight: 'bold',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    cursor: 'pointer'
                                                }}
                                            >
                                                +
                                            </button>
                                        </div>

                                        <button
                                            onClick={() => handleOrder(product.id, product.name)}
                                            style={{
                                                backgroundColor: '#03C75A',
                                                color: 'white',
                                                border: 'none',
                                                padding: '14px',
                                                borderRadius: '6px',
                                                cursor: 'pointer',
                                                width: '100%',
                                                fontWeight: '700',
                                                fontSize: '1rem'
                                            }}
                                        >
                                            구매하기
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div style={{ textAlign: 'center', padding: '60px 20px', color: theme.textSecondary }}>
                            <p>현재 '{selectedCategory}' 카테고리에</p>
                            <p>등록된 상품이 없습니다.</p>
                        </div>
                    )}

                    <div style={{ textAlign: 'center', marginTop: '40px' }}>
                        <button
                            onClick={() => navigate('/')}
                            style={{
                                padding: '12px 40px',
                                borderRadius: '6px',
                                border: `2px solid ${theme.categoryActive}`,
                                cursor: 'pointer',
                                backgroundColor: 'transparent',
                                color: theme.categoryActive,
                                fontSize: '1rem',
                                fontWeight: '700'
                            }}
                        >
                            🏠 홈으로 돌아가기
                        </button>
                    </div>
                </div>
            </div>

            {/* 주문 완료 모달 */}
            {showModal && (
                <div
                    style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.5)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000
                    }}
                >
                    <div
                        style={{
                            backgroundColor: theme.bgCard,
                            padding: '40px 30px',
                            borderRadius: '12px',
                            textAlign: 'center',
                            width: '90%',
                            maxWidth: '340px',
                            boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center'
                        }}
                    >
                        <div
                            style={{
                                width: '60px',
                                height: '60px',
                                borderRadius: '50%',
                                backgroundColor: '#03C75A',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                marginBottom: '20px'
                            }}
                        >
                            <span style={{ color: 'white', fontSize: '32px' }}>✔</span>
                        </div>

                        <h3 style={{ margin: '0 0 12px', color: theme.textPrimary, fontSize: '1.5rem', fontWeight: '700' }}>
                            주문 완료
                        </h3>
                        <p style={{ margin: '0 0 30px', color: theme.textSecondary, fontSize: '0.95rem' }}>
                            주문이 정상적으로 접수되었습니다.
                        </p>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', width: '100%' }}>
                            <button
                                onClick={handleContinueShopping}
                                style={{
                                    backgroundColor: '#03C75A',
                                    color: '#fff',
                                    border: 'none',
                                    padding: '14px',
                                    borderRadius: '6px',
                                    cursor: 'pointer',
                                    fontWeight: '700',
                                    fontSize: '1rem'
                                }}
                            >
                                쇼핑 계속하기
                            </button>

                            <button
                                onClick={handleGoToOrders}
                                style={{
                                    backgroundColor: theme.bgCard,
                                    color: theme.textPrimary,
                                    border: `1px solid ${theme.border}`,
                                    padding: '14px',
                                    borderRadius: '6px',
                                    cursor: 'pointer',
                                    fontWeight: '700',
                                    fontSize: '1rem'
                                }}
                            >
                                주문 내역 보기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProductPage;
