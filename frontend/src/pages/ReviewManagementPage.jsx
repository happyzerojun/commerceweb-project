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

    // ✅ 수정 로직을 위한 상태 추가
    const [editingReviewId, setEditingReviewId] = useState(null);
    const [editContent, setEditContent] = useState('');
    const [editRating, setEditRating] = useState(5);

    useEffect(() => {
        fetchProducts();
        fetchReviews();
    }, [productIdFromUrl]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/products');
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
                res = await api.get(`/reviews/product/${productIdFromUrl}`);
            } else {
                res = await api.get('/reviews');
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
            await api.post('/reviews', reviewData);
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
            await api.put(`/reviews/${reviewId}`, {
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
            await api.delete(`/reviews/${reviewId}`);
            alert("삭제되었습니다.");
            fetchReviews();
        } catch (err) {
            alert("본인의 리뷰만 삭제할 수 있습니다.");
        }
    };

    if (loading) return <div style={{textAlign: 'center', marginTop: '100px', fontSize: '1.2rem', color: '#666'}}>✨ 데이터를 불러오는 중입니다...</div>;

    return (
        <div style={{ backgroundColor: '#f9fafb', minHeight: '100vh', padding: '40px 20px' }}>
            <div style={{ maxWidth: '800px', margin: '0 auto' }}>

                {/* 1. 상품 필터 섹션 */}
                <div style={filterContainerStyle}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '15px' }}>
                        <span style={{ fontSize: '1.2rem' }}>🔍</span>
                        <select
                            value={productIdFromUrl || ''}
                            onChange={handleProductChange}
                            style={selectStyle}
                        >
                            <option value="">전체 리뷰 보기</option>
                            {products.map(p => (
                                <option key={p.id} value={p.id}>{p.name}</option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* 2. 리뷰 작성 폼 */}
                {orderIdFromUrl && orderIdFromUrl !== 'undefined' ? (
                    <div style={writeBoxStyle}>
                        <h3 style={{ marginTop: 0, marginBottom: '20px', textAlign: 'center', color: '#065f46' }}>✍️ 상품 리뷰 작성</h3>
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '15px', padding: '10px', backgroundColor: '#fff', borderRadius: '10px' }}>
                                <span style={{ fontWeight: 'bold', color: '#374151' }}>평점 선택</span>
                                <select value={rating} onChange={(e) => setRating(Number(e.target.value))} style={ratingSelectStyle}>
                                    {[5,4,3,2,1].map(n => <option key={n} value={n}>{n}점 {"⭐".repeat(n)}</option>)}
                                </select>
                            </div>
                            <textarea
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                placeholder="상품에 대한 솔직한 후기를 작성해주세요."
                                style={textareaStyle}
                            />
                            <button type="submit" style={submitBtnStyle}>리뷰 등록 완료</button>
                        </form>
                    </div>
                ) : (
                    <div style={infoBoxStyle}>
                        <p style={{ margin: 0, color: '#4b5563', fontSize: '0.95rem' }}>
                            💡 <strong>주문 내역</strong>에서 리뷰 작성 버튼을 클릭하면 리뷰를 남길 수 있습니다.
                        </p>
                    </div>
                )}

                {/* 3. 리뷰 리스트 출력 */}
                <div style={{ marginTop: '40px' }}>
                    <h2 style={{ textAlign: 'center', fontSize: '1.8rem', color: '#111827', marginBottom: '30px' }}>
                        {productIdFromUrl ? `📦 해당 상품의 후기 (${reviews.length})` : `📢 전체 고객 후기 (${reviews.length})`}
                    </h2>

                    {reviews.length > 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                            {reviews.map(r => (
                                <div key={r.id} style={reviewCardStyle}>
                                    {editingReviewId === r.id ? (
                                        // ✅ 수정 모드 UI
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <span style={{ fontWeight: 'bold' }}>리뷰 수정 중...</span>
                                                <select
                                                    value={editRating}
                                                    onChange={(e) => setEditRating(Number(e.target.value))}
                                                    style={{ ...ratingSelectStyle, backgroundColor: '#f3f4f6', padding: '5px 10px', borderRadius: '8px' }}
                                                >
                                                    {[5,4,3,2,1].map(n => <option key={n} value={n}>{n}점 {"⭐".repeat(n)}</option>)}
                                                </select>
                                            </div>
                                            <textarea
                                                value={editContent}
                                                onChange={(e) => setEditContent(e.target.value)}
                                                style={{ ...textareaStyle, height: '100px', border: '2px solid #3b82f6' }}
                                            />
                                            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                                                <button onClick={cancelEdit} style={{ ...deleteBtnStyle, color: '#6b7280', borderColor: '#d1d5db' }}>취소</button>
                                                <button onClick={() => handleUpdate(r.id)} style={{ ...submitBtnStyle, padding: '8px 20px', fontSize: '0.9rem', backgroundColor: '#3b82f6' }}>수정 완료</button>
                                            </div>
                                        </div>
                                    ) : (
                                        // ✅ 일반 모드 UI
                                        <>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '15px' }}>
                                                <div>
                                                    <div style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#111827' }}>{r.userName}</div>
                                                    <div style={{ fontSize: '0.85rem', color: '#6b7280', marginTop: '2px' }}>{r.productName}</div>
                                                </div>
                                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '5px' }}>
                                                    <div style={{ color: '#fbbf24', fontSize: '1.1rem' }}>{"⭐".repeat(r.rating)}</div>
                                                    <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>{new Date(r.createdAt).toLocaleDateString()}</div>
                                                </div>
                                            </div>
                                            <div style={contentBoxStyle}>{r.content}</div>
                                            <div style={{ textAlign: 'right', marginTop: '10px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                                                <button onClick={() => startEdit(r)} style={{ ...deleteBtnStyle, color: '#3b82f6', borderColor: '#3b82f6' }}>수정</button>
                                                <button onClick={() => handleDelete(r.id)} style={deleteBtnStyle}>삭제</button>
                                            </div>
                                        </>
                                    )}
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div style={emptyBoxStyle}>아직 작성된 리뷰가 없습니다.</div>
                    )}
                </div>

                {/* 홈으로 가기 버튼 섹션 */}
                <div style={{ textAlign: 'center', marginTop: '50px', marginBottom: '50px' }}>
                    <button
                        onClick={() => navigate('/')}
                        style={{
                            padding: '12px 40px',
                            borderRadius: '30px',
                            border: '2px solid #00c73c',
                            cursor: 'pointer',
                            backgroundColor: '#fff',
                            color: '#00c73c',
                            fontSize: '1rem',
                            fontWeight: 'bold',
                            transition: 'all 0.2s'
                        }}
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

// --- 스타일 가이드 (기존과 동일) ---
const filterContainerStyle = { marginBottom: '30px', padding: '25px', backgroundColor: '#ffffff', borderRadius: '16px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', border: '1px solid #e5e7eb' };
const selectStyle = { padding: '12px 20px', borderRadius: '12px', border: '1px solid #d1d5db', minWidth: '300px', fontSize: '1rem', outline: 'none', appearance: 'none', backgroundColor: '#f3f4f6' };
const writeBoxStyle = { padding: '30px', backgroundColor: '#ecfdf5', borderRadius: '20px', border: '2px solid #10b981', marginBottom: '40px', boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1)' };
const ratingSelectStyle = { border: 'none', fontSize: '1rem', fontWeight: 'bold', color: '#059669', cursor: 'pointer', outline: 'none' };
const textareaStyle = { height: '140px', padding: '15px', borderRadius: '12px', border: '1px solid #d1d5db', fontSize: '1rem', lineHeight: '1.6', outline: 'none', resize: 'none' };
const submitBtnStyle = { backgroundColor: '#10b981', color: 'white', border: 'none', padding: '15px', cursor: 'pointer', borderRadius: '12px', fontWeight: 'bold', fontSize: '1.1rem', transition: 'background 0.2s' };
const infoBoxStyle = { textAlign: 'center', padding: '20px', backgroundColor: '#fff', borderRadius: '16px', marginBottom: '30px', border: '1px dashed #d1d5db' };
const reviewCardStyle = { padding: '25px', backgroundColor: '#ffffff', borderRadius: '20px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)', border: '1px solid #f3f4f6' };
const contentBoxStyle = { padding: '15px', backgroundColor: '#f9fafb', borderRadius: '12px', color: '#374151', lineHeight: '1.7', whiteSpace: 'pre-wrap' };
const deleteBtnStyle = { color: '#ef4444', border: '1px solid #fca5a5', borderRadius: '8px', padding: '6px 14px', background: 'none', cursor: 'pointer', fontSize: '0.85rem', fontWeight: '500' };
const emptyBoxStyle = { textAlign: 'center', padding: '60px', color: '#9ca3af', backgroundColor: '#fff', borderRadius: '20px', border: '1px solid #f3f4f6', fontSize: '1.1rem' };

export default ReviewManagementPage;