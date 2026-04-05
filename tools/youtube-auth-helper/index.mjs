import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { chromium } from "playwright";

const DEFAULT_OUTPUT_DIR = path.resolve(process.cwd(), "output");
const DEFAULT_TARGET_URL = "https://www.youtube.com/watch?v=aqz-KE-bpKQ";
const DEFAULT_PROFILE_DIR = path.resolve(process.cwd(), ".profile");

function parseArgs(argv) {
  const options = {
    outputDir: DEFAULT_OUTPUT_DIR,
    targetUrl: DEFAULT_TARGET_URL,
    profileDir: DEFAULT_PROFILE_DIR,
    headless: false,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--output-dir") options.outputDir = path.resolve(argv[++i]);
    else if (arg === "--target-url") options.targetUrl = argv[++i];
    else if (arg === "--profile-dir") options.profileDir = path.resolve(argv[++i]);
    else if (arg === "--headless") options.headless = true;
    else if (arg === "--help") {
      printHelp();
      process.exit(0);
    }
  }

  return options;
}

function printHelp() {
  console.log(`youtube-auth-helper

Usage:
  npm install
  npm start -- --target-url "https://www.youtube.com/watch?v=aqz-KE-bpKQ"

Options:
  --target-url   YouTube watch URL to load while capturing auth data
  --output-dir   Directory where cookies.txt and po_token.txt are written
  --profile-dir  Persistent browser profile directory
  --headless     Run without showing the browser window
`);
}

function toNetscapeCookieLine(cookie) {
  const domain = cookie.domain.startsWith(".") ? cookie.domain : `.${cookie.domain}`;
  const includeSubdomains = domain.startsWith(".") ? "TRUE" : "FALSE";
  const secure = cookie.secure ? "TRUE" : "FALSE";
  const expires = cookie.expires && cookie.expires > 0 ? Math.floor(cookie.expires) : 0;
  return [
    domain,
    includeSubdomains,
    cookie.path || "/",
    secure,
    expires,
    cookie.name,
    cookie.value,
  ].join("\t");
}

async function writeCookiesFile(context, outputDir) {
  const cookies = await context.cookies();
  const youtubeCookies = cookies.filter((cookie) => {
    return cookie.domain.includes("youtube.com") || cookie.domain.includes("google.com");
  });

  const filePath = path.join(outputDir, "cookies.txt");
  const contents = [
    "# Netscape HTTP Cookie File",
    ...youtubeCookies.map(toNetscapeCookieLine),
    "",
  ].join("\n");
  await fs.writeFile(filePath, contents, "utf8");
  return { filePath, contents };
}

async function waitForPoToken(page, targetUrl) {
  let resolvedAuth = null;

  page.on("request", (request) => {
    if (resolvedAuth) return;
    if (!request.url().includes("/youtubei/v1/player")) return;

    const postData = request.postData();
    if (!postData) return;

    try {
      const payload = JSON.parse(postData);
      const token = payload?.serviceIntegrityDimensions?.poToken;
      const clientName = payload?.context?.client?.clientName;
      if (typeof token === "string" && token.trim().length > 0) {
        resolvedAuth = {
          poToken: token.trim(),
          poTokenClientHint: inferClientHint(clientName),
          clientName: typeof clientName === "string" ? clientName : null,
        };
      }
    } catch {
      // Ignore malformed/non-JSON payloads.
    }
  });

  await page.goto(targetUrl, { waitUntil: "domcontentloaded" });

  console.log("");
  console.log("Browser launched.");
  console.log("1. Log into YouTube in this browser window if needed.");
  console.log("2. Press play on the loaded video, or open another playable YouTube video.");
  console.log("3. Wait for this helper to detect a PO token.");
  console.log("");

  const startedAt = Date.now();
  while (!resolvedAuth) {
    await page.waitForTimeout(500);
    if (Date.now() - startedAt > 5 * 60 * 1000) {
      throw new Error("Timed out waiting for a YouTube player request containing serviceIntegrityDimensions.poToken");
    }
  }

  return resolvedAuth;
}

function inferClientHint(clientName) {
  const normalized = typeof clientName === "string" ? clientName.toUpperCase() : "";
  if (normalized.includes("MWEB")) return "mweb.gvs";
  return "web.gvs";
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  await fs.mkdir(options.outputDir, { recursive: true });
  await fs.mkdir(options.profileDir, { recursive: true });

  const context = await chromium.launchPersistentContext(options.profileDir, {
    headless: options.headless,
    viewport: { width: 1366, height: 900 },
  });

  try {
    const page = context.pages()[0] ?? (await context.newPage());
    const authData = await waitForPoToken(page, options.targetUrl);
    const { filePath: cookiesFilePath, contents: cookiesContent } = await writeCookiesFile(context, options.outputDir);
    const poTokenPath = path.join(options.outputDir, "po_token.txt");
    const summaryPath = path.join(options.outputDir, "auth_bundle.json");

    await fs.writeFile(poTokenPath, `${authData.poToken}\n`, "utf8");
    await fs.writeFile(
      summaryPath,
      JSON.stringify(
        {
          targetUrl: options.targetUrl,
          generatedAt: new Date().toISOString(),
          cookiesFile: cookiesFilePath,
          poTokenFile: poTokenPath,
          poTokenClientHint: authData.poTokenClientHint,
          poToken: authData.poToken,
          cookiesContent,
          detectedClientName: authData.clientName,
        },
        null,
        2,
      ),
      "utf8",
    );

    console.log("Auth bundle generated successfully:");
    console.log(`- Cookies: ${cookiesFilePath}`);
    console.log(`- PO token: ${poTokenPath}`);
    console.log(`- Summary: ${summaryPath}`);
    console.log(`- Client hint: ${authData.poTokenClientHint}`);
    console.log("");
    console.log("Next:");
    console.log("1. Move auth_bundle.json to your Android device");
    console.log("2. Open Settings > YouTube Auth in the Android app");
    console.log("3. Tap Import auth bundle and select auth_bundle.json");
    console.log("4. Retry the YouTube download");
  } finally {
    await context.close();
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
