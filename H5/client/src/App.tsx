import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/NotFound";
import { Route, Switch } from "wouter";
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
import RepaymentSuccessPage from "./pages/RepaymentSuccessPage";

function Router() {
  return (
    <Switch>
      <Route path={"/joint-entry"} component={JointEntryPage} />
      <Route path={"/joint-dispatch"} component={JointDispatchPage} />
      <Route path={"/joint-refund-entry"} component={JointRefundEntryPage} />
      <Route path={"/"} component={CalculatorPage} />
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
      <ThemeProvider defaultTheme="light">
        <I18nProvider>
          <LoanProvider>
            <TooltipProvider>
              <Toaster />
              <Router />
            </TooltipProvider>
          </LoanProvider>
        </I18nProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
