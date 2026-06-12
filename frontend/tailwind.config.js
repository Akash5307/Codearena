/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Codeforces-ish palette
        cf: {
          blue: "#3b5998",
          link: "#0000ee",
          green: "#00a900",
          red: "#cc0000",
          band: "#e1e1e1",
          panel: "#f8f8f8",
          border: "#cccccc",
        },
        // rating colors
        rating: {
          newbie: "#808080",
          pupil: "#008000",
          specialist: "#03a89e",
          expert: "#0000ff",
          candidate: "#aa00aa",
          master: "#ff8c00",
          grandmaster: "#ff0000",
        },
        verdict: {
          ac: "#00a900",
          wa: "#cc0000",
          pending: "#8a6d3b",
        },
      },
      fontFamily: {
        sans: ["Verdana", "Arial", "Helvetica", "sans-serif"],
        mono: ["Consolas", "Menlo", "Monaco", "monospace"],
      },
    },
  },
  plugins: [],
};
