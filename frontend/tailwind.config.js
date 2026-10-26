/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#F7FAFC', 100: '#EDF2F7', 500: '#2B6CB0', 700: '#1A365D',
        },
      },
    },
  },
  plugins: [],
}
