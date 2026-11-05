# React + Vite

## Case officer workload indicators

The Admin Dashboard counts assigned and in-progress complaints for each officer. Green covers 0–3 active complaints, yellow 4–7, and red 8 or more by default. To change the display thresholds, set positive integers `VITE_CMS_WORKLOAD_YELLOW_FROM` and `VITE_CMS_WORKLOAD_RED_FROM` before starting or building Vite. The red threshold must exceed the yellow threshold. These colors are informational and do not assign complaints.

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
