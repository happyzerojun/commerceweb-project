// frontend/src/services/authService.js

import axios from 'axios';

const API_URL = 'http://localhost:8080';

export const authService = {
    signup: async (email, password, name, role) => {
        return axios.post(`${API_URL}/signup`, {
            email,
            password,
            name,
            role
        });
    },

    login: async (email, password) => {
        return axios.post(`${API_URL}/login`, {
            email,
            password
        });
    },

    logout: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
};