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
    const [isDarkMode, setIsDarkMode] = useState(false);
    const navigate = useNavigate();

    const categories = ['전체', '가전', '의류', '식품'];

    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        setIsDarkMode(mediaQuery.matches);

        const listener = (e) => setIsDarkMode(e.matches);
        mediaQuery.addEventListener('change', listener);

        return () => mediaQuery.removeEventListener('change', listener);
    }, []);

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
                console.error('상품 로딩 에러:', error);
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
        categoryActive: '#03C75A'
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

            {/* 카테고리 탭 */}
            <div
                style={{
                    display: 'flex',
                    backgroundColor: theme.bgCard,
                    borderBottom: `2px solid ${theme.border}`,
                    position: 'sticky',
                    top: '0',
                    zIndex: 90
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
                            flex: 1,
                            textAlign: 'center'
                        }}
                    >
                        {cat}
                    </div>
                ))}
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
                            {filteredProducts.map(product => (
                                <div
                                    key={product.id}
                                    style={{
                                        borderRadius: '8px',
                                        backgroundColor: theme.bgCard,
                                        display: 'flex',
                                        flexDirection: 'column',
                                        overflow: 'hidden',
                                        border: `1px solid ${theme.border}`,
                                        boxShadow: theme.shadow
                                    }}
                                >
                                    {/* ✅ 수정된 이미지 컨테이너 */}
                                    <div
                                        style={{
                                            width: '100%',
                                            aspectRatio: '4 / 3',
                                            minHeight: '200px',
                                            backgroundColor: isDarkMode ? '#222' : '#f8f8f8',
                                            overflow: 'hidden',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center'
                                        }}
                                    >
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
                                    color: '#000',
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