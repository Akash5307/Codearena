import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The Spring Boot backend has no CORS config, so we proxy /api to it in dev.
// The browser only ever talks to the Vite origin (:5173).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
