// frontend/src/services/authService.js

import axios from 'axios';

// 백엔드 API 기본 주소 (컨텍스트 패스 포함하지 않음)
const API_URL = 'http://localhost:8080';

export const authService = {
    signup: async (email, password, name, role) => {
        // ✅ 수정됨: /signup -> /api/auth/signup
        return axios.post(`${API_URL}/api/auth/signup`, {
            email,
            password,
            name,
            role
        });
    },

    login: async (email, password) => {
        // ✅ 수정됨: /login -> /api/auth/login
        return axios.post(`${API_URL}/api/auth/login`, {
            email,
            password
        });
    },

    logout: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
};
