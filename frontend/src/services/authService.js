// frontend/src/services/authService.js

import api from './api';

// 하드코딩된 http://localhost:8080 대신 공통 axios 인스턴스(api)를 쓴다 (TS-04)

export const authService = {
    signup: async (email, password, name, role) => {
        // ✅ 수정됨: /signup -> /api/auth/signup
        return api.post('/api/auth/signup', {
            email,
            password,
            name,
            role
        });
    },

    login: async (email, password) => {
        // ✅ 수정됨: /login -> /api/auth/login
        return api.post('/api/auth/login', {
            email,
            password
        });
    },

    logout: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
};
