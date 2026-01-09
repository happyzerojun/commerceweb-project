import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import ProductPage from './pages/ProductPage';
import OrderListPage from './pages/OrderListPage';
import SalesStatsPage from './pages/SalesStatsPage';
import ReviewManagementPage from './pages/ReviewManagementPage';
import './App.css';

// ✅ 보호된 라우트 설정
function ProtectedRoute({ children }) {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }
    return children;
}

function AppRoutes() {
    const { isAuthenticated } = useAuth();

    return (
        <Routes>
            {/* 1. 인증 관련 라우트 */}
            <Route
                path="/login"
                element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
            />
            <Route
                path="/signup"
                element={isAuthenticated ? <Navigate to="/" replace /> : <SignupPage />}
            />

            {/* 2. 서비스 관련 보호된 라우트 */}
            <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
            <Route path="/products" element={<ProtectedRoute><ProductPage /></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute><OrderListPage /></ProtectedRoute>} />
            <Route path="/sales-stats" element={<ProtectedRoute><SalesStatsPage /></ProtectedRoute>} />

            {/* ✅ 리뷰 관련 라우트 (두 경로 모두 ReviewManagementPage로 연결) */}
            <Route
                path="/reviews"
                element={<ProtectedRoute><ReviewManagementPage /></ProtectedRoute>}
            />
            <Route
                path="/review-management"
                element={<ProtectedRoute><ReviewManagementPage /></ProtectedRoute>}
            />

            {/* 3. ❗ 중요: 와일드카드 라우트는 반드시 맨 마지막에 하나만 있어야 함 */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <Router>
                <AppRoutes />
            </Router>
        </AuthProvider>
    );
}