const test = require("node:test");
const assert = require("node:assert/strict");

const { createGatewayPlan, resolveFaultDirective, supportedPaths } = require("./yunka-stub");

test("should keep happy-path unchanged when no fault marker is present", () => {
  const plan = createGatewayPlan({
    requestId: "REQ_NORMAL_001",
    path: "/loan/apply",
    bizOrderNo: "APP-NORMAL-001",
    data: {
      loanId: "LN-NORMAL-001",
      platformBenefitOrderNo: "BO-NORMAL-001",
    },
  });

  assert.equal(plan.statusCode, 200);
  assert.equal(plan.delayMs, undefined);
  assert.equal(plan.body.code, 0);
  assert.equal(plan.body.data.loanId, "LN-NORMAL-001");
});

test("should resolve timeout and reject directives from stable business keys", () => {
  const timeout = resolveFaultDirective({
    requestId: "REQ_FAULT_TIMEOUT",
    path: "/repay/apply",
    bizOrderNo: "LN-001",
    data: { loanId: "LN-001" },
  });
  assert.equal(timeout.type, "timeout");

  const rejectPlan = createGatewayPlan({
    requestId: "REQ_NORMAL_002",
    path: "/repay/query",
    bizOrderNo: "RP-001_FAULT_REJECT_509",
    data: { swiftNumber: "RP-001_FAULT_REJECT_509" },
  });
  assert.equal(rejectPlan.body.code, 509);
  assert.match(rejectPlan.body.message, /Mock Yunka rejection code=509/);
});

test("should support delay injection from nested payload fields", () => {
  const plan = createGatewayPlan({
    requestId: "REQ_NORMAL_003",
    path: "/loan/apply",
    bizOrderNo: "APP-NORMAL-003",
    data: {
      platformBenefitOrderNo: "BO-003_FAULT_DELAY_25",
      loanId: "LN-003",
    },
  });

  assert.equal(plan.statusCode, 200);
  assert.equal(plan.delayMs, 25);
  assert.equal(plan.body.code, 0);
  assert.equal(plan.body.data.loanId, "LN-003");
});

test("should detect timeout marker from platformBenefitOrderNo for public loan apply flow", () => {
  const fault = resolveFaultDirective({
    requestId: "REQ_NORMAL_004",
    path: "/loan/apply",
    bizOrderNo: "APP-NORMAL-004",
    data: {
      platformBenefitOrderNo: "BO_B_P0_1_FAULT_TIMEOUT",
      loanId: "LN-004",
    },
  });

  assert.equal(fault.type, "timeout");
  assert.equal(fault.marker, "BO_B_P0_1_FAULT_TIMEOUT");
});

test("should support joint-login user token and user query paths", () => {
  const tokenPlan = createGatewayPlan({
    requestId: "REQ_USER_TOKEN_001",
    path: "/user/token",
    bizOrderNo: "JOINT-LOGIN-PUSH",
    data: {
      token: "joint-token-push-runtime-001",
    },
  });
  assert.equal(tokenPlan.statusCode, 200);
  assert.equal(tokenPlan.body.code, 0);
  assert.equal(tokenPlan.body.data.cid, "cid-joint-token-push-runtime-001");
  assert.equal(tokenPlan.body.data.name, "联合登录用户");
  assert.equal(tokenPlan.body.data.phone, "13800138000");

  const userPlan = createGatewayPlan({
    requestId: "REQ_USER_QUERY_001",
    path: "/user/query",
    bizOrderNo: "JOINT-LOGIN-PUSH",
    data: {
      userId: "mem-local-001",
      cid: "cid-joint-token-push-runtime-001",
    },
  });
  assert.equal(userPlan.statusCode, 200);
  assert.equal(userPlan.body.code, 0);
  assert.equal(userPlan.body.data.idInfo.idno, "310101199001011111");
  assert.equal(userPlan.body.data.idInfo.name, "联合登录用户");
  assert.equal(userPlan.body.data.phone, "13800138000");
  assert.equal(userPlan.body.data.basicInfo.phone, "13800138000");
  assert.ok(supportedPaths().includes("/user/token"));
  assert.ok(supportedPaths().includes("/user/query"));
});
