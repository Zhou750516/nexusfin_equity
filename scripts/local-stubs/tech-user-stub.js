#!/usr/bin/env node

const http = require("node:http");

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? 18080);
const validToken = process.env.MOCK_TECH_TOKEN ?? "mock-tech-token";

function writeJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
  });
  response.end(JSON.stringify(payload));
}

const server = http.createServer((request, response) => {
  if (request.method === "GET" && request.url === "/api/users/me") {
    const authorization = request.headers.authorization ?? "";
    if (authorization !== `Bearer ${validToken}`) {
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
      data: {
        userId: "tech-user-local-001",
        phone: "13800138000",
        realName: "测试用户",
        idCard: "310101199001011111",
        channelCode: "KJ",
      },
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

server.listen(port, host, () => {
  console.log(`[tech-user-stub] listening on http://${host}:${port}`);
});

