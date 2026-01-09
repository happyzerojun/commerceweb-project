import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const SalesStatsPage = () => {
    const { user } = useAuth();
    const [stats, setStats] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        if (!user || user.role !== 'SELLER') {
            alert("⚠️ 판매자 전용 페이지입니다. 구매자는 접근할 수 없습니다.");
            navigate('/');
            return;
        }

        api.get('/api/sales/stats')
            .then(response => {
                setStats(response.data || []);
                setLoading(false);
            })
            .catch(error => {
                console.error("데이터 로딩 에러:", error);
                setLoading(false);
            });
    }, [user, navigate]);

    const totalRevenue = useMemo(() => {
        return stats.reduce((acc, curr) => acc + (Number(curr.totalSales) || 0), 0);
    }, [stats]);

    if (!user || user.role !== 'SELLER') return null;

    return (
        <div style={{ padding: '20px', backgroundColor: '#f8f9fa', minHeight: '100vh', fontFamily: "'Noto Sans KR', sans-serif" }}>
            <div style={{ maxWidth: '900px', margin: '0 auto' }}>

                <h2 style={{ textAlign: 'center', margin: '20px 0 30px', color: '#2c3e50', fontSize: '1.6rem' }}>📊 판매 통계 대시보드</h2>

                {/* 누적 매출액 카드 */}
                <div style={{ marginBottom: '30px' }}>
                    <div style={{
                        backgroundColor: '#fff',
                        padding: '25px',
                        borderRadius: '24px',
                        boxShadow: '0 8px 24px rgba(0,0,0,0.05)',
                        textAlign: 'center',
                        borderTop: '6px solid #00c73c'
                    }}>
                        <p style={{ color: '#888', fontSize: '1rem', marginBottom: '8px' }}>총 누적 매출액</p>
                        <h1 style={{ color: '#00c73c', margin: '0', fontSize: '2.4rem', fontWeight: '800' }}>
                            {loading ? "..." : `${totalRevenue.toLocaleString()}원`}
                        </h1>
                    </div>
                </div>

                {/* 통계 테이블/리스트 영역 */}
                <div className="stats-container">
                    {!loading && stats.length > 0 ? (
                        <>
                            {/* PC용 테이블 (화면이 클 때만 보임) */}
                            <div className="pc-table-view" style={{ backgroundColor: '#fff', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr style={{ backgroundColor: '#f8f9fa', borderBottom: '2px solid #edf2f7' }}>
                                            <th style={{ padding: '18px', color: '#4a5568' }}>상품명</th>
                                            <th style={{ padding: '18px', color: '#4a5568' }}>수량</th>
                                            <th style={{ padding: '18px', color: '#4a5568' }}>합계 금액</th>
                                            <th style={{ padding: '18px', color: '#4a5568' }}>평균 별점</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {stats.map((item, index) => (
                                            <tr key={index} style={{ borderBottom: '1px solid #f1f1f1', textAlign: 'center' }}>
                                                <td style={{ padding: '18px', fontWeight: '700', textAlign: 'left', paddingLeft: '30px' }}>{item.productName}</td>
                                                <td style={{ padding: '18px' }}>{item.totalQuantity}개</td>
                                                <td style={{ padding: '18px', color: '#ff4d4f', fontWeight: 'bold' }}>{(item.totalSales || 0).toLocaleString()}원</td>
                                                <td style={{ padding: '18px', color: '#f1c40f' }}>★ {item.averageRating?.toFixed(1) || '0.0'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* 모바일용 카드 뷰 (화면이 작을 때만 보임) */}
                            <div className="mobile-card-view" style={{ display: 'none', flexDirection: 'column', gap: '15px' }}>
                                {stats.map((item, index) => (
                                    <div key={index} style={{ backgroundColor: '#fff', padding: '20px', borderRadius: '16px', border: '1px solid #eee' }}>
                                        <div style={{ fontWeight: '800', fontSize: '1.1rem', marginBottom: '10px', color: '#333' }}>{item.productName}</div>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#666', marginBottom: '5px' }}>
                                            <span>판매 수량</span>
                                            <span style={{ fontWeight: 'bold', color: '#333' }}>{item.totalQuantity}개</span>
                                        </div>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#666', marginBottom: '5px' }}>
                                            <span>평균 별점</span>
                                            <span style={{ color: '#f1c40f', fontWeight: 'bold' }}>★ {item.averageRating?.toFixed(1) || '0.0'}</span>
                                        </div>
                                        <div style={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px dashed #eee', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <span style={{ fontSize: '0.9rem', color: '#888' }}>합계 금액</span>
                                            <span style={{ fontSize: '1.2rem', fontWeight: '800', color: '#ff4d4f' }}>{(item.totalSales || 0).toLocaleString()}원</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    ) : (
                        <div style={{ textAlign: 'center', padding: '50px', backgroundColor: '#fff', borderRadius: '15px', color: '#bbb' }}>
                            {loading ? "데이터 로딩 중..." : "판매 데이터가 없습니다."}
                        </div>
                    )}
                </div>

                {/* 홈 버튼 */}
                <div style={{ textAlign: 'center', marginTop: '40px' }}>
                    <button
                        onClick={() => navigate('/')}
                        style={{
                            padding: '14px 50px',
                            backgroundColor: '#333',
                            color: '#fff',
                            border: 'none',
                            borderRadius: '30px',
                            cursor: 'pointer',
                            fontSize: '1rem',
                            fontWeight: 'bold',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                        }}
                    >
                        🏠 홈으로 돌아가기
                    </button>
                </div>
            </div>

            {/* 반응형 스타일 주입 */}
            <style>{`
                @media (max-width: 650px) {
                    .pc-table-view { display: none !important; }
                    .mobile-card-view { display: flex !important; }
                    h1 { font-size: 2rem !important; }
                }
            `}</style>
        </div>
    );
};

export default SalesStatsPage;