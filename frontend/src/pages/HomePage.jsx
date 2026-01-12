import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/HomePage.css';

export default function HomePage() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const [isDarkMode, setIsDarkMode] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        setIsDarkMode(mediaQuery.matches);

        const listener = (e) => setIsDarkMode(e.matches);
        mediaQuery.addEventListener('change', listener);
        return () => mediaQuery.removeEventListener('change', listener);
    }, []);

    // ✅ 최소 수정: 색상 간단히
    const theme = {
        bgPrimary: isDarkMode ? '#121212' : '#ffffff',
        bgCard: isDarkMode ? '#1e1e1e' : '#f9f9f9',
        textPrimary: isDarkMode ? '#ffffff' : '#000000',
        textSecondary: isDarkMode ? '#b0b0b0' : '#666666',
        textUser: isDarkMode ? '#000000' : '#000000',  // ✅ 둘 다 검정색
        bgUserName: isDarkMode ? 'transparent' : 'transparent',  // ✅ 배경 제거
        border: isDarkMode ? '#333333' : '#e0e0e0',
        shadow: isDarkMode ? '0 2px 8px rgba(0,0,0,0.6)' : '0 2px 8px rgba(0,0,0,0.08)',
        welcomeBg: isDarkMode ? '#222222' : 'transparent'  // ✅ 라이트는 배경 투명
    };

    return (
        <div className="home-container" style={{ backgroundColor: theme.bgPrimary, color: theme.textPrimary, minHeight: '100vh' }}>
            <header className="home-header" style={{ backgroundColor: theme.bgCard, borderBottom: `1px solid ${theme.border}` }}>
                <div className="header-content">
                    <h1 style={{ color: theme.textPrimary }}>🛒 영무마켓</h1>
                    <div className="header-right">
                        {/* ✅ 텍스트색만 검정색으로 */}
                        <span
                            className="user-name"
                            style={{
                                color: theme.textUser,
                                fontWeight: 'bold',
                                backgroundColor: theme.bgUserName,
                                padding: '8px 12px',
                                borderRadius: '6px'
                            }}
                        >
                            👤 {user?.name}님 환영합니다!
                        </span>
                        <button
                            className="logout-btn"
                            onClick={handleLogout}
                            style={{
                                color: theme.textPrimary,
                                borderColor: theme.border,
                                backgroundColor: theme.bgCard
                            }}
                        >
                            로그아웃
                        </button>
                    </div>
                </div>
            </header>

            <main className="home-main">
                <div className="welcome-section" style={{
                    color: theme.textPrimary,
                    backgroundColor: theme.welcomeBg,
                    padding: '40px 20px',
                    borderRadius: '8px',
                    marginBottom: '30px'
                }}>
                    <h2 style={{ color: theme.textPrimary, marginBottom: '10px' }}>환영합니다! 👋</h2>
                    <p className="welcome-subtitle" style={{ color: theme.textSecondary }}>YM MARKET 플랫폼에 로그인했습니다.</p>
                    <div className="user-info-card" style={{
                        backgroundColor: theme.bgCard,
                        border: `1px solid ${theme.border}`,
                        boxShadow: theme.shadow
                    }}>
                        <p><strong style={{ color: theme.textPrimary }}>이름:</strong> <span style={{ color: theme.textSecondary }}>{user?.name}</span></p>
                        <p><strong style={{ color: theme.textPrimary }}>이메일:</strong> <span style={{ color: theme.textSecondary }}>{user?.email}</span></p>
                        <p><strong style={{ color: theme.textPrimary }}>사용자 ID:</strong> <span style={{ color: theme.textSecondary }}>{user?.userId}</span></p>
                    </div>
                </div>

                <div className="features-section" style={{ color: theme.textPrimary }}>
                    <h3 style={{ color: theme.textPrimary }}>📋 주요 기능</h3>
                    <div className="features-grid">
                        <div
                            className="feature-card"
                            onClick={() => navigate('/products')}
                            style={{
                                backgroundColor: theme.bgCard,
                                border: `1px solid ${theme.border}`,
                                boxShadow: theme.shadow
                            }}
                        >
                            <h4 style={{ color: theme.textPrimary }}>🛍️ 상품 조회</h4>
                            <p style={{ color: theme.textSecondary }}>판매 중인 모든 상품을 확인하세요</p>
                        </div>
                        <div
                            className="feature-card"
                            onClick={() => navigate('/orders')}
                            style={{
                                backgroundColor: theme.bgCard,
                                border: `1px solid ${theme.border}`,
                                boxShadow: theme.shadow
                            }}
                        >
                            <h4 style={{ color: theme.textPrimary }}>📦 주문 관리</h4>
                            <p style={{ color: theme.textSecondary }}>구매한 상품의 주문 현황을 확인하세요</p>
                        </div>
                        <div
                            className="feature-card"
                            onClick={() => navigate('/sales-stats')}
                            style={{
                                backgroundColor: theme.bgCard,
                                border: `1px solid ${theme.border}`,
                                boxShadow: theme.shadow
                            }}
                        >
                            <h4 style={{ color: theme.textPrimary }}>💰 총 매출 확인</h4>
                            <p style={{ color: theme.textSecondary }}>모든 판매자의 매출을 훔쳐보세요.</p>
                        </div>
                        <div
                            className="feature-card"
                            onClick={() => navigate('/review-management')}
                            style={{
                                backgroundColor: theme.bgCard,
                                border: `1px solid ${theme.border}`,
                                boxShadow: theme.shadow
                            }}
                        >
                            <h4 style={{ color: theme.textPrimary }}>⭐ 리뷰 관리</h4>
                            <p style={{ color: theme.textSecondary }}>영무스토어 고객님들이 증명하는 리얼 리뷰</p>
                        </div>
                    </div>
                </div>
            </main>

            <footer className="home-footer" style={{
                backgroundColor: theme.bgCard,
                color: theme.textSecondary,
                borderTop: `1px solid ${theme.border}`
            }}>
                <p>© 2026 YM MARKET.</p>
            </footer>
        </div>
    );
}
