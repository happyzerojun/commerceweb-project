import React, { useState } from 'react';
// 1. Link 컴포넌트 추가 임포트
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import '../styles/AuthPages.css';

export default function LoginPage() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [formData, setFormData] = useState({
        email: '',
        password: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await authService.login(
                formData.email,
                formData.password
            );

            // ✅ 수정: 백엔드에서 받은 데이터에서 role도 꺼내야 합니다!
            // const { token, userId, email, name } = response.data; <-- 기존 코드 (role 누락)

            // 🔥 수정된 코드: role을 포함해서 가져오거나, 통째로 넘기기
            const { token, userId, email, name, role } = response.data;

            // role을 포함해서 저장
            login({ userId, email, name, role }, token);

            navigate('/');

        } catch (err) {
            const errorMessage = err.response?.data?.message || '로그인 실패했습니다.';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };


    return (
        <div className="auth-container">
            <div className="auth-card">
                <h1>로그인</h1>

                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="email">이메일</label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                            placeholder="example@gmail.com"
                            disabled={loading}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">비밀번호</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            placeholder="••••••••"
                            disabled={loading}
                        />
                    </div>

                    <button type="submit" disabled={loading}>
                        {loading ? '로그인 중...' : '로그인'}
                    </button>
                </form>

                {/* 2. <a> 태그 대신 <Link> 컴포넌트 사용 (SPA 방식 이동) */}
                <p className="auth-link">
                    계정이 없으신가요? <Link to="/signup">회원가입</Link>
                </p>
            </div>
        </div>
    );
}