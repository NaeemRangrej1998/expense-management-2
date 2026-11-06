import axios, { AxiosError, AxiosInstance } from 'axios';
import { ApiError } from '@/types';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // Sends HttpOnly cookies with every request
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let refreshSubscribers: Array<(success: boolean) => void> = [];

function subscribeRefresh(callback: (success: boolean) => void) {
  refreshSubscribers.push(callback);
}

function notifyRefreshSubscribers(success: boolean) {
  refreshSubscribers.forEach((cb) => cb(success));
  refreshSubscribers = [];
}

// Response interceptor: handle 401 with token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as (typeof error.config & { _retry?: boolean }) | undefined;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/')
    ) {
      if (isRefreshing) {
        // Queue the request until refresh completes
        return new Promise((resolve, reject) => {
          subscribeRefresh((success) => {
            if (success) {
              resolve(apiClient(originalRequest));
            } else {
              reject(error);
            }
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await apiClient.post('/auth/refresh');
        isRefreshing = false;
        notifyRefreshSubscribers(true);
        return apiClient(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        notifyRefreshSubscribers(false);
        // Redirect to login
        if (typeof window !== 'undefined') {
          window.location.replace(new URL('/login', window.location.origin).toString());
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export function getApiError(error: unknown): ApiError {
  if (axios.isAxiosError<ApiError>(error)) {
    const responseData = error.response?.data;
    if (responseData && typeof responseData.message === 'string') {
      return responseData;
    }

    if (!error.response) {
      const timedOut = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT';
      return {
        status: 0,
        error: timedOut ? 'API_TIMEOUT' : 'API_UNREACHABLE',
        message: timedOut
          ? 'The application server took too long to respond. Please try again.'
          : 'Could not reach the application server. Your internet may be working; check that the app server or API proxy is running.',
      };
    }
  }

  return { status: 0, error: 'UNKNOWN_ERROR', message: 'Something went wrong. Please try again.' };
}
