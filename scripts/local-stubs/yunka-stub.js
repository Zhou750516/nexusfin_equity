#!/usr/bin/env node

const http = require("node:http");
const { supportedPaths } = require("./yunka-paths");

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? 18081);
const reviewToApprovalCalls = Number(process.env.YUNKA_APPROVE_AFTER_QUERY_COUNT ?? 2);
const timeoutDelayMs = Number(process.env.YUNKA_FAULT_TIMEOUT_DELAY_MS ?? 6500);

const REJECT_PATTERN = /.*_FAULT_REJECT_(\d+)$/;
const DELAY_PATTERN = /.*_FAULT_DELAY_(\d+)$/;
const INVALID_TOKEN_PATTERN = /.*_FAULT_(TOKEN_INVALID|SESSION_EXPIRED|TOKEN_TAMPERED)$/;
const TAMPERED_TOKEN_PATTERN = /.*_TAMPERED$/;
const LOAN_QUERY_REJECTED_PATTERN = /.*_FAULT_LOAN_REJECTED$/;
const NULL_DATA_PATTERN = /.*_FAULT_DATA_NULL$/;
const MISSING_FIELD_PATTERN = /.*_FAULT_MISSING_([A-Za-z0-9_]+)$/;

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
      repayPrincipal: 1000.0,
      repayInterest: 45.0,
      repayAmount: 1045.0,
    },
    {
      termNo: 2,
      repayDate: "2026-06-29",
      repayPrincipal: 1000.0,
      repayInterest: 40.0,
      repayAmount: 1040.0,
    },
    {
      termNo: 3,
      repayDate: "2026-07-29",
      repayPrincipal: 1000.0,
      repayInterest: 38.5,
      repayAmount: 1038.5,
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
      loanAmount: 3000.0,
      remark: "借款申请已提交，正在审核中",
    };
  }
  return {
    loanId,
    status: "7001",
    loanAmount: 3000.0,
    remark: "审批通过，预计30分钟内到账",
  };
}

function rejectedLoanQueryPayload(data) {
  return {
    loanId: data.loanId ?? "LN-STUB-001",
    status: "7003",
    loanAmount: 3000.0,
    remark: "借款申请未通过审核",
  };
}

function normalizeToken(value) {
  if (typeof value !== "string") {
    return "";
  }
  return value.trim();
}

function jointUserTokenPayload(data) {
  const token = normalizeToken(data.token);
  const cidSource = token || "joint-login-default";
  return {
    cid: `cid-${cidSource}`,
    name: "联合登录用户",
    phone: "13800138000",
  };
}

function jointUserQueryPayload(data) {
  return {
    userId: data.userId ?? "mem-joint-login-stub-001",
    cid: data.cid ?? "cid-joint-login-default",
    phone: "13800138000",
    basicInfo: {
      phone: "13800138000",
    },
    idInfo: {
      idno: "310101199001011111",
      name: "联合登录用户",
    },
  };
}

function protocolAggregationPayload() {
  return {
    list: [
      {
        title: "联合权益服务协议",
        isShow: 1,
        url: "https://mock-yunka.local/protocols/benefits-service",
      },
      {
        title: "联合扣款授权协议",
        isShow: 1,
        url: "https://mock-yunka.local/protocols/debit-auth",
      },
    ],
  };
}

function benefitSyncPayload(data) {
  return {
    status: "SUCCESS",
    msg: "权益订单同步成功",
    acceptedBenefitUrl: data.benefiturl ?? data.benefitUrl ?? "",
  };
}

function handleGatewayRequest(payload) {
  const path = payload.path;
  const data = payload.data ?? {};
  switch (path) {
    case "/loan/trial":
    case "/loan/trail":
      return gatewaySuccess({
        receiveAmount: Number(data.loanAmount ?? 3000),
        repayAmount: 3123.5,
        yearRate: 18.0,
        repayPlan: [
          {
            period: 1,
            date: "2026-05-29",
            principal: 1000.0,
            interest: 45.0,
            total: 1045.0,
          },
          {
            period: 2,
            date: "2026-06-29",
            principal: 1000.0,
            interest: 40.0,
            total: 1040.0,
          },
          {
            period: 3,
            date: "2026-07-29",
            principal: 1000.0,
            interest: 38.5,
            total: 1038.5,
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
        repayAmount: 1018.5,
        amount: 1018.5,
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
        amount: 1018.5,
        repayAmount: 1018.5,
        swiftNumber: data.swiftNumber ?? data.loanId ?? "RP-STUB-001",
        discount: 26.5,
        bankCardNum: "6222020202028648",
        successTime: "2026-04-29T10:00:00+08:00",
      });
    case "/protocol/queryProtocolAggregationLink":
      return gatewaySuccess(protocolAggregationPayload());
    case "/user/token":
      return gatewaySuccess(jointUserTokenPayload(data));
    case "/user/query":
      return gatewaySuccess(jointUserQueryPayload(data));
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
    case "/benefit/sync":
      return gatewaySuccess(benefitSyncPayload(data));
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

function collectFaultCandidates(payload) {
  const values = [];
  collectStrings(payload.traceId, values);
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
  if (payload.path === "/user/token") {
    for (const candidate of candidates) {
      const match = candidate.match(INVALID_TOKEN_PATTERN);
      if (match) {
        return {
          type: "semantic_reject",
          marker: candidate,
          code: 40101,
          message: match[1] === "SESSION_EXPIRED" ? "session expired" : "token invalid",
        };
      }
      if (candidate.match(TAMPERED_TOKEN_PATTERN)) {
        return {
          type: "semantic_reject",
          marker: candidate,
          code: 40101,
          message: "token invalid: tampered",
        };
      }
    }
  }
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
  if (payload.path === "/loan/query") {
    for (const candidate of candidates) {
      if (candidate.match(LOAN_QUERY_REJECTED_PATTERN)) {
        return {
          type: "loan_query_rejected",
          marker: candidate,
        };
      }
    }
  }
  for (const candidate of candidates) {
    if (candidate.match(NULL_DATA_PATTERN)) {
      return {
        type: "null_data",
        marker: candidate,
      };
    }
  }
  for (const candidate of candidates) {
    const match = candidate.match(MISSING_FIELD_PATTERN);
    if (match) {
      return {
        type: "missing_field",
        marker: candidate,
        field: match[1],
      };
    }
  }
  return null;
}

function applySemanticFault(body, payload, fault) {
  if (!fault) {
    return body;
  }
  if (fault.type === "loan_query_rejected") {
    return gatewaySuccess(rejectedLoanQueryPayload(payload.data ?? {}));
  }
  if (fault.type === "null_data") {
    return {
      ...body,
      data: null,
    };
  }
  if (fault.type === "missing_field") {
    if (body == null || typeof body !== "object" || body.data == null || typeof body.data !== "object" || Array.isArray(body.data)) {
      return body;
    }
    const mutatedData = { ...body.data };
    delete mutatedData[fault.field];
    return {
      ...body,
      data: mutatedData,
    };
  }
  return body;
}

function createGatewayPlan(payload) {
  const fault = resolveFaultDirective(payload);
  const body = applySemanticFault(handleGatewayRequest(payload), payload, fault);
  if (!fault) {
    return {
      statusCode: 200,
      body,
    };
  }
  if (fault.type === "semantic_reject") {
    return {
      statusCode: 200,
      body: {
        code: fault.code,
        message: fault.message,
        data: null,
      },
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
      const payload = {
        ...(await parseBody(request)),
        traceId: request.headers["x-trace-id"] ?? "",
      };
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
  applySemanticFault,
  resolveFaultDirective,
  supportedPaths,
};
