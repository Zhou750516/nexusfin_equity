import { CheckCircle2, Clock, Shield } from "lucide-react";
import { useLocation } from "wouter";

const STATS = [
  { value: "3分钟", label: "极速审批" },
  { value: "7.2%起", label: "年化利率" },
  { value: "当天", label: "快速到账" },
];

const FEATURES = ["3分钟审批", "无抵押担保", "当天到账"];

const ADVANTAGES = [
  {
    title: "快速审批",
    description: "智能风控系统,最快3分钟完成审批,当天放款",
    bgColor: "#dbeafe",
    iconColor: "#155dfc",
    Icon: Clock,
  },
  {
    title: "安全合规",
    description: "持牌金融机构,严格遵守监管要求,信息安全有保障",
    bgColor: "#dcfce7",
    iconColor: "#16a34a",
    Icon: Shield,
  },
];

const STEPS = [
  { title: "填写信息", description: "提供身份证与基本信息" },
  { title: "系统审核", description: "AI智能评估,获得额度" },
  { title: "资金到账", description: "签署协议后快速放款" },
];

const NOTICES = [
  "贷款年化利率范围:7.2%-24%(符合国家监管要求)",
  "示例:借款10,000元,期限30天,综合年化利率18%,应还本息约10,148元",
  "请根据自身还款能力理性借贷,避免逾期产生不良信用记录",
  "逾期还款将影响个人征信,请按时还款",
  "本平台由持牌金融机构提供服务,保障您的合法权益",
];

export default function LandingPage() {
  const [, navigate] = useLocation();

  return (
    <div className="min-h-screen bg-[#f0f2f5] flex justify-center">
      <div className="relative w-full max-w-[440px] bg-white shadow-xl">
        <section
          className="px-5 pt-12 pb-16"
          style={{ backgroundImage: "linear-gradient(126.28deg, #155dfc 0%, #1447e6 100%)" }}
        >
          <h1 className="text-white text-[30px] font-medium leading-9 text-center tracking-[0.4px]">
            惠聚平台
          </h1>
          <p className="mt-3 text-[#dbeafe] text-base leading-6 text-center tracking-[-0.31px]">
            便捷借款 急速到账
          </p>

          <div className="mt-8 bg-white/95 rounded-3xl shadow-[0_25px_50px_rgba(0,0,0,0.25)] p-8 flex flex-col gap-8">
            <div className="flex flex-col gap-2 items-center">
              <p
                className="text-[48px] font-normal leading-[48px] tracking-[0.35px] bg-clip-text text-transparent"
                style={{ backgroundImage: "linear-gradient(90deg, #155dfc 0%, #51a2ff 100%)" }}
              >
                ¥20,000
              </p>
              <p className="text-[#6a7282] text-sm leading-5 text-center tracking-[0.55px]">
                最高可借额度
              </p>
            </div>

            <div className="grid grid-cols-3 pb-5 border-b border-[#f3f4f6]">
              {STATS.map((stat, idx) => (
                <div
                  key={stat.label}
                  className={`flex flex-col items-center gap-1 ${
                    idx > 0 ? "border-l border-[#f3f4f6]" : ""
                  }`}
                >
                  <p className="text-[#101828] text-2xl leading-8 font-normal tracking-tight">
                    {stat.value}
                  </p>
                  <p className="text-[#6a7282] text-xs leading-4">{stat.label}</p>
                </div>
              ))}
            </div>

            <button
              type="button"
              onClick={() => navigate("/calculator")}
              className="w-full h-[60px] rounded-full bg-gradient-to-r from-[#155dfc] to-[#1447e6] text-white text-lg font-medium leading-7 tracking-tight shadow-[0_20px_25px_rgba(0,0,0,0.1),0_8px_10px_rgba(0,0,0,0.1)] active:opacity-90 transition-opacity"
            >
              去借款
            </button>
          </div>

          <div className="mt-6 flex justify-center gap-6">
            {FEATURES.map((label) => (
              <div key={label} className="flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-white" strokeWidth={2} />
                <span className="text-white text-sm leading-5 tracking-tight">{label}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="bg-[#f9fafb] px-5 pt-12 pb-12">
          <h2 className="text-[#101828] text-2xl font-medium leading-8 text-center tracking-tight">
            产品优势
          </h2>
          <div className="mt-8 flex flex-col gap-4">
            {ADVANTAGES.map(({ title, description, bgColor, iconColor, Icon }) => (
              <div
                key={title}
                className="bg-white rounded-2xl px-5 py-5 flex gap-4 items-start"
              >
                <div
                  className="rounded-full size-12 flex items-center justify-center shrink-0"
                  style={{ backgroundColor: bgColor }}
                >
                  <Icon className="size-6" style={{ color: iconColor }} strokeWidth={2} />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-[#101828] text-lg font-medium leading-7 tracking-tight">
                    {title}
                  </h3>
                  <p className="mt-1 text-[#4a5565] text-sm leading-5 tracking-tight">
                    {description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="px-5 py-12">
          <h2 className="text-[#101828] text-2xl font-medium leading-8 text-center tracking-tight">
            申请流程
          </h2>
          <ol className="mt-8 flex flex-col">
            {STEPS.map((step, idx) => (
              <li key={step.title} className="flex gap-4 items-stretch">
                <div className="flex flex-col items-center shrink-0">
                  <div className="bg-[#155dfc] rounded-full size-10 flex items-center justify-center text-white text-base leading-6 tracking-[-0.31px]">
                    {idx + 1}
                  </div>
                  {idx < STEPS.length - 1 ? (
                    <div className="w-px flex-1 bg-[#e5e7eb] my-2" />
                  ) : null}
                </div>
                <div className={`flex-1 min-w-0 ${idx < STEPS.length - 1 ? "pb-8" : ""}`}>
                  <h3 className="text-[#101828] text-lg font-medium leading-7 tracking-tight">
                    {step.title}
                  </h3>
                  <p className="mt-1 text-[#4a5565] text-sm leading-5 tracking-tight">
                    {step.description}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </section>

        <section className="bg-[#f9fafb] px-5 pt-8 pb-8">
          <h3 className="text-[#101828] text-sm font-medium leading-5 tracking-tight">
            重要提示
          </h3>
          <ul className="mt-3 flex flex-col gap-2">
            {NOTICES.map((notice) => (
              <li
                key={notice}
                className="text-[#4a5565] text-xs leading-[19.5px]"
              >
                • {notice}
              </li>
            ))}
          </ul>
        </section>

        <footer className="bg-[#101828] px-5 pt-8 pb-8 flex flex-col items-center gap-2 text-xs leading-4">
          <p className="text-white">惠聚平台</p>
          <p className="text-[#99a1af]">合作持牌金融机构提供贷款服务</p>
          <p className="text-[#99a1af]">理性借贷 · 诚信守约 · 保护个人信息安全</p>
          <p className="text-[#99a1af] mt-3">© 2026 惠聚平台 | 京ICP备XXXXXXXX号</p>
        </footer>
      </div>
    </div>
  );
}
