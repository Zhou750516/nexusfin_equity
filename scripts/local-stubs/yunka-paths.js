const PRIMARY_SUPPORTED_PATHS = [
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
];

const LEGACY_ALIASED_PATHS = [
  "/loan/trail",
];

function supportedPaths() {
  return [...PRIMARY_SUPPORTED_PATHS, ...LEGACY_ALIASED_PATHS];
}

module.exports = {
  LEGACY_ALIASED_PATHS,
  PRIMARY_SUPPORTED_PATHS,
  supportedPaths,
};
