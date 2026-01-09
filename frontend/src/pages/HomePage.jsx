import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/HomePage.css';

export default function HomePage() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div className="home-container">
            <header className="home-header">
                <div className="header-content">
                    <h1>🛒 영무마켓</h1>
                    <div className="header-right">
                        <span className="user-name">👤 {user?.name}님 환영합니다!</span>
                        <button className="logout-btn" onClick={handleLogout}>로그아웃</button>
                    </div>
                </div>
            </header>

            <main className="home-main">
                <div className="welcome-section">
                    <h2>환영합니다! 👋</h2>
                    <p className="welcome-subtitle">YM MARKET 플랫폼에 로그인했습니다.</p>
                    <div className="user-info-card">
                        <p><strong>이름:</strong> {user?.name}</p>
                        <p><strong>이메일:</strong> {user?.email}</p>
                        <p><strong>사용자 ID:</strong> {user?.userId}</p>
                    </div>
                </div>

                <div className="features-section">
                    <h3>📋 주요 기능</h3>
                    <div className="features-grid">
                        <div className="feature-card" onClick={() => navigate('/products')}>
                            <h4>🛍️ 상품 조회</h4>
                            <p>판매 중인 모든 상품을 확인하세요</p>
                        </div>
                        <div className="feature-card" onClick={() => navigate('/orders')}>
                            <h4>📦 주문 관리</h4>
                            <p>구매한 상품의 주문 현황을 확인하세요</p>
                        </div>
                        <div className="feature-card" onClick={() => navigate('/sales-stats')}>
                            <h4>💰 총 매출 확인</h4>
                            <p>모든 판매자의 매출을 훔쳐보세요.</p>
                        </div>
                        <div className="feature-card" onClick={() => navigate('/review-management')}>
                            <h4>⭐ 리뷰 관리</h4>
                            <p>영무스토어 고객님들이 증명하는 리얼 리뷰</p>
                        </div>
                    </div>
                </div>
            </main>

            <footer className="home-footer">
                <p>© 2026 YM MARKET.</p>
            </footer>
        </div>
    );
}