#!/usr/bin/env node

const http = require("node:http");

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? 18081);
const reviewToApprovalCalls = Number(process.env.YUNKA_APPROVE_AFTER_QUERY_COUNT ?? 2);
const timeoutDelayMs = Number(process.env.YUNKA_FAULT_TIMEOUT_DELAY_MS ?? 6500);

const REJECT_PATTERN = /.*_FAULT_REJECT_(\d+)$/;
const DELAY_PATTERN = /.*_FAULT_DELAY_(\d+)$/;

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
    case "/card/smsSend":
      return gatewaySuccess({
        smsSeq: "sms-stub-001",
        status: "11001",
        msg: "验证码已发送",
      });
    case "/card/smsConfirm":
      return gatewaySuccess({
        status: "11002",
        msg: "验证码校验成功",
      });
    default:
      return {
        code: 10004,
        message: `unsupported path: ${path}`,
        data: {
          supportedPaths: supportedPaths(),
        },
      };
  }
}

function supportedPaths() {
  return [
    "/loan/trail",
    "/loan/apply",
    "/loan/query",
    "/loan/repayPlan",
    "/repay/trial",
    "/repay/apply",
    "/repay/query",
    "/card/userCards",
    "/card/smsSend",
    "/card/smsConfirm",
  ];
}

function collectFaultCandidates(payload) {
  const values = [];
  collectStrings(payload.requestId, values);
  collectStrings(payload.bizOrderNo, values);
  collectStrings(payload.path, values);
  collectStrings(payload.data, values);
  return values;
}

function collectStrings(value, values) {
  if (value == null) {
    return;
  }
  if (typeof value === "string") {
    values.push(value);
    return;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    values.push(String(value));
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item) => collectStrings(item, values));
    return;
  }
  if (typeof value === "object") {
    Object.values(value).forEach((item) => collectStrings(item, values));
  }
}

function resolveFaultDirective(payload) {
  const candidates = collectFaultCandidates(payload);
  for (const candidate of candidates) {
    if (candidate.includes("_FAULT_TIMEOUT")) {
      return { type: "timeout", marker: candidate, delayMs: timeoutDelayMs };
    }
  }
  for (const candidate of candidates) {
    const match = candidate.match(REJECT_PATTERN);
    if (match) {
      return {
        type: "reject",
        marker: candidate,
        code: Number(match[1]),
      };
    }
  }
  for (const candidate of candidates) {
    const match = candidate.match(DELAY_PATTERN);
    if (match) {
      return {
        type: "delay",
        marker: candidate,
        delayMs: Number(match[1]),
      };
    }
  }
  return null;
}

function createGatewayPlan(payload) {
  const fault = resolveFaultDirective(payload);
  const body = handleGatewayRequest(payload);
  if (!fault) {
    return {
      statusCode: 200,
      body,
    };
  }
  if (fault.type === "reject") {
    return {
      statusCode: 200,
      body: {
        code: fault.code,
        message: `Mock Yunka rejection code=${fault.code}`,
        data: null,
      },
    };
  }
  if (fault.type === "timeout") {
    return {
      statusCode: 200,
      body,
      delayMs: Math.max(timeoutDelayMs, fault.delayMs),
    };
  }
  if (fault.type === "delay") {
    return {
      statusCode: 200,
      body,
      delayMs: Math.max(0, fault.delayMs),
    };
  }
  return {
    statusCode: 200,
    body,
  };
}

function createServer() {
  return http.createServer(async (request, response) => {
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
        supportedPaths: supportedPaths(),
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
      const plan = createGatewayPlan(payload);
      if (plan.delayMs && plan.delayMs > 0) {
        setTimeout(() => {
          if (!response.writableEnded) {
            writeJson(response, plan.statusCode, plan.body);
          }
        }, plan.delayMs);
        return;
      }
      writeJson(response, plan.statusCode, plan.body);
    } catch (error) {
      writeJson(response, 400, {
        code: 400,
        message: `invalid request payload: ${error.message}`,
        data: null,
      });
    }
  });
}

if (require.main === module) {
  const server = createServer();
  server.listen(port, host, () => {
    console.log(`[yunka-stub] listening on http://${host}:${port}`);
  });
}

module.exports = {
  createGatewayPlan,
  createServer,
  gatewaySuccess,
  handleGatewayRequest,
  resolveFaultDirective,
  supportedPaths,
};
