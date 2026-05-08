import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { normalizeAppBase } from "@/lib/route";
import NotFound from "@/pages/NotFound";
import { useEffect } from "react";
import { Route, Router as WouterRouter, Switch, useLocation } from "wouter";
import ErrorBoundary from "./components/ErrorBoundary";
import { LoanProvider } from "./contexts/LoanContext";
import { ThemeProvider } from "./contexts/ThemeContext";
import { I18nProvider } from "./i18n/I18nProvider";
import ApprovalPendingPage from "./pages/ApprovalPendingPage";
import ApprovalResultPage from "./pages/ApprovalResultPage";
import BenefitsCardPage from "./pages/BenefitsCardPage";
import CalculatorPage from "./pages/CalculatorPage";
import ConfirmRepaymentPage from "./pages/ConfirmRepaymentPage";
import JointDispatchPage from "./pages/JointDispatchPage";
import JointEntryPage from "./pages/JointEntryPage";
import JointRefundEntryPage from "./pages/JointRefundEntryPage";
import JointUnsupportedPage from "./pages/JointUnsupportedPage";
import LandingPage from "./pages/LandingPage";
import RepaymentSuccessPage from "./pages/RepaymentSuccessPage";

const ROUTER_BASE = normalizeAppBase();

function RootEntryRedirect() {
  const [, navigate] = useLocation();

  useEffect(() => {
    navigate("/landing");
  }, [navigate]);

  return null;
}

function AppRoutes() {
  return (
    <Switch>
      <Route path={"/"} component={RootEntryRedirect} />
      <Route path={"/index"} component={RootEntryRedirect} />
      <Route path={"/joint-entry"} component={JointEntryPage} />
      <Route path={"/joint-dispatch"} component={JointDispatchPage} />
      <Route path={"/joint-refund-entry"} component={JointRefundEntryPage} />
      <Route path={"/joint-unsupported"} component={JointUnsupportedPage} />
      <Route path={"/landing"} component={LandingPage} />
      <Route path={"/calculator"} component={CalculatorPage} />
      <Route path={"/approval-pending"} component={ApprovalPendingPage} />
      <Route path={"/benefits-card"} component={BenefitsCardPage} />
      <Route path={"/approval-result"} component={ApprovalResultPage} />
      <Route path={"/confirm-repayment"} component={ConfirmRepaymentPage} />
      <Route path={"/repayment-success"} component={RepaymentSuccessPage} />
      <Route path={"/404"} component={NotFound} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <WouterRouter base={ROUTER_BASE}>
        <ThemeProvider defaultTheme="light">
          <I18nProvider>
            <LoanProvider>
              <TooltipProvider>
                <Toaster />
                <AppRoutes />
              </TooltipProvider>
            </LoanProvider>
          </I18nProvider>
        </ThemeProvider>
      </WouterRouter>
    </ErrorBoundary>
  );
}

export default App;
