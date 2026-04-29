const test = require("node:test");
const assert = require("node:assert/strict");

const { createGatewayPlan, resolveFaultDirective } = require("./yunka-stub");

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
