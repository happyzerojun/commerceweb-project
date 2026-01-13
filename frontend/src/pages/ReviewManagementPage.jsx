import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../services/api';

const ReviewManagementPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const queryParams = new URLSearchParams(location.search);

    const productIdFromUrl = queryParams.get('productId');
    const orderIdFromUrl = queryParams.get('orderId');

    const [products, setProducts] = useState([]);
    const [reviews, setReviews] = useState([]);
    const [content, setContent] = useState('');
    const [rating, setRating] = useState(5);
    const [loading, setLoading] = useState(true);
    const [isDarkMode, setIsDarkMode] = useState(false);

    // ✅ 수정 로직을 위한 상태 추가
    const [editingReviewId, setEditingReviewId] = useState(null);
    const [editContent, setEditContent] = useState('');
    const [editRating, setEditRating] = useState(5);

    // 다크모드 감지
    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        setIsDarkMode(mediaQuery.matches);

        const listener = (e) => setIsDarkMode(e.matches);
        mediaQuery.addEventListener('change', listener);

        return () => mediaQuery.removeEventListener('change', listener);
    }, []);

    useEffect(() => {
        fetchProducts();
        fetchReviews();
    }, [productIdFromUrl]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/products');
            setProducts(res.data);
        } catch (err) {
            console.error("상품 목록 로딩 실패", err);
        }
    };

    const fetchReviews = async () => {
        setLoading(true);
        try {
            let res;
            if (productIdFromUrl && productIdFromUrl !== 'undefined') {
                res = await api.get(`/api/reviews/product/${productIdFromUrl}`);
            } else {
                res = await api.get('/api/reviews');
            }
            setReviews(res.data);
        } catch (err) {
            console.error("리뷰 로딩 실패", err);
        } finally {
            setLoading(false);
        }
    };

    const handleProductChange = (e) => {
        const selectedId = e.target.value;
        if (selectedId) {
            navigate(`/review-management?productId=${selectedId}`);
        } else {
            navigate(`/review-management`);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!orderIdFromUrl || orderIdFromUrl === 'undefined') {
            return alert("주문 정보를 찾을 수 없습니다. 마이페이지에서 리뷰 작성을 시도해주세요.");
        }
        if (!content.trim()) return alert("리뷰 내용을 입력해주세요.");

        const reviewData = {
            productId: Number(productIdFromUrl),
            orderId: Number(orderIdFromUrl),
            content: content,
            rating: rating
        };

        try {
            await api.post('/api/reviews', reviewData);
            alert("리뷰가 등록되었습니다!");
            navigate(`/review-management?productId=${productIdFromUrl}`);
            fetchReviews();
            setContent('');
        } catch (err) {
            alert(err.response?.data?.message || "리뷰 등록 중 오류가 발생했습니다.");
        }
    };

    // ✅ 수정 모드 진입
    const startEdit = (review) => {
        setEditingReviewId(review.id);
        setEditContent(review.content);
        setEditRating(review.rating);
    };

    // ✅ 수정 취소
    const cancelEdit = () => {
        setEditingReviewId(null);
        setEditContent('');
        setEditRating(5);
    };

    // ✅ 수정 처리 (PUT)
    const handleUpdate = async (reviewId) => {
        if (!editContent.trim()) return alert("내용을 입력해주세요.");
        try {
            await api.put(`/api/reviews/${reviewId}`, {
                content: editContent,
                rating: editRating
            });
            alert("리뷰가 수정되었습니다.");
            setEditingReviewId(null);
            fetchReviews();
        } catch (err) {
            alert(err.response?.data?.message || "수정 권한이 없거나 오류가 발생했습니다.");
        }
    };

    const handleDelete = async (reviewId) => {
        if (!window.confirm("정말 삭제하시겠습니까?")) return;
        try {
            await api.delete(`/api/reviews/${reviewId}`);
            alert("삭제되었습니다.");
            fetchReviews();
        } catch (err) {
            alert("본인의 리뷰만 삭제할 수 있습니다.");
        }
    };

    if (loading) {
        return (
            <div style={{
                textAlign: 'center',
                marginTop: '100px',
                fontSize: '1.2rem',
                color: isDarkMode ? '#b0b0b0' : '#666',
                backgroundColor: isDarkMode ? '#0f0f0f' : '#f9fafb',
                minHeight: '100vh'
            }}>
                ✨ 데이터를 불러오는 중입니다...
            </div>
        );
    }

    // 다크/라이트 모드 통합 테마 객체
    const theme = {
        bgPrimary: isDarkMode ? '#0f0f0f' : '#f9fafb',
        bgCard: isDarkMode ? '#1a1a1a' : '#ffffff',
        bgInput: isDarkMode ? '#222222' : '#f3f4f6',
        textPrimary: isDarkMode ? '#ffffff' : '#111827',
        textSecondary: isDarkMode ? '#b0b0b0' : '#6b7280',
        textAccent: isDarkMode ? '#4fc3f7' : '#10b981',
        textRating: isDarkMode ? '#ffd700' : '#fbbf24',
        textPrice: isDarkMode ? '#4fc3f7' : '#ef4444',
        border: isDarkMode ? '#333333' : '#e5e7eb',
        shadow: isDarkMode
            ? '0 4px 12px rgba(0,0,0,0.3)'
            : '0 4px 6px -1px rgba(0,0,0,0.1)',
        shadowSmall: isDarkMode
            ? '0 2px 6px rgba(0,0,0,0.2)'
            : '0 4px 6px -1px rgba(0,0,0,0.05)',
        accentBg: isDarkMode ? '#064e3b' : '#ecfdf5',
        accentBorder: isDarkMode ? '#059669' : '#10b981',
        accentColor: '#03C75A',
        accentHover: '#02b350',
        btnHoverBg: isDarkMode ? '#333333' : '#f3f4f6'
    };

    return (
        <div style={{
            backgroundColor: theme.bgPrimary,
            minHeight: '100vh',
            padding: '40px 20px',
            fontFamily: "'Noto Sans KR', sans-serif",
            color: theme.textPrimary
        }}>
            <div style={{ maxWidth: '800px', margin: '0 auto' }}>

                {/* ========== 상품 필터 섹션 ========== */}
                <div style={{
                    marginBottom: '30px',
                    padding: '25px',
                    backgroundColor: theme.bgCard,
                    borderRadius: '12px',
                    boxShadow: theme.shadowSmall,
                    border: `1px solid ${theme.border}`
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '15px' }}>
                        <span style={{ fontSize: '1.2rem' }}>🔍</span>
                        <select
                            value={productIdFromUrl || ''}
                            onChange={handleProductChange}
                            style={{
                                padding: '12px 20px',
                                borderRadius: '8px',
                                border: `1px solid ${theme.border}`,
                                minWidth: '300px',
                                fontSize: '1rem',
                                outline: 'none',
                                appearance: 'none',
                                backgroundColor: theme.bgInput,
                                color: theme.textPrimary,
                                cursor: 'pointer'
                            }}
                        >
                            <option value="">전체 리뷰 보기</option>
                            {products.map(p => (
                                <option key={p.id} value={p.id}>{p.name}</option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* ========== 리뷰 작성 폼 ========== */}
                {orderIdFromUrl && orderIdFromUrl !== 'undefined' ? (
                    <div style={{
                        padding: '30px',
                        backgroundColor: theme.accentBg,
                        borderRadius: '12px',
                        border: `2px solid ${theme.accentBorder}`,
                        marginBottom: '40px',
                        boxShadow: theme.shadow
                    }}>
                        <h3 style={{
                            marginTop: 0,
                            marginBottom: '20px',
                            textAlign: 'center',
                            color: theme.textAccent
                        }}>
                            ✍️ 상품 리뷰 작성
                        </h3>
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '15px',
                                padding: '10px',
                                backgroundColor: theme.bgCard,
                                borderRadius: '8px',
                                border: `1px solid ${theme.border}`
                            }}>
                                <span style={{ fontWeight: 'bold', color: theme.textSecondary }}>평점 선택</span>
                                <select
                                    value={rating}
                                    onChange={(e) => setRating(Number(e.target.value))}
                                    style={{
                                        border: `1px solid ${theme.border}`,
                                        fontSize: '1rem',
                                        fontWeight: 'bold',
                                        color: theme.textAccent,
                                        cursor: 'pointer',
                                        outline: 'none',
                                        backgroundColor: theme.bgInput,
                                        padding: '6px 10px',
                                        borderRadius: '6px'
                                    }}
                                >
                                    {[5, 4, 3, 2, 1].map(n => (
                                        <option key={n} value={n}>{n}점 {"⭐".repeat(n)}</option>
                                    ))}
                                </select>
                            </div>
                            <textarea
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                placeholder="상품에 대한 솔직한 후기를 작성해주세요."
                                style={{
                                    height: '140px',
                                    padding: '15px',
                                    borderRadius: '8px',
                                    border: `1px solid ${theme.border}`,
                                    fontSize: '1rem',
                                    lineHeight: '1.6',
                                    outline: 'none',
                                    resize: 'none',
                                    backgroundColor: theme.bgCard,
                                    color: theme.textPrimary
                                }}
                            />
                            <button
                                type="submit"
                                style={{
                                    backgroundColor: theme.accentColor,
                                    color: '#ffffff',
                                    border: 'none',
                                    padding: '15px',
                                    cursor: 'pointer',
                                    borderRadius: '8px',
                                    fontWeight: 'bold',
                                    fontSize: '1.1rem',
                                    transition: 'all 0.2s ease'
                                }}
                                onMouseEnter={(e) => e.target.style.backgroundColor = theme.accentHover}
                                onMouseLeave={(e) => e.target.style.backgroundColor = theme.accentColor}
                            >
                                리뷰 등록 완료
                            </button>
                        </form>
                    </div>
                ) : (
                    <div style={{
                        textAlign: 'center',
                        padding: '20px',
                        backgroundColor: theme.bgCard,
                        borderRadius: '12px',
                        marginBottom: '30px',
                        border: `1px dashed ${theme.border}`,
                        color: theme.textSecondary
                    }}>
                        <p style={{ margin: 0, fontSize: '0.95rem' }}>
                            💡 <strong>주문 내역</strong>에서 리뷰 작성 버튼을 클릭하면 리뷰를 남길 수 있습니다.
                        </p>
                    </div>
                )}

                {/* ========== 리뷰 리스트 출력 ========== */}
                <div style={{ marginTop: '40px' }}>
                    <h2 style={{
                        textAlign: 'center',
                        fontSize: '1.8rem',
                        color: theme.textPrimary,
                        marginBottom: '30px',
                        fontWeight: '700'
                    }}>
                        {productIdFromUrl
                            ? `📦 해당 상품의 후기 (${reviews.length})`
                            : `📢 전체 고객 후기 (${reviews.length})`
                        }
                    </h2>

                    {reviews.length > 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                            {reviews.map(r => (
                                <div
                                    key={r.id}
                                    style={{
                                        padding: '25px',
                                        backgroundColor: theme.bgCard,
                                        borderRadius: '12px',
                                        boxShadow: theme.shadowSmall,
                                        border: `1px solid ${theme.border}`
                                    }}
                                >
                                    {editingReviewId === r.id ? (
                                        // ✅ 수정 모드 UI
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <span style={{ fontWeight: 'bold', color: theme.textPrimary }}>리뷰 수정 중...</span>
                                                <select
                                                    value={editRating}
                                                    onChange={(e) => setEditRating(Number(e.target.value))}
                                                    style={{
                                                        backgroundColor: theme.bgInput,
                                                        padding: '5px 10px',
                                                        borderRadius: '6px',
                                                        border: `1px solid ${theme.border}`,
                                                        color: theme.textAccent,
                                                        fontWeight: 'bold',
                                                        cursor: 'pointer',
                                                        outline: 'none'
                                                    }}
                                                >
                                                    {[5, 4, 3, 2, 1].map(n => (
                                                        <option key={n} value={n}>{n}점 {"⭐".repeat(n)}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            <textarea
                                                value={editContent}
                                                onChange={(e) => setEditContent(e.target.value)}
                                                style={{
                                                    height: '100px',
                                                    border: '2px solid #3b82f6',
                                                    padding: '15px',
                                                    borderRadius: '8px',
                                                    fontSize: '1rem',
                                                    lineHeight: '1.6',
                                                    outline: 'none',
                                                    resize: 'none',
                                                    backgroundColor: theme.bgInput,
                                                    color: theme.textPrimary
                                                }}
                                            />
                                            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                                                <button
                                                    onClick={cancelEdit}
                                                    style={{
                                                        color: theme.textSecondary,
                                                        borderColor: theme.border,
                                                        borderRadius: '6px',
                                                        padding: '6px 14px',
                                                        background: 'none',
                                                        border: `1px solid ${theme.border}`,
                                                        cursor: 'pointer',
                                                        fontSize: '0.85rem',
                                                        fontWeight: '500',
                                                        transition: 'all 0.2s ease'
                                                    }}
                                                    onMouseEnter={(e) => {
                                                        e.target.style.backgroundColor = theme.btnHoverBg;
                                                    }}
                                                    onMouseLeave={(e) => {
                                                        e.target.style.backgroundColor = 'transparent';
                                                    }}
                                                >
                                                    취소
                                                </button>
                                                <button
                                                    onClick={() => handleUpdate(r.id)}
                                                    style={{
                                                        backgroundColor: '#3b82f6',
                                                        color: '#ffffff',
                                                        padding: '8px 20px',
                                                        fontSize: '0.9rem',
                                                        border: 'none',
                                                        borderRadius: '6px',
                                                        fontWeight: '600',
                                                        cursor: 'pointer',
                                                        transition: 'all 0.2s ease'
                                                    }}
                                                    onMouseEnter={(e) => e.target.style.backgroundColor = '#2563eb'}
                                                    onMouseLeave={(e) => e.target.style.backgroundColor = '#3b82f6'}
                                                >
                                                    수정 완료
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        // ✅ 일반 모드 UI
                                        <>
                                            <div style={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'flex-start',
                                                marginBottom: '15px'
                                            }}>
                                                <div>
                                                    <div style={{
                                                        fontSize: '1.1rem',
                                                        fontWeight: 'bold',
                                                        color: theme.textPrimary
                                                    }}>
                                                        {r.userName}
                                                    </div>
                                                    <div style={{
                                                        fontSize: '0.85rem',
                                                        color: theme.textSecondary,
                                                        marginTop: '2px'
                                                    }}>
                                                        {r.productName}
                                                    </div>
                                                </div>
                                                <div style={{
                                                    display: 'flex',
                                                    flexDirection: 'column',
                                                    alignItems: 'flex-end',
                                                    gap: '5px'
                                                }}>
                                                    <div style={{
                                                        color: theme.textRating,
                                                        fontSize: '1.1rem'
                                                    }}>
                                                        {"⭐".repeat(r.rating)}
                                                    </div>
                                                    <div style={{
                                                        fontSize: '0.8rem',
                                                        color: theme.textSecondary
                                                    }}>
                                                        {new Date(r.createdAt).toLocaleDateString()}
                                                    </div>
                                                </div>
                                            </div>
                                            <div style={{
                                                padding: '15px',
                                                backgroundColor: theme.bgInput,
                                                borderRadius: '8px',
                                                color: theme.textPrimary,
                                                lineHeight: '1.7',
                                                whiteSpace: 'pre-wrap'
                                            }}>
                                                {r.content}
                                            </div>
                                            <div style={{
                                                textAlign: 'right',
                                                marginTop: '10px',
                                                display: 'flex',
                                                justifyContent: 'flex-end',
                                                gap: '10px'
                                            }}>
                                                <button
                                                    onClick={() => startEdit(r)}
                                                    style={{
                                                        color: '#3b82f6',
                                                        borderColor: '#3b82f6',
                                                        borderRadius: '6px',
                                                        padding: '6px 14px',
                                                        background: 'none',
                                                        border: '1px solid #3b82f6',
                                                        cursor: 'pointer',
                                                        fontSize: '0.85rem',
                                                        fontWeight: '500',
                                                        transition: 'all 0.2s ease'
                                                    }}
                                                    onMouseEnter={(e) => {
                                                        e.target.style.backgroundColor = '#3b82f6';
                                                        e.target.style.color = '#ffffff';
                                                    }}
                                                    onMouseLeave={(e) => {
                                                        e.target.style.backgroundColor = 'transparent';
                                                        e.target.style.color = '#3b82f6';
                                                    }}
                                                >
                                                    수정
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(r.id)}
                                                    style={{
                                                        color: '#ef4444',
                                                        border: '1px solid #fca5a5',
                                                        borderRadius: '6px',
                                                        padding: '6px 14px',
                                                        background: 'none',
                                                        cursor: 'pointer',
                                                        fontSize: '0.85rem',
                                                        fontWeight: '500',
                                                        transition: 'all 0.2s ease'
                                                    }}
                                                    onMouseEnter={(e) => {
                                                        e.target.style.backgroundColor = '#fee2e2';
                                                    }}
                                                    onMouseLeave={(e) => {
                                                        e.target.style.backgroundColor = 'transparent';
                                                    }}
                                                >
                                                    삭제
                                                </button>
                                            </div>
                                        </>
                                    )}
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div style={{
                            textAlign: 'center',
                            padding: '60px 20px',
                            color: theme.textSecondary,
                            backgroundColor: theme.bgCard,
                            borderRadius: '12px',
                            border: `1px solid ${theme.border}`,
                            fontSize: '1.1rem'
                        }}>
                            아직 작성된 리뷰가 없습니다.
                        </div>
                    )}
                </div>

                {/* ========== 홈으로 가기 버튼 섹션 ========== */}
                <div style={{ textAlign: 'center', marginTop: '50px', marginBottom: '50px' }}>
                    <button
                        onClick={() => navigate('/')}  // ← 클릭 시 홈('/')으로 이동
                        style={{
                            padding: '12px 40px',              // 상하 12px, 좌우 40px 여백
                            borderRadius: '8px',                // 모서리 둥근 정도 (8px)
                            border: `2px solid ${theme.accentColor}`,  // 테두리 2px, 초록색
                            cursor: 'pointer',                  // 마우스 올리면 손가락 모양
                            backgroundColor: 'transparent',     // 배경은 투명 (테두리만 보임)
                            color: theme.accentColor,           // 텍스트 색상 (초록색)
                            fontSize: '1rem',                   // 글자 크기
                            fontWeight: '700',                  // 글자 굵기 (굵음)
                            transition: 'all 0.2s ease'         // 0.2초 동안 부드럽게 변화
                        }}
                        onMouseEnter={(e) => {  // 마우스 올릴 때
                            e.target.style.backgroundColor = theme.accentColor;  // 배경을 초록색으로
                            e.target.style.color = '#ffffff';                     // 텍스트를 흰색으로
                        }}
                        onMouseLeave={(e) => {  // 마우스 벗어날 때
                            e.target.style.backgroundColor = 'transparent';   // 배경을 투명으로
                            e.target.style.color = theme.accentColor;         // 텍스트를 초록색으로
                        }}
                    >
                        🏠 홈으로 돌아가기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ReviewManagementPage;
