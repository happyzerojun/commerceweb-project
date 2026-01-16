import axios from 'axios';

// 환경 변수에서 API URL을 가져오거나 기본값 사용 (Vite 환경 기준)
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: BASE_URL,
    timeout: 5000, // 5초 이상 응답 없으면 에러 처리 (무한 대기 방지)
    headers: {
        'Content-Type': 'application/json',
        // 필요한 경우 CORS 관련 헤더 추가 (보통은 백엔드 설정으로 충분)
    },
});

// 요청 인터셉터: 토큰 자동 첨부
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 토큰 만료 처리
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // 401 Unauthorized 에러 발생 시 로그아웃 처리
        if (error?.response?.status === 401) {
            console.warn('세션이 만료되어 로그아웃됩니다.');
            localStorage.removeItem('token');
            localStorage.removeItem('user'); // 사용자 정보도 함께 삭제

            // 현재 페이지가 로그인 페이지가 아닐 때만 리다이렉트 (무한 루프 방지)
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default api;
