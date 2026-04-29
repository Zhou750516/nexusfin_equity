#!/usr/bin/env node

const http = require("node:http");

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? 18081);
const reviewToApprovalCalls = Number(process.env.YUNKA_APPROVE_AFTER_QUERY_COUNT ?? 2);

const queryCounts = new Map();

function writeJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
  });
  response.end(JSON.stringify(payload));
}

function parseBody(request) {
  return new Promise((resolve, reject) => {
    let raw = "";
    request.on("data", chunk => {
      raw += chunk;
    });
    request.on("end", () => {
      if (!raw) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch (error) {
        reject(error);
      }
    });
    request.on("error", reject);
  });
}

function gatewaySuccess(data, message = "SUCCESS") {
  return {
    code: 0,
    message,
    data,
  };
}

function repayPlan() {
  return [
    {
      termNo: 1,
      repayDate: "2026-05-29",
      repayPrincipal: 100000,
      repayInterest: 4500,
      repayAmount: 104500,
    },
    {
      termNo: 2,
      repayDate: "2026-06-29",
      repayPrincipal: 100000,
      repayInterest: 4000,
      repayAmount: 104000,
    },
    {
      termNo: 3,
      repayDate: "2026-07-29",
      repayPrincipal: 100000,
      repayInterest: 3850,
      repayAmount: 103850,
    },
  ];
}

function loanQueryPayload(data) {
  const loanId = data.loanId ?? "LN-STUB-001";
  const count = (queryCounts.get(loanId) ?? 0) + 1;
  queryCounts.set(loanId, count);
  if (count < reviewToApprovalCalls) {
    return {
      loanId,
      status: "7002",
      loanAmount: 300000,
      remark: "借款申请已提交，正在审核中",
    };
  }
  return {
    loanId,
    status: "7001",
    loanAmount: 300000,
    remark: "审批通过，预计30分钟内到账",
  };
}

function handleGatewayRequest(payload) {
  const path = payload.path;
  const data = payload.data ?? {};
  switch (path) {
    case "/loan/trail":
      return gatewaySuccess({
        receiveAmount: Number(data.loanAmount ?? 300000),
        repayAmount: 312350,
        yearRate: 18.0,
        repayPlan: [
          {
            period: 1,
            date: "2026-05-29",
            principal: 100000,
            interest: 4500,
            total: 104500,
          },
          {
            period: 2,
            date: "2026-06-29",
            principal: 100000,
            interest: 4000,
            total: 104000,
          },
          {
            period: 3,
            date: "2026-07-29",
            principal: 100000,
            interest: 3850,
            total: 103850,
          },
        ],
      });
    case "/loan/apply":
      return gatewaySuccess({
        loanId: data.loanId ?? "LN-STUB-001",
        status: "4002",
        remark: "借款申请已提交，正在处理中",
      });
    case "/loan/query":
      return gatewaySuccess(loanQueryPayload(data));
    case "/loan/repayPlan":
      return gatewaySuccess({
        repayPlan: repayPlan(),
      });
    case "/repay/trial":
      return gatewaySuccess({
        repayAmount: 101850,
        amount: 101850,
      });
    case "/repay/apply":
      return gatewaySuccess({
        status: "5001",
        swiftNumber: data.loanId ? `RP-${data.loanId}` : "RP-STUB-001",
        remark: "还款请求已提交，正在处理中",
      });
    case "/repay/query":
      return gatewaySuccess({
        status: "8001",
        amount: 101850,
        repayAmount: 101850,
        swiftNumber: data.swiftNumber ?? data.loanId ?? "RP-STUB-001",
        discount: 2650,
        bankCardNum: "6222020202028648",
        successTime: "2026-04-29T10:00:00+08:00",
      });
    case "/card/userCards":
      return gatewaySuccess({
        list: [
          {
            cardId: "6222020202028648",
            bankName: "招商银行",
            cardLastFour: "8648",
            isDefault: 1,
          },
        ],
      });
    default:
      return {
        code: 10004,
        message: `unsupported path: ${path}`,
        data: {
          supportedPaths: [
            "/loan/trail",
            "/loan/apply",
            "/loan/query",
            "/loan/repayPlan",
            "/repay/trial",
            "/repay/apply",
            "/repay/query",
            "/card/userCards",
          ],
        },
      };
  }
}

const server = http.createServer(async (request, response) => {
  if (request.url !== "/api/gateway/proxy") {
    writeJson(response, 404, {
      code: 404,
      message: "not found",
      data: {
        method: request.method,
        path: request.url,
      },
    });
    return;
  }

  if (request.method === "GET") {
    writeJson(response, 200, gatewaySuccess({
      gatewayPath: "/api/gateway/proxy",
      supportedPaths: [
        "/loan/trail",
        "/loan/apply",
        "/loan/query",
        "/loan/repayPlan",
        "/repay/trial",
        "/repay/apply",
        "/repay/query",
        "/card/userCards",
      ],
    }, "Yunka stub is running"));
    return;
  }

  if (request.method !== "POST") {
    writeJson(response, 405, {
      code: 405,
      message: "method not allowed",
      data: null,
    });
    return;
  }

  try {
    const payload = await parseBody(request);
    writeJson(response, 200, handleGatewayRequest(payload));
  } catch (error) {
    writeJson(response, 400, {
      code: 400,
      message: `invalid request payload: ${error.message}`,
      data: null,
    });
  }
});

server.listen(port, host, () => {
  console.log(`[yunka-stub] listening on http://${host}:${port}`);
});

