#!/usr/bin/env node

const http = require("node:http");

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? 18080);

function writeJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
  });
  response.end(JSON.stringify(payload));
}

function defaultProfiles() {
  return new Map([
    [
      process.env.MOCK_TECH_TOKEN ?? "mock-tech-token",
      {
        userId: "tech-user-local-001",
        phone: "13800138000",
        realName: "测试用户",
        idCard: "310101199001011111",
        channelCode: "KJ",
      },
    ],
    [
      process.env.MOCK_TECH_TOKEN_NO_SIGN ?? "mock-tech-token-no-sign",
      {
        userId: "tech-user-local-nosign-001",
        phone: "13800138001",
        realName: "无签约测试用户",
        idCard: "310101199001011112",
        channelCode: "KJ",
      },
    ],
    [
      process.env.MOCK_TECH_TOKEN_CLEAN ?? "mock-tech-token-clean",
      {
        userId: "tech-user-local-clean-001",
        phone: "13800138002",
        realName: "干净链路测试用户",
        idCard: "310101199001011113",
        channelCode: "KJ",
      },
    ],
    [
      process.env.MOCK_TECH_TOKEN_CLEAN_2 ?? "mock-tech-token-clean-2",
      {
        userId: "tech-user-local-clean-002",
        phone: "13800138003",
        realName: "第二干净链路测试用户",
        idCard: "310101199001011114",
        channelCode: "KJ",
      },
    ],
  ]);
}

function resolveProfile(authorization, profiles = defaultProfiles()) {
  if (!authorization?.startsWith("Bearer ")) {
    return null;
  }
  const token = authorization.slice("Bearer ".length);
  return profiles.get(token) ?? null;
}

function createServer(options = {}) {
  const profiles = options.profiles ?? defaultProfiles();
  return http.createServer((request, response) => {
    if (request.method === "GET" && request.url === "/api/users/me") {
      const profile = resolveProfile(request.headers.authorization, profiles);
      if (!profile) {
        writeJson(response, 401, {
          code: 401,
          message: "invalid tech token",
          data: null,
        });
        return;
      }
      writeJson(response, 200, {
        code: 0,
        message: "OK",
        data: profile,
      });
      return;
    }

    writeJson(response, 404, {
      code: 404,
      message: "not found",
      data: {
        method: request.method,
        path: request.url,
      },
    });
  });
}

if (require.main === module) {
  const server = createServer();
  server.listen(port, host, () => {
    console.log(`[tech-user-stub] listening on http://${host}:${port}`);
  });
}

module.exports = {
  createServer,
  defaultProfiles,
  resolveProfile,
};
