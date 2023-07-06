const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "FAIR%20data%20hub";

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

module.exports = {
  "/graphql": {
    target: `${HOST}/${SCHEMA}`,
    ...opts,
  },
  "^/.*/graphql$": {
    target: `${HOST}/${SCHEMA}`,
    changeOrigin: true,
    secure: false,
  },
  "/apps/central/theme.css": {
    target: `${HOST}/${SCHEMA}`,
    changeOrigin: true,
    secure: false,
  },
  /* should match only '/schema_name/graphql', previous ** was to eager also matching if graphql was /graphql or /a/b/graphql */
  "^/[a-zA-Z0-9_.%-]+/graphql": {
    target: `${HOST}`,
    ...opts,
  },
  "/api": { target: `${HOST}`, ...opts },
  "/apps": { target: `${HOST}`, ...opts },
  "/theme.css": { target: `${HOST}/${SCHEMA}`, ...opts },
};
