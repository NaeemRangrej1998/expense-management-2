import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  async rewrites() {
    const apiTarget = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080';
    return [
      {
        source: '/api/:path*',
        destination: `${apiTarget}/api/:path*`,
      },
    ];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
