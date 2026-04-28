/**
 * Local mock backend for /api/* — used in development when the Java backend is not running.
 * Listens on :8080 to match the vite proxy target in vite.config.ts.
 */
import express, { type Request, type Response } from "express";

const app = express();
app.use(express.json());

const applicationPurposeStore = new Map<string, string>();
let applicationSequence = 1;

function ok<T>(data: T) {
  return { code: 0, data };
}

function shiftMonths(base: Date, months: number): Date {
  const d = new Date(base);
  d.setMonth(d.getMonth() + months);
  return d;
}

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

app.get("/api/loan/calculator-config", (_req: Request, res: Response) => {
  res.json(
    ok({
      amountRange: { min: 1000, max: 50000, step: 1000, default: 20000 },
      termOptions: [
        { label: "3期", value: 3 },
        { label: "6期", value: 6 },
        { label: "12期", value: 12 },
      ],
      annualRate: 0.072,
      lender: "工商银行",
      receivingAccount: { bankName: "工商银行", lastFour: "8888", accountId: "acc-001" },
    }),
  );
});

app.post("/api/loan/calculate", (req: Request, res: Response) => {
  const { amount = 20000, term = 3 } = (req.body ?? {}) as { amount: number; term: number };
  const monthlyRate = 0.072 / 12;
  const monthlyPrincipal = Math.round(amount / term);
  const monthlyInterest = Math.round(amount * monthlyRate);
  const monthlyTotal = monthlyPrincipal + monthlyInterest;
  const today = new Date();
  const repaymentPlan = Array.from({ length: term }, (_, i) => ({
    period: i + 1,
    date: isoDate(shiftMonths(today, i + 1)),
    principal: monthlyPrincipal,
    interest: monthlyInterest,
    total: monthlyTotal,
  }));
  res.json(
    ok({
      totalFee: monthlyInterest * term,
      annualRate: "7.2%",
      repaymentPlan,
    }),
  );
});

app.post("/api/loan/apply", (req: Request, res: Response) => {
  const purpose = typeof req.body?.purpose === "string" ? req.body.purpose : "shopping";
  const applicationId = `mock-app-${String(applicationSequence).padStart(3, "0")}`;
  applicationSequence += 1;
  applicationPurposeStore.set(applicationId, purpose);
  res.json(
    ok({
      applicationId,
      status: "pending",
      estimatedTime: "1-3分钟",
      benefitsActivated: false,
    }),
  );
});

app.get("/api/loan/approval-status/:applicationId", (req: Request, res: Response) => {
  const purpose = applicationPurposeStore.get(req.params.applicationId) ?? "shopping";
  res.json(
    ok({
      applicationId: req.params.applicationId,
      status: "reviewing",
      purpose,
      steps: [
        { name: "提交申请", status: "completed", description: "申请已提交" },
        { name: "智能审批", status: "in_progress", description: "正在进行资质审核" },
        { name: "放款准备", status: "pending", description: "审批通过后即可放款" },
      ],
      benefitsCard: {
        available: true,
        price: 99,
        features: ["视频会员", "出行优惠", "生活权益"],
      },
    }),
  );
});

app.get("/api/loan/approval-result/:applicationId", (req: Request, res: Response) => {
  const purpose = applicationPurposeStore.get(req.params.applicationId) ?? "shopping";
  res.json(
    ok({
      applicationId: req.params.applicationId,
      status: "approved",
      purpose,
      approvedAmount: 3000,
      estimatedArrivalTime: "预计30分钟内到账，具体以短信通知为准",
      steps: [
        { name: "提交申请", status: "completed", description: "申请已提交成功" },
        { name: "审批完成", status: "completed", description: "资质审核已通过" },
        { name: "准备放款", status: "completed", description: "资金将在30分钟内到账" },
      ],
      benefitsCardActivated: false,
      tip: "审批通过,请尽快确认放款",
      loanId: "mock-loan-001",
    }),
  );
});

app.get("/api/benefits/card-detail", (_req: Request, res: Response) => {
  res.json(
    ok({
      cardName: "惠选卡",
      price: 200,
      totalSaving: 448,
      features: [
        {
          title: "先享后付，无压力消费",
          description: "支持多种消费场景，先享受服务后付款，让您的消费更灵活",
        },
        {
          title: "稳赚不赔，100%回本保障",
          description: "双倍权益随心领，怎么用都划算",
        },
        {
          title: "匹配优质通道，不成功不收费",
          description: "智能匹配最适合您的优质通道，申请不成功不收取任何费用",
        },
      ],
      categories: [
        {
          name: "影音会员",
          icon: "tv",
          benefits: [
            {
              discount: "5折",
              title: "每月4选1影视VIP会员",
              description: "腾讯视频、优酷会员、爱奇艺、芒果TV任选其一",
              validity: "有效期30天/次",
              originalPrice: 30,
              saving: 15,
            },
            {
              discount: "4.5折",
              title: "每月2选1音乐VIP会员",
              description: "QQ音乐豪华绿钻、网易云音乐黑胶VIP任选",
              validity: "有效期30天/次",
              originalPrice: 20,
              saving: 11,
            },
          ],
        },
        { name: "出行服务", icon: "car", benefits: [] },
        { name: "生活服务", icon: "life", benefits: [] },
        { name: "日常购物", icon: "shop", benefits: [] },
      ],
      tips: [
        "权益服务费基于“惠聚”会员服务收取，包含影音娱乐、购物出行、生活服务、放款通道匹配等特权。",
        "本费用为互联网增值服务，独立于借款，非贷款利息、保障金或违约金。",
        "您充分知晓服务内容，先享后付模式下系统将从您指定银行卡扣费。",
      ],
      protocols: [],
    }),
  );
});

app.post("/api/benefits/activate", (_req: Request, res: Response) => {
  res.json(
    ok({
      activationId: "mock-act-001",
      status: "activated",
      message: "权益卡已激活",
    }),
  );
});

app.get("/api/repayment/info/:loanId", (req: Request, res: Response) => {
  res.json(
    ok({
      loanId: req.params.loanId,
      repaymentAmount: 1018.5,
      repaymentType: "early",
      bankCard: { bankName: "招商银行", lastFour: "8648", accountId: "acc-001" },
      tip: "还款后将立即生效，剩余期数对应的利息将不再收取。请确认银行卡余额充足。",
    }),
  );
});

app.post("/api/repayment/submit", (_req: Request, res: Response) => {
  res.json(
    ok({
      repaymentId: "mock-rep-001",
      status: "processing",
      message: "还款处理中",
    }),
  );
});

app.get("/api/repayment/result/:repaymentId", (req: Request, res: Response) => {
  res.json(
    ok({
      repaymentId: req.params.repaymentId,
      status: "success",
      amount: 1018.5,
      repaymentTime: "2026-03-19T14:32:00+08:00",
      bankCard: { bankName: "招商银行", lastFour: "8648" },
      interestSaved: 26.5,
      tips: [
        "还款金额已从您的银行卡扣除，请注意查收银行通知",
        "提前还款后，您的信用额度将即时恢复",
        "如需查看还款记录，可前往「我的」-「账单明细」",
        "若有任何疑问，请联系客服：400-888-8888",
      ],
    }),
  );
});

const port = 8080;
app.listen(port, () => {
  console.log(`Mock API server running on http://localhost:${port}`);
});
