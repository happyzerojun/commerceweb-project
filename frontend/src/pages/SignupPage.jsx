import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import '../styles/AuthPages.css';

export default function SignupPage() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [formData, setFormData] = useState({
        email: '',
        password: '',
        name: '',
        role: 'CUSTOMER'
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
            const response = await authService.signup(
                formData.email,
                formData.password,
                formData.name,
                formData.role
            );

            const { token, userId, email, name, role } = response.data;
            login({ userId, email, name, role }, token);
            navigate('/');

        } catch (err) {
            const errorMessage = err.response?.data?.message || '회원가입 실패했습니다.';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h1>회원가입</h1>

                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="name">이름</label>
                        <input
                            type="text"
                            id="name"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            required
                            placeholder="홍길동"
                            disabled={loading}
                        />
                    </div>

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

                    {/* ✅ 가입 유형 섹션: 깨짐 방지를 위해 스타일 보정 */}
                    <div className="form-group">
                        <label>가입 유형</label>
                        <div style={{
                            display: 'flex',
                            justifyContent: 'center',
                            gap: '20px',
                            marginTop: '8px',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '5px',
                            backgroundColor: '#fafafa'
                        }}>
                            <label style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                whiteSpace: 'nowrap' // 글자 잘림 방지
                            }}>
                                <input
                                    type="radio"
                                    name="role"
                                    value="CUSTOMER"
                                    checked={formData.role === 'CUSTOMER'}
                                    onChange={handleChange}
                                    style={{ width: 'auto', margin: 0 }} // 크기 고정
                                />
                                <span>구매자</span>
                            </label>
                            <label style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                whiteSpace: 'nowrap' // 글자 잘림 방지
                            }}>
                                <input
                                    type="radio"
                                    name="role"
                                    value="SELLER"
                                    checked={formData.role === 'SELLER'}
                                    onChange={handleChange}
                                    style={{ width: 'auto', margin: 0 }} // 크기 고정
                                />
                                <span>판매자</span>
                            </label>
                        </div>
                    </div>

                    <button type="submit" disabled={loading} style={{ marginTop: '10px' }}>
                        {loading ? '가입 중...' : '회원가입'}
                    </button>
                </form>

                <p className="auth-link">
                    이미 계정이 있으신가요? <a href="/login">로그인</a>
                </p>
            </div>
        </div>
    );
}