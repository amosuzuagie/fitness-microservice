import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
import App from "./App.jsx";
import { store } from "./store/store.js";
import { AuthProvider } from "react-oauth2-code-pkce";
import { authConfig } from "./authConfig.js";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <AuthProvider
      authConfig={authConfig}
      loadingComponent={<div>Loading...</div>}
    >
      <Provider store={store}>
        <App />
      </Provider>
    </AuthProvider>
  </StrictMode>
);
