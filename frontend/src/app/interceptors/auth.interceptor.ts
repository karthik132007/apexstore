import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('ecom_token');
  const traceId = 'trace-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now();

  let headers = req.headers.set('X-Trace-Id', traceId);

  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  const cloned = req.clone({ headers });
  return next(cloned);
};
