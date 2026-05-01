const test = require("node:test");
const assert = require("node:assert/strict");

const { createGatewayPlan, resolveFaultDirective, supportedPaths } = require("./yunka-stub");
const { PRIMARY_SUPPORTED_PATHS } = require("./yunka-paths");

const contractCases = [
  {
    path: "/loan/trial",
    payload: {
      requestId: "REQ_CONTRACT_LOAN_TRIAL_001",
      bizOrderNo: "LC-CONTRACT-001",
      data: {
        uid: "cid-contract-001",
        applyId: "LC-CONTRACT-001",
        loanAmount: 300000,
        loanPeriod: 3,
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.receiveAmount, 300000);
    },
  },
  {
    path: "/loan/apply",
    payload: {
      requestId: "REQ_CONTRACT_LOAN_APPLY_001",
      bizOrderNo: "LA-CONTRACT-001",
      data: {
        platformBenefitOrderNo: "BO-CONTRACT-001",
        loanId: "LN-CONTRACT-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.loanId, "LN-CONTRACT-001");
    },
  },
  {
    path: "/loan/query",
    payload: {
      requestId: "REQ_CONTRACT_LOAN_QUERY_001",
      bizOrderNo: "LQ-CONTRACT-001",
      data: {
        loanId: "LN-CONTRACT-QUERY-001",
      },
    },
    assertSuccess(plan) {
      assert.ok(["7001", "7002"].includes(plan.body.data.status));
    },
  },
  {
    path: "/protocol/queryProtocolAggregationLink",
    payload: {
      requestId: "REQ_CONTRACT_PROTOCOL_001",
      bizOrderNo: "benefits-card-detail",
      data: {
        userId: "cid-contract-001",
        loanAmount: 300000,
        loanPeriod: 3,
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.list.length, 2);
    },
  },
  {
    path: "/benefit/sync",
    payload: {
      requestId: "REQ_CONTRACT_BENEFIT_SYNC_001",
      bizOrderNo: "ord-contract-001",
      data: {
        userId: "cid-contract-001",
        platformBenefitOrderNo: "APP-CONTRACT-001",
        benefitStatus: "ACTIVE",
        benefitAmount: 300000,
        benefiturl: "https://mock-qw.local/exercise?partnerOrderNo=ord-contract-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.status, "SUCCESS");
      assert.match(plan.body.data.acceptedBenefitUrl, /^https:\/\/mock-qw\.local\/exercise/);
    },
  },
  {
    path: "/user/token",
    payload: {
      requestId: "REQ_CONTRACT_USER_TOKEN_001",
      bizOrderNo: "JOINT-CONTRACT-001",
      data: {
        token: "joint-token-contract-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.cid, "cid-joint-token-contract-001");
    },
  },
  {
    path: "/user/query",
    payload: {
      requestId: "REQ_CONTRACT_USER_QUERY_001",
      bizOrderNo: "JOINT-CONTRACT-001",
      data: {
        userId: "mem-contract-001",
        cid: "cid-joint-token-contract-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.idInfo.idno, "310101199001011111");
    },
  },
  {
    path: "/repay/trial",
    payload: {
      requestId: "REQ_CONTRACT_REPAY_TRIAL_001",
      bizOrderNo: "RP-TRIAL-CONTRACT-001",
      data: {
        loanId: "LN-CONTRACT-REPAY-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.repayAmount, 101850);
    },
  },
  {
    path: "/repay/apply",
    payload: {
      requestId: "REQ_CONTRACT_REPAY_APPLY_001",
      bizOrderNo: "RP-APPLY-CONTRACT-001",
      data: {
        loanId: "LN-CONTRACT-REPAY-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.status, "5001");
    },
  },
  {
    path: "/repay/query",
    payload: {
      requestId: "REQ_CONTRACT_REPAY_QUERY_001",
      bizOrderNo: "RP-QUERY-CONTRACT-001",
      data: {
        swiftNumber: "RP-CONTRACT-001",
      },
    },
    assertSuccess(plan) {
      assert.equal(plan.body.data.status, "8001");
    },
  },
];

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

test("should support semantic invalid-token fault on /user/token", () => {
  const invalidTokenPlan = createGatewayPlan({
    requestId: "REQ_USER_TOKEN_INVALID_001",
    path: "/user/token",
    bizOrderNo: "JOINT-LOGIN-INVALID",
    data: {
      token: "joint-token-invalid-runtime-001_FAULT_TOKEN_INVALID",
    },
  });
  assert.equal(invalidTokenPlan.statusCode, 200);
  assert.equal(invalidTokenPlan.body.code, 40101);
  assert.equal(invalidTokenPlan.body.message, "token invalid");

  const expiredPlan = createGatewayPlan({
    requestId: "REQ_USER_TOKEN_EXPIRED_001",
    path: "/user/token",
    bizOrderNo: "JOINT-LOGIN-EXPIRED",
    data: {
      token: "joint-token-expired-runtime-001_FAULT_SESSION_EXPIRED",
    },
  });
  assert.equal(expiredPlan.statusCode, 200);
  assert.equal(expiredPlan.body.code, 40101);
  assert.equal(expiredPlan.body.message, "session expired");
});

test("should support current local baseline paths for loan calculate and benefits sync", () => {
  const trialPlan = createGatewayPlan({
    requestId: "REQ_LOAN_TRIAL_001",
    path: "/loan/trial",
    bizOrderNo: "LC-001",
    data: {
      uid: "cid-joint-token-001",
      applyId: "LC-001",
      loanAmount: 300000,
      loanPeriod: 3,
    },
  });
  assert.equal(trialPlan.statusCode, 200);
  assert.equal(trialPlan.body.code, 0);
  assert.equal(trialPlan.body.data.receiveAmount, 300000);

  const protocolPlan = createGatewayPlan({
    requestId: "REQ_PROTOCOL_001",
    path: "/protocol/queryProtocolAggregationLink",
    bizOrderNo: "benefits-card-detail",
    data: {
      userId: "cid-joint-token-001",
      loanAmount: 300000,
      loanPeriod: 3,
    },
  });
  assert.equal(protocolPlan.statusCode, 200);
  assert.equal(protocolPlan.body.code, 0);
  assert.equal(protocolPlan.body.data.list.length, 2);
  assert.equal(protocolPlan.body.data.list[0].isShow, 1);

  const benefitSyncPlan = createGatewayPlan({
    requestId: "REQ_BENEFIT_SYNC_001",
    path: "/benefit/sync",
    bizOrderNo: "ord-benefit-sync-001",
    data: {
      userId: "cid-joint-token-001",
      platformBenefitOrderNo: "APP-001",
      benefitStatus: "ACTIVE",
      benefitAmount: 300000,
      benefiturl: "https://mock-qw.local/exercise?partnerOrderNo=ord-benefit-sync-001",
    },
  });
  assert.equal(benefitSyncPlan.statusCode, 200);
  assert.equal(benefitSyncPlan.body.code, 0);
  assert.equal(benefitSyncPlan.body.data.status, "SUCCESS");
  assert.ok(supportedPaths().includes("/loan/trial"));
  assert.ok(supportedPaths().includes("/protocol/queryProtocolAggregationLink"));
  assert.ok(supportedPaths().includes("/benefit/sync"));
});

test("should keep stub supported paths aligned with the current Yunka contract baseline", () => {
  assert.deepEqual(
    PRIMARY_SUPPORTED_PATHS,
    [
      "/loan/trial",
      "/loan/apply",
      "/loan/query",
      "/loan/repayPlan",
      "/repay/trial",
      "/repay/apply",
      "/repay/query",
      "/protocol/queryProtocolAggregationLink",
      "/user/token",
      "/user/query",
      "/card/userCards",
      "/card/smsSend",
      "/card/smsConfirm",
      "/credit/image/query",
      "/benefit/sync",
    ],
  );
});

test("should support every required Yunka contract path without unsupported-path drift", () => {
  for (const contractCase of contractCases) {
    const plan = createGatewayPlan({
      ...contractCase.payload,
      path: contractCase.path,
    });

    assert.equal(plan.statusCode, 200, `${contractCase.path} should return HTTP 200 in local stub`);
    assert.equal(plan.body.code, 0, `${contractCase.path} drifted or is unsupported`);
    contractCase.assertSuccess(plan);
  }
});
