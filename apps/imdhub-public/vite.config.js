import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const host = "https://demo-recon4imd.molgeniscloud.org";
const schema = "IMDHub";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

export default defineConfig(() => {
  return {
    css: {
      preprocessorOptions: {
        scss: {
          // @import "src/styles/variables.scss";
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "../molgenis-viz/src/styles/resets.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: "",
    server: {
      proxy: {
        "/api/graphql": {
          target: `${host}/${schema}`,
          ...opts,
        },
        "^/[a-zA-Z0-9_.%-]+/api/graphql": {
          target: host,
          ...opts,
        },
        "/api": {
          target: `${host}/api`,
          ...opts,
        },
        "/graphql": {
          target: `${host}/api/graphql`,
          ...opts,
        },
        "/apps": {
          target: host,
          ...opts,
        },
        "/theme.css": {
          target: `${host}/apps/central`,
          ...opts,
        },
      },
    },
  };
});
