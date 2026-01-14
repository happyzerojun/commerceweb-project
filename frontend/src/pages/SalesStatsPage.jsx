import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const SalesStatsPage = () => {
    const { user } = useAuth();
    const [stats, setStats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isDarkMode, setIsDarkMode] = useState(false);
    const navigate = useNavigate();

    // 다크모드 감지
    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        setIsDarkMode(mediaQuery.matches);

        const listener = (e) => setIsDarkMode(e.matches);
        mediaQuery.addEventListener('change', listener);

        return () => mediaQuery.removeEventListener('change', listener);
    }, []);

    // 데이터 로딩
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

    // 다크/라이트 모드 통합 테마 객체
    const theme = {
        bgPrimary: isDarkMode ? '#0f0f0f' : '#f8f9fa',
        bgCard: isDarkMode ? '#1a1a1a' : '#ffffff',
        textPrimary: isDarkMode ? '#ffffff' : '#2c3e50',
        textSecondary: isDarkMode ? '#b0b0b0' : '#888888',
        textPrice: isDarkMode ? '#4fc3f7' : '#ff4d4f',
        textRating: isDarkMode ? '#ffd700' : '#f1c40f',
        border: isDarkMode ? '#333333' : '#e0e0e0',
        shadow: isDarkMode
            ? '0 4px 12px rgba(0,0,0,0.3)'
            : '0 4px 12px rgba(0,0,0,0.08)',
        shadowSmall: isDarkMode
            ? '0 2px 6px rgba(0,0,0,0.2)'
            : '0 2px 6px rgba(0,0,0,0.05)',
        accentColor: '#03C75A',
        accentHover: '#02b350'
    };

    return (
        <div style={{
            padding: '20px',
            backgroundColor: theme.bgPrimary,
            minHeight: '100vh',
            fontFamily: "'Noto Sans KR', sans-serif",
            color: theme.textPrimary
        }}>
            <div style={{ maxWidth: '900px', margin: '0 auto' }}>

                {/* ========== 제목 ========== */}
                <h2 style={{
                    textAlign: 'center',
                    margin: '20px 0 30px',
                    color: theme.textPrimary,
                    fontSize: '1.6rem',
                    fontWeight: '700'
                }}>
                    📊 판매 통계 대시보드
                </h2>

                {/* ========== 누적 매출액 카드 ========== */}
                <div style={{ marginBottom: '30px' }}>
                    <div style={{
                        backgroundColor: theme.bgCard,
                        padding: '25px',
                        borderRadius: '12px',
                        boxShadow: theme.shadowSmall,
                        textAlign: 'center',
                        borderTop: `6px solid ${theme.accentColor}`,
                        border: `1px solid ${theme.border}`,
                        borderTopWidth: '6px'
                    }}>
                        <p style={{
                            color: theme.textSecondary,
                            fontSize: '1rem',
                            marginBottom: '8px',
                            fontWeight: '500'
                        }}>
                            총 누적 매출액
                        </p>
                        <h1 style={{
                            color: theme.accentColor,
                            margin: '0',
                            fontSize: '2.4rem',
                            fontWeight: '800'
                        }}>
                            {loading ? "..." : `${totalRevenue.toLocaleString()}원`}
                        </h1>
                    </div>
                </div>

                {/* ========== 통계 테이블/리스트 영역 ========== */}
                <div className="stats-container">
                    {!loading && stats.length > 0 ? (
                        <>
                            {/* PC용 테이블 (화면이 클 때만 보임) */}
                            <div className="pc-table-view" style={{
                                backgroundColor: theme.bgCard,
                                borderRadius: '12px',
                                boxShadow: theme.shadowSmall,
                                overflow: 'hidden',
                                border: `1px solid ${theme.border}`
                            }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                    <tr style={{
                                        backgroundColor: isDarkMode ? '#252525' : '#f8f9fa',
                                        borderBottom: `2px solid ${theme.border}`
                                    }}>
                                        <th style={{
                                            padding: '18px',
                                            color: theme.textSecondary,
                                            fontWeight: '600',
                                            textAlign: 'left'
                                        }}>
                                            상품명
                                        </th>
                                        <th style={{
                                            padding: '18px',
                                            color: theme.textSecondary,
                                            fontWeight: '600'
                                        }}>
                                            수량
                                        </th>
                                        <th style={{
                                            padding: '18px',
                                            color: theme.textSecondary,
                                            fontWeight: '600'
                                        }}>
                                            합계 금액
                                        </th>
                                        <th style={{
                                            padding: '18px',
                                            color: theme.textSecondary,
                                            fontWeight: '600'
                                        }}>
                                            평균 별점
                                        </th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {stats.map((item, index) => (
                                        <tr
                                            key={index}
                                            style={{
                                                borderBottom: `1px solid ${theme.border}`,
                                                textAlign: 'center',
                                                transition: 'background-color 0.2s ease'
                                            }}
                                            onMouseEnter={(e) => e.target.parentElement.style.backgroundColor = isDarkMode ? '#252525' : '#f8f9fa'}
                                            onMouseLeave={(e) => e.target.parentElement.style.backgroundColor = 'transparent'}
                                        >
                                            <td style={{
                                                padding: '18px',
                                                fontWeight: '600',
                                                textAlign: 'left',
                                                paddingLeft: '30px',
                                                color: theme.textPrimary
                                            }}>
                                                {item.productName}
                                            </td>
                                            <td style={{
                                                padding: '18px',
                                                color: theme.textPrimary
                                            }}>
                                                {item.totalQuantity}개
                                            </td>
                                            <td style={{
                                                padding: '18px',
                                                color: theme.textPrice,
                                                fontWeight: 'bold'
                                            }}>
                                                {(item.totalSales || 0).toLocaleString()}원
                                            </td>
                                            <td style={{
                                                padding: '18px',
                                                color: theme.textRating,
                                                fontWeight: '600'
                                            }}>
                                                ★ {item.averageRating?.toFixed(1) || '0.0'}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* 모바일용 카드 뷰 (화면이 작을 때만 보임) */}
                            <div className="mobile-card-view" style={{
                                display: 'none',
                                flexDirection: 'column',
                                gap: '15px'
                            }}>
                                {stats.map((item, index) => (
                                    <div
                                        key={index}
                                        style={{
                                            backgroundColor: theme.bgCard,
                                            padding: '20px',
                                            borderRadius: '12px',
                                            border: `1px solid ${theme.border}`,
                                            boxShadow: theme.shadowSmall
                                        }}>
                                        <div style={{
                                            fontWeight: '700',
                                            fontSize: '1.1rem',
                                            marginBottom: '10px',
                                            color: theme.textPrimary
                                        }}>
                                            {item.productName}
                                        </div>

                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            color: theme.textSecondary,
                                            marginBottom: '8px',
                                            fontSize: '0.9rem'
                                        }}>
                                            <span>판매 수량</span>
                                            <span style={{
                                                fontWeight: '600',
                                                color: theme.textPrimary
                                            }}>
                                                {item.totalQuantity}개
                                            </span>
                                        </div>

                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            color: theme.textSecondary,
                                            marginBottom: '12px',
                                            fontSize: '0.9rem'
                                        }}>
                                            <span>평균 별점</span>
                                            <span style={{
                                                color: theme.textRating,
                                                fontWeight: '600'
                                            }}>
                                                ★ {item.averageRating?.toFixed(1) || '0.0'}
                                            </span>
                                        </div>

                                        <div style={{
                                            marginTop: '10px',
                                            paddingTop: '12px',
                                            borderTop: `1px solid ${theme.border}`,
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center'
                                        }}>
                                            <span style={{
                                                fontSize: '0.9rem',
                                                color: theme.textSecondary
                                            }}>
                                                합계 금액
                                            </span>
                                            <span style={{
                                                fontSize: '1.2rem',
                                                fontWeight: '700',
                                                color: theme.textPrice
                                            }}>
                                                {(item.totalSales || 0).toLocaleString()}원
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    ) : (
                        <div style={{
                            textAlign: 'center',
                            padding: '50px',
                            backgroundColor: theme.bgCard,
                            borderRadius: '12px',
                            color: theme.textSecondary,
                            border: `1px solid ${theme.border}`
                        }}>
                            {loading ? "데이터 로딩 중..." : "판매 데이터가 없습니다."}
                        </div>
                    )}
                </div>

                {/* ========== 홈 버튼 ========== */}
                <div style={{ textAlign: 'center', marginTop: '40px', marginBottom: '30px' }}>
                    <button
                        onClick={() => navigate('/')}
                        style={{
                            padding: '14px 50px',
                            backgroundColor: theme.accentColor,
                            color: theme.accentColor,
                            border: `2px solid ${theme.accentColor}`,
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontSize: '1rem',
                            fontWeight: '700',
                            boxShadow: theme.shadowSmall,
                            transition: 'all 0.2s ease'
                        }}
                        onMouseEnter={(e) => {
                            e.target.style.backgroundColor = theme.accentHover;
                            e.target.style.boxShadow = theme.shadow;
                        }}
                        onMouseLeave={(e) => {
                            e.target.style.backgroundColor = theme.accentColor;
                            e.target.style.boxShadow = theme.shadowSmall;
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
